# Jahia OAuth Module - Cypress Tests

Comprehensive end-to-end tests for the Jahia OAuth module, testing authentication flows for multiple OAuth providers (Google, LinkedIn, GitHub, Facebook) without making real API calls.

## Overview

These tests validate the complete OAuth authentication flow in Jahia using:
- **Cypress** for end-to-end browser testing
- **WireMock** for mocking OAuth provider APIs
- **Docker Compose** for running Jahia and WireMock in containers
- **@faker-js/faker** for generating random test data

## Quick Start

### Build Docker Images
```bash
./ci.build.sh
```
This builds the Docker images required for testing, including Jahia with the OAuth module and WireMock.

### Run Tests
```bash
./ci.startup.sh
```
This starts the test environment using `docker-compose.yml` and runs the Cypress test suite.

## Test Architecture

### Test Structure

```
tests/
├── cypress/
│   └── e2e/
│       ├── facebookConnector.cy.ts    # Facebook OAuth tests
│       ├── googleConnector.cy.ts      # Google OAuth tests
│       ├── linkedInConnector.cy.ts    # LinkedIn OAuth tests
│       ├── githubConnector.cy.ts      # GitHub OAuth tests
│       └── utils/
│           ├── testScenarios.ts       # Shared test scenarios
│           ├── mocking.ts             # WireMock mock registration
│           ├── utils.ts               # Connector configuration
│           └── types.ts               # TypeScript types
├── jahia-module/                      # Test Jahia module
└── docker-compose.yml                 # Test environment setup
```

### Test Scenarios

Each OAuth connector is tested with three scenarios:

1. **Successful Authentication** ✅
   - User clicks OAuth button
   - OAuth flow completes successfully
   - User is authenticated and their details are displayed

