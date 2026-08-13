/**
 * Security regression tests for JAHIA-SEC-123 — OAuth session fixation via the `state` parameter (CWE-384 / CWE-352).
 *
 * The module used to send the raw HTTP session id as the OAuth `state` parameter and, on the callback, use whatever
 * `state` came back as the key the resolved identity is cached under - without ever checking it against the flow that
 * was actually initiated. An attacker could therefore have a victim complete an OAuth login whose identity got bound to
 * a `state` (session id) of the attacker's choosing, leading to SSO account takeover.
 *
 * The fix issues a cryptographically random, single-use `state` bound to the initiating session and rejects any
 * callback that does not present it back. These tests pin that behaviour down:
 *   1. the `state` sent to the provider is unpredictable and is not the session id;
 *   2. a callback whose `state` was not issued for the session is rejected and binds no identity;
 *   3. a legitimate flow, whose `state` round-trips unchanged, still authenticates.
 */

import {
    createSite,
    deleteSite,
    enableModule,
    publishAndWaitJobEnding
} from '@jahia/cypress';
import {faker} from '@faker-js/faker';
import {configureGoogleConnector, assertIsUnauthenticated} from './utils/utils';
import {
    clearAllMocks,
    registerGoogleOauthAuthorizeMock,
    registerGoogleOauthTokenMock,
    registerGoogleUserInfoMock
} from './utils/mocking';
import {testSuccessfulAuthentication, OAuthConnectorTestConfig} from './utils/testScenarios';
import {ClientCredentials, GoogleUser} from './utils/types';

const generateGoogleUser = (): GoogleUser => ({
    sub: faker.string.uuid(),
    name: faker.person.fullName(),
    givenName: faker.person.firstName(),
    familyName: faker.person.lastName(),
    email: faker.internet.email(),
    emailVerified: true,
    picture: faker.internet.url()
});

const asJson = (body: unknown): {authorizationUrl?: string} =>
    (typeof body === 'string' ? JSON.parse(body) : body) as {authorizationUrl?: string};

const asText = (body: unknown): string =>
    (typeof body === 'string' ? body : JSON.stringify(body));

describe('OAuth state / session-fixation hardening (JAHIA-SEC-123)', () => {
    let siteKey: string;
    let user: GoogleUser;
    let authCode: string;
    let clientCredentials: ClientCredentials;

    const connectUrl = () => `/sites/${siteKey}/home.connectToGoogleAction.do`;
    const callbackUrl = (code: string, state: string) =>
        `/sites/${siteKey}/home.googleOAuthCallbackAction.do?code=${encodeURIComponent(code)}&state=${encodeURIComponent(state)}`;
    const pageUrl = () => `/sites/${siteKey}/google.html`;

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

        clientCredentials = {
            clientId: faker.internet.password(),
            clientSecret: faker.internet.password()
        };
        configureGoogleConnector(siteKey, clientCredentials);

        user = generateGoogleUser();
        authCode = faker.internet.password();

        clearAllMocks();
        cy.logout();
        cy.clearCookies();
    });

    afterEach(() => {
        deleteSite(siteKey);
    });

    it('Should send a random state that is not the HTTP session id', () => {
        // Initiating the flow returns the authorization URL the browser would be redirected to.
        // The action only serves its JSON body for an "Accept: application/json" request (as the button XHR does);
        // without it Jahia renders an HTML page with an empty body.
        cy.request({url: connectUrl(), headers: {Accept: 'application/json'}, failOnStatusCode: false}).then(response => {
            expect(response.status).to.eq(200);

            const authorizationUrl = asJson(response.body).authorizationUrl;
            expect(authorizationUrl, 'an authorization URL is returned').to.be.a('string');
            expect(authorizationUrl).to.include('/oauth/authorize');

            const state = new URL(authorizationUrl as string).searchParams.get('state');
            expect(state, 'the state parameter is present').to.be.a('string').and.to.have.length.greaterThan(0);

            cy.getCookie('JSESSIONID').then(cookie => {
                const sessionId = cookie?.value;
                expect(sessionId, 'a session was established').to.be.a('string').and.to.have.length.greaterThan(0);

                // Core of the fix: the state must not be (or contain) the session id, and must carry real entropy
                expect(state, 'state must not be the session id').to.not.eq(sessionId);
                expect(state, 'state must not embed the session id').to.not.contain(sessionId as string);
                expect((state as string).length, 'state must be a high-entropy token').to.be.greaterThan(30);
            });
        });
    });

    it('Should reject a callback whose state was not issued for the session', () => {
        // Everything except the state is valid, so the state check is the only thing that can refuse the login
        registerGoogleOauthAuthorizeMock(siteKey, clientCredentials, authCode);
        registerGoogleOauthTokenMock(authCode);
        registerGoogleUserInfoMock(user);

        // Establish a session and a real, in-progress flow bound to it (Accept: application/json so the action runs
        // and stores its state, mirroring the button XHR)
        cy.request({url: connectUrl(), headers: {Accept: 'application/json'}, failOnStatusCode: false})
            .its('status').should('eq', 200);

        cy.getCookie('JSESSIONID').then(cookie => {
            const sessionId = cookie?.value as string;
            expect(sessionId, 'a session id cookie was set').to.be.a('string').and.to.have.length.greaterThan(0);

            // Replay the SEC-123 primitive: complete the callback with a valid code but state = the session id,
            // a value the attacker controls but that was never issued as this flow's state.
            cy.request({url: callbackUrl(authCode, sessionId), followRedirect: true, failOnStatusCode: false})
                .then(response => {
                    expect(asText(response.body), 'a forged state must not authenticate')
                        .to.not.include('Authentication successful');
                });
        });

        // No identity may have been bound to the session: the user is still a guest
        cy.visit(pageUrl());
        assertIsUnauthenticated();
    });

    it('Should still authenticate a legitimate flow whose state round-trips unchanged', () => {
        const config: OAuthConnectorTestConfig<GoogleUser> = {
            connectorName: 'Google',
            pageUrl: pageUrl(),
            buttonSelector: 'google-button',
            credentials: clientCredentials,
            user,
            authCode,
            siteKey,
            registerAuthorizeMock: registerGoogleOauthAuthorizeMock,
            registerTokenMock: registerGoogleOauthTokenMock,
            registerUserInfoMock: registerGoogleUserInfoMock,
            expectedUserFields: {
                username: user.sub,
                firstName: user.givenName,
                lastName: user.familyName,
                email: user.email
            }
        };

        testSuccessfulAuthentication(config);
    });
});
