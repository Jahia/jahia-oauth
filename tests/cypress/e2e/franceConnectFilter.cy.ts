/**
 * The FranceConnect filter answers on /callback and dispatches to the page path given in the "url" parameter, so that
 * a FranceConnect test server - which only accepts /callback as a redirect URI - can still reach the callback action.
 *
 * A dispatch is not re-evaluated by the security filter chain, so the filter must never dispatch to anything but a
 * page calling the FranceConnect callback action. These tests pin that down: the guard used to be a suffix test on the
 * raw parameter, which the request dispatcher then read differently (it strips path parameters and splits off the
 * query string before mapping the path), giving an unauthenticated caller the tools consoles and, through them,
 * arbitrary Groovy execution.
 */

const CALLBACK_ACTION = '.franceConnectOAuthCallbackAction.do';

// Executed by the JCR/Groovy consoles if the dispatch ever reaches them
const GROOVY_PROBE = 'out.print("id".execute().text); return true';

const requestCallback = (url: string, extraParams: Record<string, string> = {}) =>
    cy.request({
        url: '/callback',
        qs: {url, ...extraParams},
        failOnStatusCode: false,
        followRedirect: false
    });

const dispatchBypasses = [
    {
        name: 'a path parameter after the JSP name',
        url: `/modules/tools/jcrConsole.jsp;${CALLBACK_ACTION}`
    },
    {
        name: 'a query string separator after the JSP name',
        url: `/modules/tools/jcrConsole.jsp?ignored=${CALLBACK_ACTION}`
    },
    {
        name: 'a traversal combined with a path parameter',
        url: `/modules/tools/../tools/jcrConsole.jsp;${CALLBACK_ACTION}`
    },
    {
        name: 'a traversal out of an allowed prefix',
        url: `/sites/../modules/tools/jcrConsole.jsp;${CALLBACK_ACTION}`
    },
    {
        name: 'an encoded path parameter',
        url: `/modules/tools/jcrConsole.jsp%3b${CALLBACK_ACTION}`
    },
    {
        name: 'the Groovy console behind a path parameter',
        url: `/modules/tools/groovyConsole.jsp;${CALLBACK_ACTION}`
    },
    {
        name: 'the Karaf console behind a path parameter',
        url: `/modules/tools/karaf.jsp;${CALLBACK_ACTION}`
    },
    {
        name: 'the provisioning console behind a path parameter',
        url: `/modules/tools/provisioning.jsp;${CALLBACK_ACTION}`
    },
    {
        name: 'a path that is not a page path',
        url: `/atmosphere/anything${CALLBACK_ACTION}`
    },
    {
        name: 'the OSGi console',
        url: `/tools/osgi/system/console/bundles${CALLBACK_ACTION}`
    }
];

describe('FranceConnect /callback filter', () => {
    // Status returned when the filter declines to dispatch, so the assertions below do not hardcode Jahia's response
    let declinedStatus: number;

    before(() => {
        cy.logout();
        cy.clearCookies();
        requestCallback('/not-a-callback-path').then(response => {
            declinedStatus = response.status;
        });
    });

    beforeEach(() => {
        // Every request below must be evaluated as an unauthenticated visitor
        cy.logout();
        cy.clearCookies();
    });

    dispatchBypasses.forEach(dispatchBypass => {
        it(`Should not dispatch to a protected resource reached through ${dispatchBypass.name}`, () => {
            requestCallback(dispatchBypass.url, {action: 'execute', script: GROOVY_PROBE}).then(response => {
                const body = typeof response.body === 'string' ? response.body : JSON.stringify(response.body);

                expect(response.status, 'the target must be declined, like any unacceptable url parameter')
                    .to.eq(declinedStatus);
                expect(body, 'no console must be rendered')
                    .to.not.match(/<title>[^<]*(JCR Console|Groovy Console|Karaf|Provisioning)/i);
                expect(body, 'no command output must be returned').to.not.match(/uid=\d+\(/);
            });
        });
    });

    it('Should not dispatch to a target the request dispatcher would normalize', () => {
        // An empty segment is collapsed before the path is mapped, so what was checked is not what would be served
        requestCallback(`/cms//render/live/en/sites/systemsite/home${CALLBACK_ACTION}`).then(response => {
            expect(response.status).to.eq(declinedStatus);
        });
    });

    it('Should not serve the tools consoles to an unauthenticated visitor directly', () => {
        // Control: the consoles are gated, so the dispatch above is what the previous tests could have bypassed
        cy.request({
            url: '/modules/tools/jcrConsole.jsp',
            failOnStatusCode: false,
            followRedirect: false
        }).then(response => {
            expect(response.status).to.not.eq(200);
        });
    });

    it('Should still dispatch to the FranceConnect callback action', () => {
        // The feature the filter exists for: a page path calling the callback action is dispatched, and the response is
        // the one the callback action produces when it is reached directly
        const callbackPath = `/sites/systemsite/home${CALLBACK_ACTION}`;

        // The session id ends up in the redirect until the container knows cookies are supported
        const withoutSessionId = (location: string) => String(location).replace(/;jsessionid=[^?]*/, '');

        cy.request({url: callbackPath, failOnStatusCode: false, followRedirect: false}).then(direct => {
            requestCallback(callbackPath).then(response => {
                expect(response.status, 'the dispatch must not be rejected').to.eq(direct.status);
                expect(withoutSessionId(response.headers.location), 'the callback action must have handled the request')
                    .to.eq(withoutSessionId(direct.headers.location));
            });
        });
    });
});
