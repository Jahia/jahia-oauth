import {
    addNode,
    createSite,
    deleteSite,
    enableModule,
    publishAndWaitJobEnding
} from '@jahia/cypress';
import {faker} from '@faker-js/faker';

/**
 * The oauthResult view emits the authentication result flag into an inline
 * <script> block as a JSON value. That value must always be a well-formed
 * boolean literal derived server-side from the request — never the raw request
 * input echoed back into the script. This suite renders the view in live mode
 * as a guest and asserts the emitted flag stays a well-formed literal regardless
 * of what the caller supplies for the isAuthenticate parameter.
 */
describe('oauthResult view — authentication flag rendering', () => {
    let siteKey: string;
    // A distinctive token that, if the raw parameter were reflected verbatim,
    // would appear in the rendered script. Its absence proves the value is not
    // echoed back.
    const RAW_MARKER = 'oauthResultRawEcho';

    beforeEach(() => {
        siteKey = faker.lorem.slug();
        createSite(siteKey, {
            locale: 'en',
            serverName: 'localhost',
            templateSet: 'jahia-oauth-test-module'
        });
        enableModule('jahia-oauth', siteKey);

        // Place the oauthResult component on a page that renders in live mode.
        // (The template set's home page does not render; the connector pages do.)
        addNode({
            parentPathOrId: `/sites/${siteKey}/google/pagecontent`,
            primaryNodeType: 'joant:oauthResult',
            name: 'authentication-result'
        });

        publishAndWaitJobEnding(`/sites/${siteKey}`);
        cy.logout();
    });

    afterEach(() => {
        deleteSite(siteKey);
    });

    it('emits a well-formed boolean literal for the isAuthenticate flag and never reflects raw request input', () => {
        // A value that is neither the boolean "true" nor "false": if it were
        // emitted as-is it would break out of the inline <script> block.
        const rawValue = `</script><script>document.title='${RAW_MARKER}'</script><script>//`;

        cy.request({
            url: `/cms/render/live/en/sites/${siteKey}/google.html`,
            qs: {isAuthenticate: rawValue},
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            const body = response.body as string;

            // The component actually rendered its inline script.
            expect(body).to.contain('window.opener.postMessage');

            // The raw request value must not be reflected verbatim into the page.
            expect(body).to.not.contain(RAW_MARKER);
            expect(body).to.not.contain('</script><script>');

            // The flag must be emitted as a well-formed boolean literal. With a
            // non-"true" input the server-side coercion must yield false.
            expect(body).to.match(/isAuthenticate:\s*false\b/);
        });
    });

    it('emits true for the isAuthenticate flag when the request supplies true', () => {
        cy.request({
            url: `/cms/render/live/en/sites/${siteKey}/google.html`,
            qs: {isAuthenticate: 'true'},
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            const body = response.body as string;
            expect(body).to.contain('window.opener.postMessage');
            expect(body).to.match(/isAuthenticate:\s*true\b/);
        });
    });
});
