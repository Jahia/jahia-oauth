import {
    createSite,
    deleteSite,
    enableModule,
    publishAndWaitJobEnding
} from '@jahia/cypress';
import {faker} from '@faker-js/faker';
import {configureGoogleConnector} from './utils/utils';
import {
    clearAllMocks,
    registerGoogleOauthAuthorizeMock,
    registerGoogleOauthTokenMock,
    registerGoogleUserInfoMock
} from './utils/mocking';
import {ClientCredentials, GoogleUser} from './utils/types';
import {
    testSuccessfulAuthentication,
    testAuthenticationWithWrongCredentials,
    testAuthenticationWithWrongAuthCode,
    OAuthConnectorTestConfig
} from './utils/testScenarios';

describe('Tests for the Google connector', () => {
    let siteKey: string;
    let googleUser: GoogleUser;
    let authCode: string;
    let googleClientCredentials: ClientCredentials;

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
        googleClientCredentials = {
            clientId: faker.internet.password(),
            clientSecret: faker.internet.password()
        };
        cy.log(
            `Generated client credentials: ${JSON.stringify(googleClientCredentials)}`
        );

        configureGoogleConnector(siteKey, googleClientCredentials);
    });

    beforeEach(() => {
        // Create a user
        googleUser = {
            sub: faker.string.uuid(),
            name: faker.person.fullName(),
            givenName: faker.person.firstName(),
            familyName: faker.person.lastName(),
            email: faker.internet.email(),
            emailVerified: faker.datatype.boolean(),
            picture: faker.internet.url()
        };

        authCode = faker.internet.password();

        clearAllMocks();

        cy.logout();
    });

    after(() => {
        deleteSite(siteKey);
    });

    it('Should display user details when successfully authenticated', () => {
        const config: OAuthConnectorTestConfig<GoogleUser> = {
            connectorName: 'Google',
            pageUrl: `/sites/${siteKey}/google.html`,
            buttonSelector: 'google-button',
            credentials: googleClientCredentials,
            user: googleUser,
            authCode: authCode,
            siteKey: siteKey,
            registerAuthorizeMock: registerGoogleOauthAuthorizeMock,
            registerTokenMock: registerGoogleOauthTokenMock,
            registerUserInfoMock: registerGoogleUserInfoMock,
            expectedUserFields: {
                username: googleUser.sub,
                firstName: googleUser.givenName,
                lastName: googleUser.familyName,
                email: googleUser.email
            }
        };

        testSuccessfulAuthentication(config);
    });

    it('Should remain unauthenticated when the client credentials do not match', () => {
        const config: OAuthConnectorTestConfig<GoogleUser> = {
            connectorName: 'Google',
            pageUrl: `/sites/${siteKey}/google.html`,
            buttonSelector: 'google-button',
            credentials: googleClientCredentials,
            user: googleUser,
            authCode: authCode,
            siteKey: siteKey,
            registerAuthorizeMock: registerGoogleOauthAuthorizeMock,
            registerTokenMock: registerGoogleOauthTokenMock,
            registerUserInfoMock: registerGoogleUserInfoMock,
            expectedUserFields: {
                username: googleUser.sub,
                firstName: googleUser.givenName,
                lastName: googleUser.familyName,
                email: googleUser.email
            }
        };

        testAuthenticationWithWrongCredentials(config, () => ({
            clientId: faker.internet.password(),
            clientSecret: faker.internet.password()
        }));
    });

    it('Should remain unauthenticated when the authorization code does not match', () => {
        const config: OAuthConnectorTestConfig<GoogleUser> = {
            connectorName: 'Google',
            pageUrl: `/sites/${siteKey}/google.html`,
            buttonSelector: 'google-button',
            credentials: googleClientCredentials,
            user: googleUser,
            authCode: authCode,
            siteKey: siteKey,
            registerAuthorizeMock: registerGoogleOauthAuthorizeMock,
            registerTokenMock: registerGoogleOauthTokenMock,
            registerUserInfoMock: registerGoogleUserInfoMock,
            expectedUserFields: {
                username: googleUser.sub,
                firstName: googleUser.givenName,
                lastName: googleUser.familyName,
                email: googleUser.email
            }
        };

        testAuthenticationWithWrongAuthCode(config);
    });
});
