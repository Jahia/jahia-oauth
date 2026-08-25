import {
    addNode,
    createSite,
    deleteSite,
    enableModule,
    publishAndWaitJobEnding
} from '@jahia/cypress';

/*
 * The connector button view and the result view each place a value into markup: an editor-set
 * title into the button label, an editor-set CSS class and HTML id into attributes, and the
 * sign-in outcome from the request into an inline script.
 *
 * Every value below holds characters that are syntax in the context it lands in. A view that
 * stopped encoding its output would therefore change the SHAPE of the markup rather than the
 * visible text, which is what each assertion measures.
 */
const LABEL = 'Sign in "now" & <fast>';
const CSS_CLASS = 'probe-button my "quoted" class';
const HTML_ID = 'probe-id" data-extra="1';

// What the request carries when the outcome is not a boolean at all.
const NON_BOOLEAN_OUTCOME = 'true};window.probe=1;//';

describe('OAuth button and result rendering', () => {
    const siteKey = 'oauthRenderingTestSite';
    const area = `/sites/${siteKey}/google/pagecontent`;
    const page = `/sites/${siteKey}/google.html`;

    before(() => {
        deleteSite(siteKey);
        createSite(siteKey, {
            locale: 'en',
            serverName: 'localhost',
            templateSet: 'jahia-oauth-test-module'
        });
        publishAndWaitJobEnding(`/sites/${siteKey}`);
        enableModule('jahia-oauth', siteKey);

        addNode({
            parentPathOrId: area,
            name: 'renderingProbeButton',
            primaryNodeType: 'joant:googleButton',
            properties: [
                {name: 'jcr:title', value: LABEL, language: 'en'},
                {name: 'cssClass', value: CSS_CLASS},
                {name: 'htmlId', value: HTML_ID}
            ]
        });
        addNode({
            parentPathOrId: area,
            name: 'renderingProbeResult',
            primaryNodeType: 'joant:oauthResult'
        });
        publishAndWaitJobEnding(`/sites/${siteKey}`, ['en']);
    });

    after(() => {
        deleteSite(siteKey);
    });

    beforeEach(() => {
        // Both views render for an unauthenticated visitor only. An authenticated probe sees
        // no button at all, which would make every assertion below vacuous rather than failing.
        cy.logout();
        cy.clearCookies();
    });

    it('renders a configured title as the text of the button label', () => {
        cy.visit(page);
        cy.get('button.probe-button p.btn-text').should('have.text', LABEL);
    });

    it('keeps a configured CSS class and HTML id inside their own attributes', () => {
        cy.visit(page);
        cy.get('button.probe-button').should($button => {
            expect($button.attr('class'), 'class attribute').to.contain('my "quoted" class');
            expect($button.attr('id'), 'id attribute').to.equal(HTML_ID);
            expect($button[0].getAttributeNames(), 'attributes carried by the button')
                .to.have.members(['class', 'onclick', 'id']);
        });
    });

    it('reports a non-boolean outcome as false rather than as itself', () => {
        cy.request({url: page, qs: {isAuthenticate: NON_BOOLEAN_OUTCOME}}).then(response => {
            expect(response.body).to.contain('isAuthenticate: false}');
            expect(response.body).to.not.contain('window.probe');
        });
    });

    it('reports a genuine outcome unchanged, and addresses the opener by origin', () => {
        cy.request({url: page, qs: {isAuthenticate: 'true'}}).then(response => {
            expect(response.body).to.contain('isAuthenticate: true}');
            expect(response.body).to.contain('}, window.location.origin)');
        });
    });

    it('ignores a message that did not come from this page own origin', () => {
        cy.request(page).then(response => {
            expect(response.body).to.contain('event.origin !== window.location.origin');
        });
    });
});
