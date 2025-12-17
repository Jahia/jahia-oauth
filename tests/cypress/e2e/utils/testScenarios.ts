/**
 * Shared test scenarios for OAuth connectors
 * These functions encapsulate common test patterns to avoid duplication
 */

import {faker} from '@faker-js/faker';
import {assertIsUnauthenticated} from './utils';
import {ClientCredentials} from './types';

/**
 * Predefined mapping from test field names to JCR property names
 * These correspond to standard properties that are commonly used across OAuth providers
 */
const PREDEFINED_FIELD_MAPPING: Record<string, string> = {
    firstName: 'j:firstName',
    lastName: 'j:lastName',
    email: 'j:email',
    title: 'j:title',
    gender: 'j:gender',
    organization: 'j:organization',
    function: 'j:function',
    about: 'j:about',
    linkedInID: 'j:linkedInID',
    twitterID: 'j:twitterID',
    facebookID: 'j:facebookID',
    skypeID: 'j:skypeID'
};

/**
 * Expected user fields to check after successful authentication
 * Can include both predefined fields (firstName, lastName, email, etc.) and custom fields
 */
export interface ExpectedUserFields {
    // Standard predefined fields
    username?: string; // Special case - not a JCR property
    firstName?: string; // Maps to j:firstName
    lastName?: string; // Maps to j:lastName
    email?: string; // Maps to j:email
    title?: string; // Maps to j:title
    gender?: string; // Maps to j:gender
    organization?: string; // Maps to j:organization
    function?: string; // Maps to j:function
    about?: string; // Maps to j:about
    linkedInID?: string; // Maps to j:linkedInID
    twitterID?: string; // Maps to j:twitterID
    facebookID?: string; // Maps to j:facebookID
    skypeID?: string; // Maps to j:skypeID

    // Custom fields - use JCR property name as key (e.g., "j:customField": "value")
    [customField: string]: string | undefined;
}

/**
 * Configuration for OAuth connector tests
 */
export interface OAuthConnectorTestConfig<TUser> {
    connectorName: string;
    pageUrl: string;
    buttonSelector: string;
    credentials: ClientCredentials;
    user: TUser;
    authCode: string;
    siteKey: string;
    registerAuthorizeMock: (siteKey: string, credentials: ClientCredentials, authCode: string) => Cypress.Chainable;
    registerTokenMock: (authCode: string) => Cypress.Chainable;
    registerUserInfoMock: (user: TUser, customFields?: Record<string, string>) => Cypress.Chainable;
    expectedUserFields: ExpectedUserFields; // Fields to validate after authentication - supports both predefined and custom fields
    customFields?: Record<string, string>; // Custom fields to pass to registerUserInfoMock
}

/**
 * Common setup for OAuth flow testing
 * Sets up window.open stub, clicks the button, and returns the popup location
 */
function setupOAuthFlowAndClickButton<TUser>(config: OAuthConnectorTestConfig<TUser>): () => string {
    // Visit the page with OAuth button
    cy.visit(config.pageUrl);

    // Verify guest user initially
    assertIsUnauthenticated();

    // Stub window.open and track the popup location
    // The key is to simulate the popup navigation in a separate window context
    // while keeping the main window on the original page to receive the postMessage
    let popupLocation = '';
    cy.window().then(win => {
        const mockLocation = {href: ''};
        const mockPopup = {
            location: mockLocation,
            // eslint-disable-next-line @typescript-eslint/no-empty-function
            close: () => {}
        };
        // Watch for changes to the location href
        Object.defineProperty(mockLocation, 'href', {
            get: () => popupLocation,
            set: (url: string) => {
                popupLocation = url;
            }
        });
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        cy.stub(win as any, 'open').returns(mockPopup);
    });

    // Click the OAuth login button
    cy.get('button p').contains(config.buttonSelector).click();

    // Wait for the XHR callback to set the popup location
    // eslint-disable-next-line cypress/no-unnecessary-waiting
    cy.wait(1000);

    // Return a function to get the popup location (closure to capture the value)
    return () => popupLocation;
}

/**
 * Simulate the OAuth flow by making a request to the authorization URL
 * This sets the session cookie without navigating the main window
 */
function simulateOAuthFlowRequest(popupLocation: string) {
    return cy.request({
        url: popupLocation,
        followRedirect: true,
        failOnStatusCode: false
    });
}

/**
 * Simulate the main window receiving postMessage and reloading
 */
function simulatePostMessageReload(siteKey: string) {
    cy.window().then(win => {
        // Simulate receiving the postMessage by directly triggering the page reload
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (win as any).location.search = `site=${siteKey}`;
    });

    // Wait for the page to reload with the query parameter
    cy.url().should('include', `site=${siteKey}`);
}

/**
 * Verify user fields after successful authentication
 * Handles both predefined fields and custom fields with JCR property names
 */
function verifyAuthenticatedUserFields(expectedFields: ExpectedUserFields) {
    cy.get('[data-test="user-logged-in"]', {timeout: 10000}).should('be.visible');

    // Iterate over all expected fields
    Object.entries(expectedFields).forEach(([fieldKey, expectedValue]) => {
        if (expectedValue === undefined) {
            return; // Skip undefined values
        }

        // Special case: username is not a JCR property
        if (fieldKey === 'username') {
            cy.get('[data-test="username"]').should('contain', expectedValue);
            return;
        }

        // For both predefined and custom fields, use the full JCR property name as data-test attribute
        let dataTestAttribute: string;
        if (fieldKey in PREDEFINED_FIELD_MAPPING) {
            dataTestAttribute = PREDEFINED_FIELD_MAPPING[fieldKey]; // E.g., 'j:firstName'
        } else {
            dataTestAttribute = fieldKey; // Custom field, e.g., 'j:customField'
        }

        cy.get(`[data-test="${dataTestAttribute}"]`).should('contain', expectedValue);
    });
}

