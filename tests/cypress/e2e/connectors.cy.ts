import {
    createSite,
    deleteSite,
    enableModule,
    publishAndWaitJobEnding
} from '@jahia/cypress';
import {faker} from '@faker-js/faker';
import {
    configureGoogleConnector,
    configureLinkedInConnector,
    configureGitHubConnector,
    configureFacebookConnector
} from './utils/utils';
import {
    clearAllMocks,
    registerGoogleOauthAuthorizeMock,
    registerGoogleOauthTokenMock,
    registerGoogleUserInfoMock,
    registerLinkedInOauthAuthorizeMock,
    registerLinkedInOauthTokenMock,
    registerLinkedInUserInfoMocks,
    registerGitHubOauthAuthorizeMock,
    registerGitHubOauthTokenMock,
    registerGitHubUserInfoMock,
    registerFacebookOauthAuthorizeMock,
    registerFacebookOauthTokenMock,
    registerFacebookUserInfoMock
} from './utils/mocking';
import {ClientCredentials, GoogleUser, LinkedInUser, GitHubUser, FacebookUser} from './utils/types';
import {
    testSuccessfulAuthentication,
    testAuthenticationWithWrongCredentials,
    testAuthenticationWithWrongAuthCode,
    OAuthConnectorTestConfig
} from './utils/testScenarios';

/**
 * Property mapping structure for custom fields
 */
interface PropertyMapping {
    connector: {valueType: string; name: string};
    editable: boolean;
    mapper: {valueType: string; name: string; mandatory: boolean};
}

/**
 * Connector configuration structure
 */
interface ConnectorConfig<TUser> {
    name: string;
    buttonSelector: string;
    configurator: (siteKey: string, credentials: ClientCredentials, customMapping?: PropertyMapping[]) => void;
    userGenerator: () => TUser;
    registerAuthorizeMock: (siteKey: string, credentials: ClientCredentials, authCode: string) => Cypress.Chainable;
    registerTokenMock: (authCode: string) => Cypress.Chainable;
    registerUserInfoMock: (user: TUser, customFields?: Record<string, string>) => Cypress.Chainable;
    expectedFieldsMapper: (user: TUser) => Record<string, string>;
}

/**
 * All OAuth connectors to test
 */
const connectors: ConnectorConfig<any>[] = [
    {
        name: 'Google',
        buttonSelector: 'google-button',
        configurator: configureGoogleConnector,
        userGenerator: (): GoogleUser => ({
            sub: faker.string.uuid(),
            name: faker.person.fullName(),
            givenName: faker.person.firstName(),
            familyName: faker.person.lastName(),
            email: faker.internet.email(),
            emailVerified: faker.datatype.boolean(),
            picture: faker.internet.url()
        }),
        registerAuthorizeMock: registerGoogleOauthAuthorizeMock,
        registerTokenMock: registerGoogleOauthTokenMock,
        registerUserInfoMock: registerGoogleUserInfoMock,
        expectedFieldsMapper: (user: GoogleUser) => ({
            username: user.sub,
            firstName: user.givenName,
            lastName: user.familyName,
            email: user.email
        })
    },
    {
        name: 'LinkedIn',
        buttonSelector: 'linkedin-button',
        configurator: configureLinkedInConnector,
        userGenerator: (): LinkedInUser => ({
            id: faker.string.uuid(),
            localizedFirstName: faker.person.firstName(),
            localizedLastName: faker.person.lastName(),
            emailAddress: faker.internet.email(),
            pictureUrl: faker.internet.url()
        }),
        registerAuthorizeMock: registerLinkedInOauthAuthorizeMock,
        registerTokenMock: registerLinkedInOauthTokenMock,
        registerUserInfoMock: registerLinkedInUserInfoMocks,
        expectedFieldsMapper: (user: LinkedInUser) => ({
            username: user.id,
            firstName: user.localizedFirstName,
            lastName: user.localizedLastName,
            email: user.emailAddress
        })
    },
    {
        name: 'GitHub',
        buttonSelector: 'github-button',
        configurator: configureGitHubConnector,
        userGenerator: (): GitHubUser => ({
            id: faker.number.int({min: 1000000, max: 9999999}),
            login: faker.internet.username(),
            name: faker.person.fullName(),
            email: faker.internet.email(),
            // eslint-disable-next-line camelcase
            avatar_url: faker.internet.url(),
            location: faker.location.city(),
            bio: faker.lorem.sentence()
        }),
        registerAuthorizeMock: registerGitHubOauthAuthorizeMock,
        registerTokenMock: registerGitHubOauthTokenMock,
        registerUserInfoMock: registerGitHubUserInfoMock,
        expectedFieldsMapper: (user: GitHubUser) => ({
            username: String(user.id),
            firstName: user.name,
            lastName: user.name
            // Note: Email omitted as GitHub does not reliably expose it
        })
    },
    {
        name: 'Facebook',
        buttonSelector: 'facebook-button',
        configurator: configureFacebookConnector,
        userGenerator: (): FacebookUser => ({
            id: faker.string.uuid(),
            name: faker.person.fullName(),
            // eslint-disable-next-line camelcase
            first_name: faker.person.firstName(),
            // eslint-disable-next-line camelcase
            last_name: faker.person.lastName(),
            email: faker.internet.email(),
            picture: {
                data: {
                    url: faker.internet.url()
                }
            }
        }),
        registerAuthorizeMock: registerFacebookOauthAuthorizeMock,
        registerTokenMock: registerFacebookOauthTokenMock,
        registerUserInfoMock: registerFacebookUserInfoMock,
        expectedFieldsMapper: (user: FacebookUser) => ({
            username: user.id,
            firstName: user.first_name,
            lastName: user.last_name,
            email: user.email
        })
    }
];

