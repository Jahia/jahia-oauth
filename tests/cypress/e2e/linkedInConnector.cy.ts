import {
    createSite,
    deleteSite,
    enableModule,
    publishAndWaitJobEnding
} from '@jahia/cypress';
import {faker} from '@faker-js/faker';
import {configureLinkedInConnector} from './utils/utils';
import {
    clearAllMocks,
    registerLinkedInOauthAuthorizeMock,
    registerLinkedInOauthTokenMock,
    registerLinkedInUserInfoMocks
} from './utils/mocking';
import {ClientCredentials, LinkedInUser} from './utils/types';
import {
    OAuthConnectorTestConfig,
    testAuthenticationWithWrongAuthCode,
    testAuthenticationWithWrongCredentials,
    testSuccessfulAuthentication
} from './utils/testScenarios';

describe('Tests for the LinkedIn connector', () => {
    let siteKey: string;
    let linkedInUser: LinkedInUser;
    let authCode: string;
    let linkedInClientCredentials: ClientCredentials;

    before(() => {
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
        linkedInClientCredentials = {
            clientId: faker.internet.password(),
            clientSecret: faker.internet.password()
        };
        cy.log(
            `Generated client credentials: ${JSON.stringify(
                linkedInClientCredentials
            )}`
        );

        configureLinkedInConnector(siteKey, linkedInClientCredentials);
    });

    beforeEach(() => {
    // Create a user
        linkedInUser = {
            id: faker.string.uuid(),
            localizedFirstName: faker.person.firstName(),
            localizedLastName: faker.person.lastName(),
            emailAddress: faker.internet.email(),
            pictureUrl: faker.internet.url()
        };

        authCode = faker.internet.password();

        clearAllMocks();

        cy.logout();
    });

    after(() => {
        deleteSite(siteKey);
    });

    it('Should display user details when successfully authenticated', () => {
        const config: OAuthConnectorTestConfig<LinkedInUser> = {
            connectorName: 'LinkedIn',
            pageUrl: `/sites/${siteKey}/linkedin.html`,
            buttonSelector: 'linkedin-button',
            credentials: linkedInClientCredentials,
            user: linkedInUser,
            authCode: authCode,
            siteKey: siteKey,
            registerAuthorizeMock: registerLinkedInOauthAuthorizeMock,
            registerTokenMock: registerLinkedInOauthTokenMock,
            registerUserInfoMock: registerLinkedInUserInfoMocks,
            expectedUserFields: {
                username: linkedInUser.id,
                firstName: linkedInUser.localizedFirstName,
                lastName: linkedInUser.localizedLastName,
                email: linkedInUser.emailAddress
            }
        };

        testSuccessfulAuthentication(config);
    });

    it('Should remain unauthenticated when the client credentials do not match', () => {
        const config: OAuthConnectorTestConfig<LinkedInUser> = {
            connectorName: 'LinkedIn',
            pageUrl: `/sites/${siteKey}/linkedin.html`,
            buttonSelector: 'linkedin-button',
            credentials: linkedInClientCredentials,
            user: linkedInUser,
            authCode: authCode,
            siteKey: siteKey,
            registerAuthorizeMock: registerLinkedInOauthAuthorizeMock,
            registerTokenMock: registerLinkedInOauthTokenMock,
            registerUserInfoMock: registerLinkedInUserInfoMocks,
            expectedUserFields: {
                username: linkedInUser.id,
                firstName: linkedInUser.localizedFirstName,
                lastName: linkedInUser.localizedLastName,
                email: linkedInUser.emailAddress
            }
        };

        testAuthenticationWithWrongCredentials(config, () => ({
            clientId: faker.internet.password(),
            clientSecret: faker.internet.password()
        }));
    });

    it('Should remain unauthenticated when the authorization code does not match', () => {
        const config: OAuthConnectorTestConfig<LinkedInUser> = {
            connectorName: 'LinkedIn',
            pageUrl: `/sites/${siteKey}/linkedin.html`,
            buttonSelector: 'linkedin-button',
            credentials: linkedInClientCredentials,
            user: linkedInUser,
            authCode: authCode,
            siteKey: siteKey,
            registerAuthorizeMock: registerLinkedInOauthAuthorizeMock,
            registerTokenMock: registerLinkedInOauthTokenMock,
            registerUserInfoMock: registerLinkedInUserInfoMocks,
            expectedUserFields: {
                username: linkedInUser.id,
                firstName: linkedInUser.localizedFirstName,
                lastName: linkedInUser.localizedLastName,
                email: linkedInUser.emailAddress
            }
        };

        testAuthenticationWithWrongAuthCode(config);
    });
});
