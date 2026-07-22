import {
    createSite,
    deleteSite,
    enableModule,
    publishAndWaitJobEnding
} from '@jahia/cypress';
import {faker} from '@faker-js/faker';

/**
 * The OAuth result page hands the authentication state back to the opener window
 * through an inline script. That state is a boolean, and the view must always emit
 * it as a well-formed boolean literal derived from the request — regardless of what
 * the `isAuthenticate` query parameter actually contains. These tests pin that
 * contract: the flag is rendered as `true`/`false` and an unexpected value is
 * coerced, never echoed into the script verbatim.
 */
describe('OAuth result page — authentication-state flag rendering', () => {
    let siteKey: string;

    const renderUrl = (rawFlag: string): string =>
        `/cms/render/live/en/sites/${siteKey}/oauth-result.html?isAuthenticate=${rawFlag}`;

    beforeEach(() => {
        siteKey = faker.lorem.slug();
        createSite(siteKey, {
            locale: 'en',
            serverName: 'localhost',
            templateSet: 'jahia-oauth-test-module'
        });
        publishAndWaitJobEnding(`/sites/${siteKey}`);
        enableModule('jahia-oauth', siteKey);
        publishAndWaitJobEnding(`/sites/${siteKey}`);
        // The result page is public; exercise it as an anonymous visitor.
        cy.logout();
    });

    afterEach(() => {
        cy.logout();
        deleteSite(siteKey);
    });

    it('emits a boolean literal for the authenticated flag', () => {
        cy.request(renderUrl('true')).then(res => {
            expect(res.status).to.eq(200);
            expect(res.body).to.contain('isAuthenticate: true');
        });
    });

    it('emits a boolean literal for the unauthenticated flag', () => {
        cy.request(renderUrl('false')).then(res => {
            expect(res.status).to.eq(200);
            expect(res.body).to.contain('isAuthenticate: false');
        });
    });

    it('coerces an unexpected flag value to a boolean literal instead of echoing it', () => {
        const rawFlag = encodeURIComponent("</script><script>document.title='PROBE-HIT'</script><script>//");
        cy.request(renderUrl(rawFlag)).then(res => {
            expect(res.status).to.eq(200);
            // The value is coerced to a well-formed boolean literal...
            expect(res.body).to.contain('isAuthenticate: false');
            // ...and never lands in the page as raw markup.
            expect(res.body).not.to.contain('PROBE-HIT');
        });
    });
});
