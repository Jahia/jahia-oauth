import {
    addNode,
    createSite,
    deleteSite,
    enableModule,
    publishAndWaitJobEnding
} from '@jahia/cypress';
import {faker} from '@faker-js/faker';

/**
 * The oauthResult view echoes the `isAuthenticate` request parameter into an inline
 * script as the value of an object property. That position expects a boolean literal,
 * so the view must always emit a well-formed `true` / `false` token derived from the
 * parameter — never the raw parameter text. This suite renders the component in live
 * mode and asserts the emitted script stays well-formed whatever the parameter holds.
 */
describe('oauthResult view - isAuthenticate parameter rendering', () => {
    let siteKey: string;

    // The test template set ships page templates for the connector pages (facebook, google,
    // linkedin, github) but not for `home`, so `home.html` renders empty. Render on `facebook`,
    // which has a working template and a `pagecontent` area, in live mode.
    const renderUrl = (value: string) =>
        `/cms/render/live/en/sites/${siteKey}/facebook.html?isAuthenticate=${value}`;

    beforeEach(() => {
        siteKey = faker.lorem.slug();
        createSite(siteKey, {
            locale: 'en',
            serverName: 'localhost',
            templateSet: 'jahia-oauth-test-module'
        });
        enableModule('jahia-oauth', siteKey);

        // Render the oauthResult component from the facebook page content area (live mode).
        addNode({
            parentPathOrId: `/sites/${siteKey}/facebook/pagecontent`,
            name: 'authentication-result',
            primaryNodeType: 'joant:oauthResult'
        });
        publishAndWaitJobEnding(`/sites/${siteKey}`);
        cy.logout();
    });

    afterEach(() => {
        deleteSite(siteKey);
    });

    it('emits a false boolean literal when the parameter is not the string true', () => {
        const raw = encodeURIComponent("</script><script>document.title='probe'</script><script>//");
        cy.request(renderUrl(raw)).then(response => {
            expect(response.status).to.eq(200);
            // The property value is a boolean literal derived from the parameter...
            expect(response.body).to.contain('isAuthenticate: false');
            // ...and the raw parameter text never reaches the script structure.
            expect(response.body).to.not.contain("document.title='probe'");
            expect(response.body).to.not.contain('</script><script>');
        });
    });

    it('emits a true boolean literal for the authenticated flow', () => {
        cy.request(renderUrl('true')).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('isAuthenticate: true');
        });
    });
});
