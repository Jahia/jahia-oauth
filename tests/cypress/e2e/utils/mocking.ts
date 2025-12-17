import {ClientCredentials, GoogleUser, LinkedInUser, GitHubUser, FacebookUser} from './types';

const getWiremockBaseUrl = () => {
    return Cypress.env('WIREMOCK_URL') || 'http://wiremock:8888';
};

export function clearAllMocks() {
    const wiremockBaseUrl = getWiremockBaseUrl();
    return cy.request('DELETE', `${wiremockBaseUrl}/__admin/mappings`);
}

export function registerGoogleOauthAuthorizeMock(
    siteKey: string,
    googleClientCredentials: ClientCredentials,
    authCode: string
) {
    const baseUrl = Cypress.config('baseUrl');
    const callbackUrl = `${baseUrl}/sites/${siteKey}/home.googleOAuthCallbackAction.do`;
    const wiremockBaseUrl = getWiremockBaseUrl();

    return cy.request({
        method: 'POST',
        url: `${wiremockBaseUrl}/__admin/mappings`,
        body: {
            request: {
                method: 'GET',
                urlPath: '/google-mocked/oauth/authorize',
                queryParameters: {
                    // eslint-disable-next-line camelcase
                    access_type: {equalTo: 'offline'},
                    prompt: {equalTo: 'consent'},
                    // eslint-disable-next-line camelcase
                    include_granted_scopes: {equalTo: 'true'},
                    // eslint-disable-next-line camelcase
                    response_type: {equalTo: 'code'},
                    // eslint-disable-next-line camelcase
                    client_id: {equalTo: googleClientCredentials.clientId},
                    // eslint-disable-next-line camelcase
                    redirect_uri: {matches: String.raw`.*${siteKey}.*googleOAuthCallbackAction\.do`},
                    scope: {equalTo: 'profile email'}
                    // Note: state parameter is intentionally not matched to ignore its value
                }
            },
            response: {
                status: 302,
                headers: {
                    Location: `${callbackUrl}?code=${authCode}&state={{request.query.state}}`
                },
                transformers: ['response-template']
            }
        }
    }).then(response => response.body);
}

export function registerGoogleOauthTokenMock(authCode: string) {
    // Use direct WireMock API to support bodyPatterns for form parameter matching
    // WireMock Captain doesn't support advanced body matching for form parameters
    const wiremockBaseUrl =
    Cypress.env('WIREMOCK_URL') || 'http://wiremock:8888';

    return cy
        .request({
            method: 'POST',
            url: `${wiremockBaseUrl}/__admin/mappings`,
            body: {
                request: {
                    method: 'POST',
                    urlPath: '/google-mocked/oauth/token',
                    bodyPatterns: [
                        {contains: `code=${authCode}`},
                        {contains: 'grant_type=authorization_code'}
                    ]
                },
                response: {
                    status: 200,
                    jsonBody: {
                        // eslint-disable-next-line camelcase
                        access_token: '',
                        // eslint-disable-next-line camelcase
                        token_type: 'Bearer',
                        // eslint-disable-next-line camelcase
                        expires_in: 3600,
                        scope: 'profile email',
                        // eslint-disable-next-line camelcase
                        refresh_token: 'mock-refresh-token'
                    },
                    headers: {
                        'Content-Type': 'application/json'
                    }
                }
            }
        })
        .then(response => response.body);
}

export function registerGoogleUserInfoMock(user: GoogleUser) {
    const wiremockBaseUrl = getWiremockBaseUrl();

    return cy.request({
        method: 'POST',
        url: `${wiremockBaseUrl}/__admin/mappings`,
        body: {
            request: {
                method: 'GET',
                urlPath: '/google-mocked/userinfo'
            },
            response: {
                status: 200,
                jsonBody: {
                    sub: user.sub,
                    name: user.name,
                    // eslint-disable-next-line camelcase
                    given_name: user.givenName,
                    // eslint-disable-next-line camelcase
                    family_name: user.familyName,
                    email: user.email,
                    // eslint-disable-next-line camelcase
                    email_verified: user.emailVerified,
                    picture: user.picture
                }
            }
        }
    }).then(response => response.body);
}

