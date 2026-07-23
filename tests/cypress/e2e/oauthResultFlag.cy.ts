import {
    createSite,
    deleteSite,
    enableModule,
    publishAndWaitJobEnding,
    addNode
} from '@jahia/cypress';
import {faker} from '@faker-js/faker';

/**
 * Robustness checks for the oauth-result view.
 *
 * The view emits the authentication outcome as an inline JavaScript value:
 *   window.opener.postMessage({authenticationIsDone: true, isAuthenticate: <flag>}, ...)
 *
 * `<flag>` is derived from the `isAuthenticate` request parameter. It must always render as a
 * well-formed boolean literal (`true` / `false`) regardless of what the caller passes, so the
 * emitted script is always syntactically valid and never carries arbitrary request text into the
 * page. These tests place the oauth-result component on a published page and render it in live
 * mode as a guest, exercising both a normal value and a hostile one.
 */
describe('oauth-result authentication flag', () => {
    let siteKey: string;
    let pagePath: string;

    // A value that, if reflected verbatim, would break out of the inline <script> block and
    // introduce a foreign statement. A well-formed boolean literal never contains this marker.
    const MARKER = 'oauthFlagProbe12345';
    const HOSTILE = `</script><script>window.${MARKER}=1</script><script>//`;

    beforeEach(() => {
        siteKey = faker.lorem.slug();
        // The home page of this template set does not render (empty 200); the connector
        // pages (e.g. google) do. Place the component on a rendering page.
        pagePath = `/sites/${siteKey}/google`;

        createSite(siteKey, {
            locale: 'en',
            serverName: 'localhost',
            templateSet: 'jahia-oauth-test-module'
        });
        enableModule('jahia-oauth', siteKey);

        // The page template renders a `pagecontent` area; place the component there.
        addNode({
            parentPathOrId: pagePath,
            primaryNodeType: 'jnt:contentList',
            name: 'pagecontent'
        });
        addNode({
            parentPathOrId: `${pagePath}/pagecontent`,
            primaryNodeType: 'joant:oauthResult',
            name: 'authentication-result'
        });
        publishAndWaitJobEnding(`/sites/${siteKey}`);
    });

    afterEach(() => {
        deleteSite(siteKey);
    });

    const renderLive = (isAuthenticate: string) =>
        cy.request({
            url: `${Cypress.config().baseUrl}/cms/render/live/en${pagePath}.html`,
            qs: {isAuthenticate},
            failOnStatusCode: false
        });

    it('emits a boolean literal for a normal affirmative value', () => {
        renderLive('true').then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('isAuthenticate: true');
        });
    });

    it('coerces an unexpected value to a boolean literal rather than reflecting it', () => {
        renderLive(HOSTILE).then(response => {
            expect(response.status).to.eq(200);
            // The flag must degrade to a well-formed boolean, not carry the raw value through.
            expect(response.body).to.contain('isAuthenticate: false');
            expect(response.body).not.to.contain(MARKER);
        });
    });
});