/**
 * Data-driven tests for all OAuth connectors
 */
connectors.forEach(connector => {
    describe(`Tests for the ${connector.name} connector`, () => {
        let siteKey: string;
        let user: any;
        let authCode: string;
        let clientCredentials: ClientCredentials;

        beforeEach(() => {
            siteKey = faker.lorem.slug();

            createSite(siteKey, {
                locale: 'en',
                serverName: 'localhost',
                templateSet: 'jahia-oauth-test-module'
            });
            publishAndWaitJobEnding(`/sites/${siteKey}`);
            enableModule('jahia-oauth', siteKey);
            enableModule('jcr-auth-provider', siteKey);

            // Generate random client credentials
            clientCredentials = {
                clientId: faker.internet.password(),
                clientSecret: faker.internet.password()
            };
            cy.log(`Generated client credentials for ${connector.name}: ${JSON.stringify(clientCredentials)}`);

            // Configure the connector
            connector.configurator(siteKey, clientCredentials);

            // Generate a new user for each test
            user = connector.userGenerator();

            // Generate a new auth code
            authCode = faker.internet.password();

            // Clear all WireMock mocks
            clearAllMocks();

            // Logout any existing session
            cy.logout();
        });

        afterEach(() => {
            deleteSite(siteKey);
        });

        it('Should display user details when successfully authenticated', () => {
            const config: OAuthConnectorTestConfig<any> = {
                connectorName: connector.name,
                pageUrl: `/sites/${siteKey}/${connector.name.toLowerCase()}.html`,
                buttonSelector: connector.buttonSelector,
                credentials: clientCredentials,
                user: user,
                authCode: authCode,
                siteKey: siteKey,
                registerAuthorizeMock: connector.registerAuthorizeMock,
                registerTokenMock: connector.registerTokenMock,
                registerUserInfoMock: connector.registerUserInfoMock,
                expectedUserFields: connector.expectedFieldsMapper(user)
            };

            testSuccessfulAuthentication(config);
        });

        it('Should display user details and the custom fields when successfully authenticated', () => {
            // Configure additional custom fields
            const customFields = {
                customField: faker.lorem.word(),
                otherField: faker.lorem.word()
            };

            // Create custom mapping: customField -> j:organization, otherField -> j:twitterID
            const customMapping = [
                {
                    connector: {valueType: 'string', name: 'customField'},
                    editable: false,
                    mapper: {valueType: 'string', name: 'j:organization', mandatory: false}
                },
                {
                    connector: {valueType: 'string', name: 'otherField'},
                    editable: false,
                    mapper: {valueType: 'string', name: 'j:twitterID', mandatory: false}
                }
            ];

            // Re-configure the connector with custom mapping
            connector.configurator(siteKey, clientCredentials, customMapping);

            const config: OAuthConnectorTestConfig<any> = {
                connectorName: connector.name,
                pageUrl: `/sites/${siteKey}/${connector.name.toLowerCase()}.html`,
                buttonSelector: connector.buttonSelector,
                credentials: clientCredentials,
                user: user,
                authCode: authCode,
                siteKey: siteKey,
                registerAuthorizeMock: connector.registerAuthorizeMock,
                registerTokenMock: connector.registerTokenMock,
                registerUserInfoMock: connector.registerUserInfoMock,
                expectedUserFields: {
                    ...connector.expectedFieldsMapper(user),
                    // Custom fields mapped to predefined properties
                    organization: customFields.customField,
                    twitterID: customFields.otherField
                },
                customFields: customFields
            };

            cy.logout();
            testSuccessfulAuthentication(config);
        });

        it('Should remain unauthenticated when the client credentials do not match', () => {
            const config: OAuthConnectorTestConfig<any> = {
                connectorName: connector.name,
                pageUrl: `/sites/${siteKey}/${connector.name.toLowerCase()}.html`,
                buttonSelector: connector.buttonSelector,
                credentials: clientCredentials,
                user: user,
                authCode: authCode,
                siteKey: siteKey,
                registerAuthorizeMock: connector.registerAuthorizeMock,
                registerTokenMock: connector.registerTokenMock,
                registerUserInfoMock: connector.registerUserInfoMock,
                expectedUserFields: connector.expectedFieldsMapper(user)
            };

            testAuthenticationWithWrongCredentials(config, () => ({
                clientId: faker.internet.password(),
                clientSecret: faker.internet.password()
            }));
        });

        it('Should remain unauthenticated when the authorization code does not match', () => {
            const config: OAuthConnectorTestConfig<any> = {
                connectorName: connector.name,
                pageUrl: `/sites/${siteKey}/${connector.name.toLowerCase()}.html`,
                buttonSelector: connector.buttonSelector,
                credentials: clientCredentials,
                user: user,
                authCode: authCode,
                siteKey: siteKey,
                registerAuthorizeMock: connector.registerAuthorizeMock,
                registerTokenMock: connector.registerTokenMock,
                registerUserInfoMock: connector.registerUserInfoMock,
                expectedUserFields: connector.expectedFieldsMapper(user)
            };

            testAuthenticationWithWrongAuthCode(config);
        });
    });
});