export function registerLinkedInOauthAuthorizeMock(
    siteKey: string,
    linkedInClientCredentials: ClientCredentials,
    authCode: string
) {
    const baseUrl = Cypress.config('baseUrl');
    const callbackUrl = `${baseUrl}/sites/${siteKey}/home.linkedInOAuthCallbackAction.do`;
    const wiremockBaseUrl = getWiremockBaseUrl();

    return cy.request({
        method: 'POST',
        url: `${wiremockBaseUrl}/__admin/mappings`,
        body: {
            request: {
                method: 'GET',
                urlPath: '/linkedin-mocked/oauth/authorize',
                queryParameters: {
                    // eslint-disable-next-line camelcase
                    response_type: {equalTo: 'code'},
                    // eslint-disable-next-line camelcase
                    client_id: {equalTo: linkedInClientCredentials.clientId},
                    // eslint-disable-next-line camelcase
                    redirect_uri: {matches: String.raw`.*${siteKey}.*linkedInOAuthCallbackAction\.do`},
                    scope: {equalTo: 'r_liteprofile r_emailaddress'}
                    // Note: state parameter is intentionally not matched to ignore its value
                }
            },
            response: {
                status: 302,
                headers: {
                    Location: `${callbackUrl}?code=${authCode}&state={{request.query.state}}`
                },
                transformers: ['response-template']
            }
        }
    }).then(response => response.body);
}

