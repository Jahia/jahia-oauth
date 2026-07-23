import {
    createSite,
    deleteSite,
    enableModule,
    publishAndWaitJobEnding
} from '@jahia/cypress';
import {faker} from '@faker-js/faker';

/**
 * Robustness tests for the OAuth result view (joant:oauthResult / oauthResult.jsp).
 *
 * The view emits an inline script that hands the authentication outcome back to the
 * opener window via postMessage. The `isAuthenticate` value in that message is a
 * boolean outcome flag, and the message target must be the current origin. These
 * tests pin that contract: the emitted value is always a well-formed boolean literal
 * derived from the request, never a verbatim echo of the raw request parameter, and
 * the message is scoped to window.location.origin.
 */
describe('OAuth result view - request parameter handling', () => {
    let siteKey: string;

    const resultPath = () => `/sites/${siteKey}/oauth-result.html`;

    // A parameter value that is not a well-formed boolean and contains script-context
    // delimiters. A correct view coerces this to the boolean literal `false`; an
    // unguarded view would splice it verbatim into the inline script.
    const malformedFlag = "</script><script>document.title='PROBE-HIT'</script><script>//";

    before(() => {
        siteKey = faker.lorem.slug();
        createSite(siteKey, {
            locale: 'en',
            serverName: 'localhost',
            templateSet: 'jahia-oauth-test-module'
        });
        publishAndWaitJobEnding(`/sites/${siteKey}`);
        enableModule('jahia-oauth', siteKey);
        publishAndWaitJobEnding(`/sites/${siteKey}`);
    });

    after(() => {
        deleteSite(siteKey);
    });

    beforeEach(() => {
        // The result page is a public, unauthenticated view.
        cy.logout();
    });

    it('emits a boolean literal (not the raw parameter) for a malformed isAuthenticate value', () => {
        cy.request({
            url: resultPath(),
            qs: {isAuthenticate: malformedFlag},
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            // The outcome flag is coerced to a well-formed boolean literal...
            expect(response.body).to.include('isAuthenticate: false');
            // ...and the raw parameter text is never spliced back into the script.
            expect(response.body).to.not.include("document.title='PROBE-HIT'");
            expect(response.body).to.not.include('</script><script>');
        });
    });

    it('preserves the true outcome on the normal (success) path', () => {
        cy.request({
            url: resultPath(),
            qs: {isAuthenticate: 'true'},
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.include('isAuthenticate: true');
        });
    });

    it('scopes the postMessage to the current origin rather than a wildcard target', () => {
        cy.request({
            url: resultPath(),
            qs: {isAuthenticate: 'true'},
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.include('window.opener.postMessage');
            expect(response.body).to.include('window.location.origin');
            expect(response.body).to.not.include("}, '*')");
        });
    });
});
