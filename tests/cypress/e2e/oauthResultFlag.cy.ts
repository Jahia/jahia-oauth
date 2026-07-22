import {createSite, deleteSite, enableModule, publishAndWaitJobEnding} from '@jahia/cypress';
import {faker} from '@faker-js/faker';

/**
 * The oauth-result view echoes the `isAuthenticate` request flag into an inline script that
 * calls window.opener.postMessage(...). That flag is only ever meaningfully true or false, so
 * the emitted token must be a well-formed boolean literal — never the raw request value dropped
 * verbatim into the script. These specs pin that contract at the live render layer.
 */
describe('oauth-result view — isAuthenticate flag rendering', () => {
    let siteKey: string;

    const resultUrl = (value: string): string =>
        `/cms/render/live/en/sites/${siteKey}/oauth-result.html?isAuthenticate=${encodeURIComponent(value)}`;

    beforeEach(() => {
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

    afterEach(() => {
        deleteSite(siteKey);
    });

    it('normalises a stray flag value to a boolean literal', () => {
        // A value that is not a clean boolean must not survive into the inline script verbatim.
        const stray = '</scr' + 'ipt><script>window.__resultProbe__=1</scr' + 'ipt><script>//';

        cy.request(resultUrl(stray)).its('body').then((body: string) => {
            // The flag is rendered as a boolean literal in the postMessage payload...
            expect(body).to.match(/isAuthenticate:\s*(true|false)\b/);
            // ...and the stray value never surfaces as active markup or a bare token.
            expect(body).not.to.contain('window.__resultProbe__');
            expect(body).not.to.contain('</scr' + 'ipt><script>');
        });
    });

    it('still reports a genuine successful result as true', () => {
        cy.request(resultUrl('true')).its('body').then((body: string) => {
            expect(body).to.contain('isAuthenticate: true');
        });
    });

    it('reports a non-success result as false', () => {
        cy.request(resultUrl('false')).its('body').then((body: string) => {
            expect(body).to.contain('isAuthenticate: false');
        });
    });
});
