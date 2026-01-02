import {ClientCredentials} from './types';

interface PropertyMapping {
    connector: {
        valueType: string;
        name: string;
        propertyToRequest?: string;
    };
    editable: boolean;
    mapper: {
        valueType: string;
        name: string;
        mandatory: boolean;
    };
}

interface OAuthConnectorConfig {
    siteKey: string;
    credentials: ClientCredentials;
    connectorName: string;
    apiPrefix: string;
    callbackAction: string;
    scope: string;
    oauthApiName: string;
    mapping: PropertyMapping[];
    customMapping?: PropertyMapping[];
}

/**
 * Generic OAuth connector configuration
 */
function configureOAuthConnector(config: OAuthConnectorConfig) {
    const configFileName = `org.jahia.modules.auth-${config.siteKey}.cfg`;
    const baseUrl = Cypress.config().baseUrl;

    // Merge base mapping with custom mapping if provided
    const fullMapping = [
        ...config.mapping,
        ...(config.customMapping || [])
    ];

    const configContent = String.raw`# ${config.connectorName} OAuth Configuration
siteKey = ${config.siteKey}
${config.apiPrefix}.isEnabled = true
${config.apiPrefix}.apiKey = ${config.credentials.clientId}
${config.apiPrefix}.apiSecret = ${config.credentials.clientSecret}
${config.apiPrefix}.callbackUrl = ${baseUrl}/sites/${config.siteKey}/home.${config.callbackAction}
${config.apiPrefix}.scope = ${config.scope}
${config.apiPrefix}.oauthApiName = ${config.oauthApiName}
${config.apiPrefix}.mappers.jcrOAuthProvider.enabled = true
${config.apiPrefix}.mappers.jcrOAuthProvider.createUserAtSiteLevel = false
${config.apiPrefix}.mappers.jcrOAuthProvider.mapping = ${JSON.stringify(fullMapping)}
`;

    cy.log('file content: \n' + configContent + '\n\n');

    cy.runProvisioningScript({
        script: {
            fileContent: `- installConfiguration: "${configFileName}"`,
            fileName: `install-${config.connectorName.toLowerCase()}-config.yml`,
            type: 'application/yaml'
        },
        files: [
            {
                fileContent: configContent,
                fileName: configFileName,
                type: 'text/plain'
            }
        ]
    });
}

export function configureGoogleConnector(
    siteKey: string,
    googleCredentials: ClientCredentials,
    customMapping?: PropertyMapping[]
) {
    const mapping: PropertyMapping[] = [
        {
            connector: {valueType: 'string', name: 'id', propertyToRequest: 'sub'},
            editable: false,
            mapper: {valueType: 'string', name: 'ssoLoginId', mandatory: true}
        },
        {
            connector: {valueType: 'string', name: 'email'},
            editable: false,
            mapper: {valueType: 'email', name: 'j:email', mandatory: false}
        },
        {
            connector: {valueType: 'string', name: 'givenName', propertyToRequest: 'given_name'},
            editable: false,
            mapper: {valueType: 'string', name: 'j:firstName', mandatory: false}
        },
        {
            connector: {valueType: 'string', name: 'familyName', propertyToRequest: 'family_name'},
            editable: false,
            mapper: {valueType: 'string', name: 'j:lastName', mandatory: false}
        }
    ];

    configureOAuthConnector({
        siteKey,
        credentials: googleCredentials,
        connectorName: 'Google',
        apiPrefix: 'GoogleApi20',
        callbackAction: 'googleOAuthCallbackAction.do',
        scope: 'profile email',
        oauthApiName: 'mockedGoogleAPI',
        mapping,
        customMapping
    });
}

export function configureLinkedInConnector(
    siteKey: string,
    linkedInCredentials: ClientCredentials,
    customMapping?: PropertyMapping[]
) {
    const mapping: PropertyMapping[] = [
        {
            connector: {valueType: 'string', name: 'id'},
            editable: false,
            mapper: {valueType: 'string', name: 'ssoLoginId', mandatory: true}
        },
        {
            connector: {valueType: 'email', name: 'emailAddress'},
            editable: false,
            mapper: {valueType: 'email', name: 'j:email', mandatory: false}
        },
        {
            connector: {valueType: 'string', name: 'localizedFirstName'},
            editable: false,
            mapper: {valueType: 'string', name: 'j:firstName', mandatory: false}
        },
        {
            connector: {valueType: 'string', name: 'localizedLastName'},
            editable: false,
            mapper: {valueType: 'string', name: 'j:lastName', mandatory: false}
        }
    ];

    configureOAuthConnector({
        siteKey,
        credentials: linkedInCredentials,
        connectorName: 'LinkedIn',
        apiPrefix: 'LinkedInApi20',
        callbackAction: 'linkedInOAuthCallbackAction.do',
        scope: 'r_liteprofile r_emailaddress',
        oauthApiName: 'mockedLinkedInAPI',
        mapping,
        customMapping
    });
}

export function configureGitHubConnector(
    siteKey: string,
    gitHubCredentials: ClientCredentials,
    customMapping?: PropertyMapping[]
) {
    const mapping: PropertyMapping[] = [
        {
            connector: {valueType: 'string', name: 'id'},
            editable: false,
            mapper: {valueType: 'string', name: 'ssoLoginId', mandatory: true}
        },
        // GitHub only returns one field for the name, so mapping it to both firstName and lastName
        {
            connector: {valueType: 'string', name: 'name'},
            editable: false,
            mapper: {valueType: 'string', name: 'j:firstName', mandatory: false}
        },
        {
            connector: {valueType: 'string', name: 'name'},
            editable: false,
            mapper: {valueType: 'string', name: 'j:lastName', mandatory: false}
        }
    ];

    configureOAuthConnector({
        siteKey,
        credentials: gitHubCredentials,
        connectorName: 'GitHub',
        apiPrefix: 'GitHubApi',
        callbackAction: 'githubOAuthCallbackAction.do',
        scope: 'user:email',
        oauthApiName: 'mockedGitHubAPI',
        mapping,
        customMapping
    });
}

export function configureFacebookConnector(
    siteKey: string,
    facebookCredentials: ClientCredentials,
    customMapping?: PropertyMapping[]
) {
    const mapping: PropertyMapping[] = [
        {
            connector: {valueType: 'string', name: 'id'},
            editable: false,
            mapper: {valueType: 'string', name: 'ssoLoginId', mandatory: true}
        },
        {
            connector: {valueType: 'email', name: 'email'},
            editable: false,
            mapper: {valueType: 'email', name: 'j:email', mandatory: false}
        },
        {
            connector: {valueType: 'string', name: 'first_name'},
            editable: false,
            mapper: {valueType: 'string', name: 'j:firstName', mandatory: false}
        },
        {
            connector: {valueType: 'string', name: 'last_name'},
            editable: false,
            mapper: {valueType: 'string', name: 'j:lastName', mandatory: false}
        }
    ];

    configureOAuthConnector({
        siteKey,
        credentials: facebookCredentials,
        connectorName: 'Facebook',
        apiPrefix: 'FacebookApi',
        callbackAction: 'facebookOAuthCallbackAction.do',
        scope: 'email public_profile',
        oauthApiName: 'mockedFacebookAPI',
        mapping,
        customMapping
    });
}

export function assertIsUnauthenticated() {
    cy.get('[data-test="user-guest"]')
        .should('be.visible')
        .and('contain', 'Guest');
}