2. **Wrong Client Credentials** ❌
   - OAuth button is clicked with incorrect credentials configured
   - Authorization fails (WireMock doesn't match the request)
   - User remains unauthenticated

3. **Wrong Authorization Code** ❌
   - Authorization succeeds but token exchange fails
   - WireMock token endpoint doesn't match
   - User remains unauthenticated

## WireMock Mocking

### Why WireMock?

WireMock allows us to mock OAuth provider APIs (Google, Facebook, LinkedIn, GitHub) without making real HTTP calls. This provides:
- **Fast tests** - No network latency
- **Reliable tests** - No external dependencies
- **Flexible scenarios** - Easy to test error cases
- **No rate limiting** - Test as much as needed

### How Mocking Works

#### 1. Mock API Registration

Before each test, WireMock mocks are registered for the OAuth flow:

```typescript
// Register authorize endpoint mock
config.registerAuthorizeMock(siteKey, credentials, authCode);

// Register token exchange endpoint mock
config.registerTokenMock(authCode);

// Register user info endpoint mock
config.registerUserInfoMock(user);
```

#### 2. Mock Endpoints Configuration

The Jahia OAuth module is configured to use WireMock URLs instead of real OAuth provider URLs:

```properties
# Example for Google
GoogleApi20.isEnabled = true
GoogleApi20.apiKey = <test-client-id>
GoogleApi20.apiSecret = <test-client-secret>
GoogleApi20.oauthApiName = mockedGoogleAPI  # ← Uses WireMock
```

The `MockOAuthTestService.java` registers custom OAuth API implementations that point to WireMock:

```java
jahiaOAuthService.addOAuthDefaultApi20(
    "mockedGoogleAPI", 
    connectorConfig -> new MockGoogleApi20("http://wiremock:8888/google-mocked")
);
```

#### 3. WireMock Mock Examples

**Authorize Endpoint Mock:**
```typescript
cy.request({
    method: 'POST',
    url: 'http://wiremock:8888/__admin/mappings',
    body: {
        request: {
            method: 'GET',
            urlPath: '/google-mocked/oauth/authorize',
            queryParameters: {
                client_id: {equalTo: 'test-client-id'},
                redirect_uri: {matches: '.*googleOAuthCallbackAction\\.do'},
                scope: {equalTo: 'profile email'}
            }
        },
        response: {
            status: 302,
            headers: {
                Location: 'http://localhost:8080/callback?code=auth-code&state={{request.query.state}}'
            }
        }
    }
});
```

**Token Exchange Mock:**
```typescript
cy.request({
    method: 'POST',
    url: 'http://wiremock:8888/__admin/mappings',
    body: {
        request: {
            method: 'POST',
            urlPath: '/google-mocked/oauth/token',
            bodyPatterns: [
                {contains: 'code=auth-code'},
                {contains: 'grant_type=authorization_code'}
            ]
        },
        response: {
            status: 200,
            jsonBody: {
                access_token: 'mock-access-token',
                token_type: 'Bearer'
            }
        }
    }
});
```

**User Info Mock:**
```typescript
cy.request({
    method: 'POST',
    url: 'http://wiremock:8888/__admin/mappings',
    body: {
        request: {
            method: 'GET',
            urlPath: '/google-mocked/userinfo'
        },
        response: {
            status: 200,
            jsonBody: {
                sub: 'user-id',
                email: 'user@example.com',
                given_name: 'John',
                family_name: 'Doe'
            }
        }
    }
});
```

## OAuth Flow Simulation

### The Challenge

OAuth typically works with popup windows:
1. Main window opens a popup
2. Popup navigates through OAuth flow
3. Popup sends `postMessage` to main window
4. Main window reloads to show authenticated user

### The Solution

Our tests simulate this flow without real popups:

```typescript
// 1. Stub window.open to track popup navigation
cy.window().then(win => {
    const mockPopup = { location: { href: '' }, close: () => {} };
    cy.stub(win, 'open').returns(mockPopup);
});

// 2. Click OAuth button (triggers XHR to get authorization URL)
cy.get('button').contains('google-button').click();

// 3. Make request to OAuth flow (simulates popup navigation)
cy.request({
    url: popupLocation,
    followRedirect: true  // Follows authorize → callback → result redirects
});

// 4. Simulate postMessage reload
cy.window().then(win => {
    win.location.search = 'site=my-site';  // Triggers page reload
});

// 5. Verify user is authenticated
cy.get('[data-test="user-logged-in"]').should('be.visible');
```

### Key Implementation Details

**window.open Stub:**
- Returns a mock popup object with `location` and `close()` methods
- Captures the authorization URL set by the OAuth button JavaScript
- Prevents actual popup windows in headless CI mode

**cy.request() for OAuth Flow:**
- Uses `cy.request()` instead of `cy.visit()` to keep the main window in place
- Sets the JSESSIONID cookie without navigating away
- Simulates what would happen in a real popup window

**PostMessage Simulation:**
- Manually triggers the page reload that would normally be triggered by the postMessage event
- Sets `window.location.search` to reload the page with the authenticated session

## Test Data

All test data is randomly generated using `@faker-js/faker`:

```typescript
const credentials = {
    clientId: faker.internet.password(),
    clientSecret: faker.internet.password()
};

const user = {
    id: faker.string.uuid(),
    email: faker.internet.email(),
    firstName: faker.person.firstName(),
    lastName: faker.person.lastName()
};

const authCode = faker.internet.password();
```

This ensures each test run is independent and doesn't rely on specific test data.

## Test Module

The `jahia-module` directory contains a minimal Jahia module for testing:

- **Page templates** with OAuth buttons (Google, Facebook, LinkedIn, GitHub)
- **User info display** showing authenticated user details
- **Mock OAuth API registration** via `MockOAuthTestService.java`

## Troubleshooting

### Tests Fail in CI Mode

**Problem:** Tests work in interactive mode (`cypress open`) but fail in CI mode (`cypress run`).

**Solution:** Ensure `window.open` is properly stubbed before clicking OAuth buttons. The tests handle this automatically.

### WireMock Not Matching Requests

**Problem:** WireMock returns 404 instead of mocked responses.

**Solution:** 
- Check that WireMock is running: `docker-compose ps`
- Verify mock registration with: `curl http://localhost:8888/__admin/mappings`
- Ensure query parameters and body patterns match exactly

### Session Cookie Not Set

**Problem:** User not authenticated after OAuth flow.

**Solution:**
- Verify `cy.getCookie('JSESSIONID')` returns a cookie
- Ensure `followRedirect: true` in `cy.request()` options
- Check that the callback action is processing correctly

### Window.opener PostMessage Errors

**Problem:** `Cannot read properties of null (reading 'postMessage')` errors.

**Solution:** The tests use `onBeforeLoad` to stub `window.opener` before the OAuth result page loads. This is handled automatically in `testScenarios.ts`.

## Development

### Running Tests Locally

```bash
# Interactive mode (opens Cypress UI)
yarn e2e:debug

# Headless mode (CI mode)
yarn e2e:ci
```

### Adding a New OAuth Provider

1. Add types in `utils/types.ts`:
   ```typescript
   export interface NewProviderUser { ... }
   ```

2. Add mocking functions in `utils/mocking.ts`:
   ```typescript
   export function registerNewProviderOauthAuthorizeMock(...) { ... }
   export function registerNewProviderOauthTokenMock(...) { ... }
   export function registerNewProviderUserInfoMock(...) { ... }
   ```

3. Add configuration in `utils/utils.ts`:
   ```typescript
   export function configureNewProviderConnector(...) {
       configureOAuthConnector({ ... });
   }
   ```

4. Create test file `newProviderConnector.cy.ts` using the shared test scenarios

5. Register mock API in `MockOAuthTestService.java`

## Architecture Diagrams

### Successful OAuth Flow

```
┌─────────────┐                  ┌──────────┐                 ┌─────────┐
│ Cypress     │                  │  Jahia   │                 │WireMock │
│ Test        │                  │  Server  │                 │         │
└──────┬──────┘                  └────┬─────┘                 └────┬────┘
       │                              │                            │
       │ 1. Visit page with button    │                            │
       ├─────────────────────────────>│                            │
       │                              │                            │
       │ 2. Click OAuth button        │                            │
       ├─────────────────────────────>│                            │
       │                              │                            │
       │                              │ 3. Get auth URL (XHR)      │
       │<─────────────────────────────┤                            │
       │                              │                            │
       │ 4. Request auth URL          │                            │
       ├──────────────────────────────┼───────────────────────────>│
       │                              │                            │
       │                              │                            │ 5. Redirect
       │<─────────────────────────────┼────────────────────────────┤ to callback
       │                              │                            │
       │ 6. Callback processes OAuth  │                            │
       ├─────────────────────────────>│                            │
       │                              │                            │
       │                              │ 7. Exchange token          │
       │                              ├───────────────────────────>│
       │                              │                            │
       │                              │ 8. Return access token     │
       │                              │<───────────────────────────┤
       │                              │                            │
       │                              │ 9. Get user info           │
       │                              ├───────────────────────────>│
       │                              │                            │
       │                              │ 10. Return user data       │
       │                              │<───────────────────────────┤
       │                              │                            │
       │ 11. Redirect to result page  │                            │
       │<─────────────────────────────┤                            │
       │ (JSESSIONID cookie set)      │                            │
       │                              │                            │
       │ 12. Reload main page         │                            │
       ├─────────────────────────────>│                            │
       │                              │                            │
       │ 13. Page with user details   │                            │
       │<─────────────────────────────┤                            │
       │ (authenticated)              │                            │
       │                              │                            │
```

## References

- [Cypress Documentation](https://docs.cypress.io/)
- [WireMock Documentation](http://wiremock.org/docs/)
- [Jahia OAuth Module](https://github.com/Jahia/jahia-oauth)
- [@faker-js/faker](https://fakerjs.dev/)

