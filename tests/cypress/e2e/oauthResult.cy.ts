import {
    createSite,
    deleteSite,
    enableModule,
    publishAndWaitJobEnding
} from '@jahia/cypress';
import {faker} from '@faker-js/faker';

/**
 * The OAuth result view renders an inline script that hands the authentication
 * outcome to the opener window via postMessage. Two correctness properties must
 * hold regardless of the value supplied on the request URL:
 *
 *   1. The `isAuthenticate` request parameter is rendered as a well-formed
 *      JavaScript boolean literal (`true` / `false`) — it is a boolean flag, so
 *      an arbitrary request-parameter value must never reach the inline script
 *      verbatim (which would emit malformed / arbitrary JavaScript).
 *   2. The message targets the page's own origin, not the `'*'` wildcard.
 *
 * The view is rendered live and is reachable by an unauthenticated visitor, so
 * the assertions run as a guest request against the rendered HTML.
 */
describe('OAuth result view — authentication flag rendering', () => {
    let siteKey: string;

    before(() => {
        siteKey = faker.lorem.slug();
        createSite(siteKey, {
            locale: 'en',
            serverName: 'localhost',
            templateSet: 'jahia-oauth-test-module'
        });
        publishAndWaitJobEnding(`/sites/${siteKey}`);
        enableModule('jahia-oauth', siteKey);
        cy.logout();
    });

    after(() => {
        deleteSite(siteKey);
    });

    const renderResultPage = (isAuthenticate: string) =>
        cy.request({
            url: `/sites/${siteKey}/oauth-result.html`,
            qs: {isAuthenticate},
            failOnStatusCode: false
        });

    it('renders the flag as a boolean literal for a well-formed value', () => {
        renderResultPage('true').then(res => {
            expect(res.status).to.eq(200);
            expect(res.body).to.contain('isAuthenticate: true');
        });
    });

    it('coerces an arbitrary parameter value to a boolean and never reflects it into the inline script', () => {
        const marker = `zzflag${faker.string.alphanumeric(8)}`;
        const raw = `</script><script>document.title='${marker}'</script><script>//`;
        renderResultPage(raw).then(res => {
            expect(res.status).to.eq(200);
            // The non-"true" value is coerced to the boolean literal false ...
            expect(res.body).to.contain('isAuthenticate: false');
            // ... and the raw request value never appears in the rendered response.
            expect(res.body).not.to.contain(marker);
        });
    });

    it('targets the message at the page origin rather than the wildcard', () => {
        renderResultPage('true').then(res => {
            expect(res.body).to.contain('window.location.origin');
            expect(res.body).not.to.contain(", '*')");
        });
    });
});
