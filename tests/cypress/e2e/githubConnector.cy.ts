import {
    createSite,
    deleteSite,
    enableModule,
    publishAndWaitJobEnding
} from '@jahia/cypress';
import {faker} from '@faker-js/faker';
import {configureGitHubConnector} from './utils/utils';
import {
    clearAllMocks,
    registerGitHubOauthAuthorizeMock,
    registerGitHubOauthTokenMock,
    registerGitHubUserInfoMock
} from './utils/mocking';
import {ClientCredentials, GitHubUser} from './utils/types';
import {
    testSuccessfulAuthentication,
    testAuthenticationWithWrongCredentials,
    testAuthenticationWithWrongAuthCode,
    OAuthConnectorTestConfig
} from './utils/testScenarios';

describe('Tests for the GitHub connector', () => {
    let siteKey: string;
    let gitHubUser: GitHubUser;
    let authCode: string;
    let gitHubClientCredentials: ClientCredentials;

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
        gitHubClientCredentials = {
            clientId: faker.internet.password(),
            clientSecret: faker.internet.password()
        };
        cy.log(
            `Generated client credentials: ${JSON.stringify(gitHubClientCredentials)}`
        );

        configureGitHubConnector(siteKey, gitHubClientCredentials);
    });

    beforeEach(() => {
        // Create a user
        gitHubUser = {
            id: faker.number.int({min: 1000000, max: 9999999}),
            login: faker.internet.username(),
            name: faker.person.fullName(),
            email: faker.internet.email(),
            // eslint-disable-next-line camelcase
            avatar_url: faker.internet.url(),
            location: faker.location.city(),
            bio: faker.lorem.sentence()
        };

        authCode = faker.internet.password();

        clearAllMocks();

        cy.logout();
    });

    after(() => {
        deleteSite(siteKey);
    });

    it('Should display user details when successfully authenticated', () => {
        const config: OAuthConnectorTestConfig<GitHubUser> = {
            connectorName: 'GitHub',
            pageUrl: `/sites/${siteKey}/github.html`,
            buttonSelector: 'github-button',
            credentials: gitHubClientCredentials,
            user: gitHubUser,
            authCode: authCode,
            siteKey: siteKey,
            registerAuthorizeMock: registerGitHubOauthAuthorizeMock,
            registerTokenMock: registerGitHubOauthTokenMock,
            registerUserInfoMock: registerGitHubUserInfoMock,
            expectedUserFields: {
                username: String(gitHubUser.id),
                firstName: gitHubUser.name,
                lastName: gitHubUser.name
                // Email omitted - GitHub does not expose it
            }
        };

        testSuccessfulAuthentication(config);
    });

    it('Should remain unauthenticated when the client credentials do not match', () => {
        const config: OAuthConnectorTestConfig<GitHubUser> = {
            connectorName: 'GitHub',
            pageUrl: `/sites/${siteKey}/github.html`,
            buttonSelector: 'github-button',
            credentials: gitHubClientCredentials,
            user: gitHubUser,
            authCode: authCode,
            siteKey: siteKey,
            registerAuthorizeMock: registerGitHubOauthAuthorizeMock,
            registerTokenMock: registerGitHubOauthTokenMock,
            registerUserInfoMock: registerGitHubUserInfoMock,
            expectedUserFields: {
                username: gitHubUser.login,
                firstName: gitHubUser.name,
                email: gitHubUser.email
            }
        };

        testAuthenticationWithWrongCredentials(config, () => ({
            clientId: faker.internet.password(),
            clientSecret: faker.internet.password()
        }));
    });

    it('Should remain unauthenticated when the authorization code does not match', () => {
        const config: OAuthConnectorTestConfig<GitHubUser> = {
            connectorName: 'GitHub',
            pageUrl: `/sites/${siteKey}/github.html`,
            buttonSelector: 'github-button',
            credentials: gitHubClientCredentials,
            user: gitHubUser,
            authCode: authCode,
            siteKey: siteKey,
            registerAuthorizeMock: registerGitHubOauthAuthorizeMock,
            registerTokenMock: registerGitHubOauthTokenMock,
            registerUserInfoMock: registerGitHubUserInfoMock,
            expectedUserFields: {
                username: gitHubUser.login,
                firstName: gitHubUser.name,
                email: gitHubUser.email
            }
        };

        testAuthenticationWithWrongAuthCode(config);
    });
});