export function registerLinkedInOauthTokenMock(authCode: string) {
    const wiremockBaseUrl = getWiremockBaseUrl();

    return cy.request({
        method: 'POST',
        url: `${wiremockBaseUrl}/__admin/mappings`,
        body: {
            request: {
                method: 'POST',
                urlPath: '/linkedin-mocked/oauth/token',
                bodyPatterns: [
                    {contains: `code=${authCode}`},
                    {contains: 'grant_type=authorization_code'}
                ]
            },
            response: {
                status: 200,
                jsonBody: {
                    // eslint-disable-next-line camelcase
                    access_token: 'mock-linkedin-access-token',
                    // eslint-disable-next-line camelcase
                    token_type: 'Bearer',
                    // eslint-disable-next-line camelcase
                    expires_in: 5184000
                },
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        }
    }).then(response => response.body);
}

export function registerLinkedInUserInfoMocks(user: LinkedInUser) {
    const wiremockBaseUrl = getWiremockBaseUrl();

    // LinkedIn uses two separate endpoints
    // Note: Using Direct WireMock API instead of WireMock Captain because:
    // - WireMock Captain's IWireMockRequest types don't support matcher objects in queryParameters
    // - TypeScript expects KeyValue (string|number|boolean) but we need {equalTo: 'value'}
    // - Direct API properly supports query parameter matching with matcher objects

    // Register email endpoint first, then profile endpoint
    return cy.request({
        method: 'POST',
        url: `${wiremockBaseUrl}/__admin/mappings`,
        body: {
            request: {
                method: 'GET',
                urlPath: '/linkedin-mocked/v2/emailAddress',
                queryParameters: {
                    q: {equalTo: 'members'},
                    projection: {equalTo: '(elements*(handle~))'}
                }
            },
            response: {
                status: 200,
                jsonBody: {
                    elements: [
                        {
                            'handle~': {
                                emailAddress: user.emailAddress
                            }
                        }
                    ]
                }
            }
        }
    }).then(() => {
        // Register profile endpoint
        return cy.request({
            method: 'POST',
            url: `${wiremockBaseUrl}/__admin/mappings`,
            body: {
                request: {
                    method: 'GET',
                    urlPath: '/linkedin-mocked/v2/me',
                    queryParameters: {
                        projection: {equalTo: '(id,localizedFirstName,localizedLastName,profilePicture(displayImage~:playableStreams),elements)'}
                    }
                },
                response: {
                    status: 200,
                    jsonBody: {
                        id: user.id,
                        localizedFirstName: user.localizedFirstName,
                        localizedLastName: user.localizedLastName,
                        ...(user.pictureUrl && {
                            profilePicture: {
                                'displayImage~': {
                                    elements: [
                                        {
                                            identifiers: [
                                                {
                                                    identifier: user.pictureUrl
                                                }
                                            ]
                                        }
                                    ]
                                }
                            }
                        })
                    }
                }
            }
        });
    });
}

export function registerGitHubOauthAuthorizeMock(
    siteKey: string,
    gitHubClientCredentials: ClientCredentials,
    authCode: string
) {
    const baseUrl = Cypress.config('baseUrl');
    const callbackUrl = `${baseUrl}/sites/${siteKey}/home.githubOAuthCallbackAction.do`;
    const wiremockBaseUrl = getWiremockBaseUrl();

    return cy.request({
        method: 'POST',
        url: `${wiremockBaseUrl}/__admin/mappings`,
        body: {
            request: {
                method: 'GET',
                urlPath: '/github-mocked/oauth/authorize',
                queryParameters: {
                    // eslint-disable-next-line camelcase
                    client_id: {equalTo: gitHubClientCredentials.clientId},
                    // eslint-disable-next-line camelcase
                    redirect_uri: {matches: String.raw`.*${siteKey}.*githubOAuthCallbackAction\.do`},
                    scope: {equalTo: 'user:email'}
                    // Note: state parameter is intentionally not matched to ignore its value
                }
            },
            response: {
                status: 302,
                headers: {
                    Location: `${callbackUrl}?code=${authCode}&state={{request.query.state}}`
                },
                transformers: ['response-template']
            }
        }
    }).then(response => response.body);
}

export function registerGitHubOauthTokenMock(authCode: string) {
    const wiremockBaseUrl = getWiremockBaseUrl();

    return cy.request({
        method: 'POST',
        url: `${wiremockBaseUrl}/__admin/mappings`,
        body: {
            request: {
                method: 'POST',
                urlPath: '/github-mocked/oauth/token',
                bodyPatterns: [
                    {contains: `code=${authCode}`},
                    {contains: 'grant_type=authorization_code'}
                ]
            },
            response: {
                status: 200,
                jsonBody: {
                    // eslint-disable-next-line camelcase
                    access_token: 'mock-github-access-token',
                    // eslint-disable-next-line camelcase
                    token_type: 'bearer',
                    scope: 'user:email'
                },
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        }
    }).then(response => response.body);
}

export function registerGitHubUserInfoMock(user: GitHubUser) {
    const wiremockBaseUrl = getWiremockBaseUrl();

    return cy.request({
        method: 'POST',
        url: `${wiremockBaseUrl}/__admin/mappings`,
        body: {
            request: {
                method: 'GET',
                urlPath: '/github-mocked/user'
            },
            response: {
                status: 200,
                jsonBody: {
                    id: user.id,
                    login: user.login,
                    name: user.name,
                    email: user.email,
                    // eslint-disable-next-line camelcase
                    avatar_url: user.avatar_url,
                    ...(user.location && {location: user.location}),
                    ...(user.bio && {bio: user.bio})
                }
            }
        }
    }).then(response => response.body);
}

export function registerFacebookOauthAuthorizeMock(
    siteKey: string,
    facebookClientCredentials: ClientCredentials,
    authCode: string
) {
    const baseUrl = Cypress.config('baseUrl');
    const callbackUrl = `${baseUrl}/sites/${siteKey}/home.facebookOAuthCallbackAction.do`;
    const wiremockBaseUrl = getWiremockBaseUrl();

    return cy.request({
        method: 'POST',
        url: `${wiremockBaseUrl}/__admin/mappings`,
        body: {
            request: {
                method: 'GET',
                urlPath: '/facebook-mocked/oauth/authorize',
                queryParameters: {
                    // eslint-disable-next-line camelcase
                    client_id: {equalTo: facebookClientCredentials.clientId},
                    // eslint-disable-next-line camelcase
                    redirect_uri: {matches: String.raw`.*${siteKey}.*facebookOAuthCallbackAction\.do`},
                    scope: {equalTo: 'email public_profile'}
                    // Note: state parameter is intentionally not matched to ignore its value
                }
            },
            response: {
                status: 302,
                headers: {
                    Location: `${callbackUrl}?code=${authCode}&state={{request.query.state}}`
                },
                transformers: ['response-template']
            }
        }
    }).then(response => response.body);
}

export function registerFacebookOauthTokenMock(authCode: string) {
    const wiremockBaseUrl = getWiremockBaseUrl();

    return cy.request({
        method: 'POST',
        url: `${wiremockBaseUrl}/__admin/mappings`,
        body: {
            request: {
                method: 'POST',
                urlPath: '/facebook-mocked/oauth/token',
                bodyPatterns: [
                    {contains: `code=${authCode}`},
                    {contains: 'grant_type=authorization_code'}
                ]
            },
            response: {
                status: 200,
                jsonBody: {
                    // eslint-disable-next-line camelcase
                    access_token: 'mock-facebook-access-token',
                    // eslint-disable-next-line camelcase
                    token_type: 'bearer',
                    // eslint-disable-next-line camelcase
                    expires_in: 5184000
                },
                headers: {
                    'Content-Type': 'application/json'
                }
            }
        }
    }).then(response => response.body);
}

export function registerFacebookUserInfoMock(user: FacebookUser) {
    const wiremockBaseUrl = getWiremockBaseUrl();

    return cy.request({
        method: 'POST',
        url: `${wiremockBaseUrl}/__admin/mappings`,
        body: {
            request: {
                method: 'GET',
                urlPath: '/facebook-mocked/v7.0/me'
            },
            response: {
                status: 200,
                jsonBody: {
                    id: user.id,
                    name: user.name,
                    // eslint-disable-next-line camelcase
                    first_name: user.first_name,
                    // eslint-disable-next-line camelcase
                    last_name: user.last_name,
                    email: user.email,
                    ...(user.picture && {picture: user.picture})
                }
            }
        }
    }).then(response => response.body);
}
