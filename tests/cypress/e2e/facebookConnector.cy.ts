import {
    createSite,
    deleteSite,
    enableModule,
    publishAndWaitJobEnding
} from '@jahia/cypress';
import {faker} from '@faker-js/faker';
import {configureFacebookConnector} from './utils/utils';
import {
    clearAllMocks,
    registerFacebookOauthAuthorizeMock,
    registerFacebookOauthTokenMock,
    registerFacebookUserInfoMock
} from './utils/mocking';
import {ClientCredentials, FacebookUser} from './utils/types';
import {
    testSuccessfulAuthentication,
    testAuthenticationWithWrongCredentials,
    testAuthenticationWithWrongAuthCode,
    OAuthConnectorTestConfig
} from './utils/testScenarios';

describe('Tests for the Facebook connector', () => {
    let siteKey: string;
    let facebookUser: FacebookUser;
    let authCode: string;
    let facebookClientCredentials: ClientCredentials;

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
        facebookClientCredentials = {
            clientId: faker.internet.password(),
            clientSecret: faker.internet.password()
        };
        cy.log(
            `Generated client credentials: ${JSON.stringify(facebookClientCredentials)}`
        );

        configureFacebookConnector(siteKey, facebookClientCredentials);
    });

    beforeEach(() => {
        // Create a user
        facebookUser = {
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
        };

        authCode = faker.internet.password();

        clearAllMocks();

        cy.logout();
    });

    after(() => {
        deleteSite(siteKey);
    });

    it('Should display user details when successfully authenticated', () => {
        const config: OAuthConnectorTestConfig<FacebookUser> = {
            connectorName: 'Facebook',
            pageUrl: `/sites/${siteKey}/facebook.html`,
            buttonSelector: 'facebook-button',
            credentials: facebookClientCredentials,
            user: facebookUser,
            authCode: authCode,
            siteKey: siteKey,
            registerAuthorizeMock: registerFacebookOauthAuthorizeMock,
            registerTokenMock: registerFacebookOauthTokenMock,
            registerUserInfoMock: registerFacebookUserInfoMock,
            expectedUserFields: {
                username: facebookUser.id,
                firstName: facebookUser.first_name,
                lastName: facebookUser.last_name,
                email: facebookUser.email
            }
        };

        testSuccessfulAuthentication(config);
    });

    it('Should remain unauthenticated when the client credentials do not match', () => {
        const config: OAuthConnectorTestConfig<FacebookUser> = {
            connectorName: 'Facebook',
            pageUrl: `/sites/${siteKey}/facebook.html`,
            buttonSelector: 'facebook-button',
            credentials: facebookClientCredentials,
            user: facebookUser,
            authCode: authCode,
            siteKey: siteKey,
            registerAuthorizeMock: registerFacebookOauthAuthorizeMock,
            registerTokenMock: registerFacebookOauthTokenMock,
            registerUserInfoMock: registerFacebookUserInfoMock,
            expectedUserFields: {
                username: facebookUser.id,
                firstName: facebookUser.first_name,
                lastName: facebookUser.last_name,
                email: facebookUser.email
            }
        };

        testAuthenticationWithWrongCredentials(config, () => ({
            clientId: faker.internet.password(),
            clientSecret: faker.internet.password()
        }));
    });

    it('Should remain unauthenticated when the authorization code does not match', () => {
        const config: OAuthConnectorTestConfig<FacebookUser> = {
            connectorName: 'Facebook',
            pageUrl: `/sites/${siteKey}/facebook.html`,
            buttonSelector: 'facebook-button',
            credentials: facebookClientCredentials,
            user: facebookUser,
            authCode: authCode,
            siteKey: siteKey,
            registerAuthorizeMock: registerFacebookOauthAuthorizeMock,
            registerTokenMock: registerFacebookOauthTokenMock,
            registerUserInfoMock: registerFacebookUserInfoMock,
            expectedUserFields: {
                username: facebookUser.id,
                firstName: facebookUser.first_name,
                lastName: facebookUser.last_name,
                email: facebookUser.email
            }
        };

        testAuthenticationWithWrongAuthCode(config);
    });
});