/**
 * Test scenario: Successful authentication
 * Difference from other scenarios: Uses correct credentials and auth code, expects authentication to succeed
 */
export function testSuccessfulAuthentication<TUser>(
    config: OAuthConnectorTestConfig<TUser>
) {
    cy.log(`Testing successful authentication for ${config.connectorName}`);

    // Configure all mocks with CORRECT credentials and auth code
    config.registerAuthorizeMock(config.siteKey, config.credentials, config.authCode);
    config.registerTokenMock(config.authCode);
    config.registerUserInfoMock(config.user, config.customFields);

    // Common setup: visit page, stub window.open, click button
    const getPopupLocation = setupOAuthFlowAndClickButton(config);

    // Verify the popup location was set to the authorization URL
    cy.wrap(null).then(() => {
        const popupLocation = getPopupLocation();
        expect(popupLocation).to.include('/oauth/authorize');
        expect(popupLocation).to.include(`client_id=${config.credentials.clientId}`);
        expect(popupLocation).to.include('redirect_uri');

        // Simulate the popup OAuth flow - this should SUCCEED
        return simulateOAuthFlowRequest(popupLocation).then(response => {
            // Assert on the response status and body content
            expect(response.status).to.be.eq(200);
            expect(response.body).to.include('Authentication successful');
        });
    });

    // Verify session cookie was set (authentication succeeded)
    cy.getCookie('JSESSIONID').should('exist');

    // Simulate the postMessage reload
    simulatePostMessageReload(config.siteKey);

    // EXPECTED: User should be logged in with correct details
    verifyAuthenticatedUserFields(config.expectedUserFields);
}

/**
 * Test scenario: Authentication fails with wrong client credentials
 * Difference from successful auth: Mocks are registered with WRONG credentials, so authorization fails
 */
export function testAuthenticationWithWrongCredentials<TUser>(
    config: OAuthConnectorTestConfig<TUser>,
    generateDifferentCredentials: () => ClientCredentials
) {
    cy.log(`Testing authentication failure with wrong credentials for ${config.connectorName}`);

    // Generate DIFFERENT credentials
    const differentCredentials = generateDifferentCredentials();
    cy.log(`Generated different client credentials: ${JSON.stringify(differentCredentials)}`);

    // KEY DIFFERENCE: Configure mocks with WRONG credentials
    // The authorize mock expects differentCredentials.clientId, but the app sends config.credentials.clientId
    // This causes the WireMock authorize endpoint to NOT match, so OAuth flow fails
    config.registerAuthorizeMock(config.siteKey, differentCredentials, config.authCode);
    config.registerTokenMock(config.authCode);
    config.registerUserInfoMock(config.user);

    // Common setup: visit page, stub window.open, click button
    const getPopupLocation = setupOAuthFlowAndClickButton(config);

    // Verify the popup location was set
    cy.wrap(null).then(() => {
        const popupLocation = getPopupLocation();
        expect(popupLocation).to.include('/oauth/authorize');
        expect(popupLocation).to.include(`client_id=${config.credentials.clientId}`);
        expect(popupLocation).to.include('redirect_uri');

        // Try to simulate the OAuth flow - this should FAIL (WireMock won't match)
        // return simulateOAuthFlowRequest(popupLocation);
        return simulateOAuthFlowRequest(popupLocation).then(response => {
            expect(response.body).to.not.include('Authentication successful');
        });
    });

    // Wait for any processing to complete
    // eslint-disable-next-line cypress/no-unnecessary-waiting
    cy.wait(2000);

    // Reload the page to check authentication status
    cy.visit(config.pageUrl);

    // EXPECTED: User should remain unauthenticated (OAuth flow failed)
    assertIsUnauthenticated();
}

/**
 * Test scenario: Authentication fails with wrong authorization code
 * Difference from successful auth: Token mock expects WRONG auth code, so token exchange fails
 */
export function testAuthenticationWithWrongAuthCode<TUser>(
    config: OAuthConnectorTestConfig<TUser>
) {
    cy.log(`Testing authentication failure with wrong auth code for ${config.connectorName}`);

    // Generate DIFFERENT auth code
    const differentAuthCode = faker.internet.password();
    cy.log(`Generated different auth code: ${differentAuthCode}`);

    // KEY DIFFERENCE: Token mock expects differentAuthCode, but callback will send config.authCode
    // The authorize mock returns config.authCode in redirect, but token mock expects differentAuthCode
    // This causes the WireMock token endpoint to NOT match, so token exchange fails
    config.registerAuthorizeMock(config.siteKey, config.credentials, config.authCode);
    config.registerTokenMock(differentAuthCode); // ← Wrong auth code here
    config.registerUserInfoMock(config.user);

    // Common setup: visit page, stub window.open, click button
    const getPopupLocation = setupOAuthFlowAndClickButton(config);

    // Verify the popup location was set
    cy.wrap(null).then(() => {
        const popupLocation = getPopupLocation();
        expect(popupLocation).to.include('/oauth/authorize');
        expect(popupLocation).to.include(`client_id=${config.credentials.clientId}`);
        expect(popupLocation).to.include('redirect_uri');

        // Try to simulate the OAuth flow - authorize succeeds but token exchange FAILS
        return simulateOAuthFlowRequest(popupLocation).then(response => {
            expect(response.body).to.not.include('Authentication successful');
        });
    });

    // Wait for any processing to complete
    // eslint-disable-next-line cypress/no-unnecessary-waiting
    cy.wait(2000);

    // Reload the page to check authentication status
    cy.visit(config.pageUrl);

    // EXPECTED: User should remain unauthenticated (token exchange failed)
    assertIsUnauthenticated();
}
