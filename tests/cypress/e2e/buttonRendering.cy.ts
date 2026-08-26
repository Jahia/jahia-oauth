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
 *
 * Each assertion reads rendered output. None of them matches the source text of a script, so
 * none of them would stay green if the code it covers became unreachable.
 */
const LABEL = 'Sign in "now" & <fast>';
const CSS_CLASS = 'probe-button my "quoted" class';
const HTML_ID = 'probe-id" data-extra="1';
const PROBE = '[id^="probe-id"]';

// What the request carries when the outcome is not a boolean at all.
const NON_BOOLEAN_OUTCOME = 'true};window.probe=1;//';

// The four connector buttons the test module places on a page of their own. The same label and
// attribute code is copied across all of them, so covering one would not cover the family.
// franceConnectButton renders its label as SVG text and carries no label sink, and the test
// module ships no page for it.
// The size and colour properties are set explicitly because they are interpolated into the
// same class attribute, immediately before cssClass. Leaving one empty runs the two values
// together into a single class token, which is not what an editor-created node looks like.
const BUTTONS = [
    {page: 'google', nodeType: 'joant:googleButton', variant: {name: 'buttonColor', value: 'light'}},
    {page: 'github', nodeType: 'joant:githubButton', variant: {name: 'buttonColor', value: 'light'}},
    {page: 'linkedin', nodeType: 'joant:linkedInButton', variant: {name: 'buttonSize', value: 'large'}},
    {page: 'facebook', nodeType: 'joant:facebookButton', variant: {name: 'buttonSize', value: 'large'}}
];

describe('OAuth button and result rendering', () => {
    const siteKey = 'oauthRenderingTestSite';

    before(() => {
        deleteSite(siteKey);
        createSite(siteKey, {
            locale: 'en',
            serverName: 'localhost',
            templateSet: 'jahia-oauth-test-module'
        });
        publishAndWaitJobEnding(`/sites/${siteKey}`);
        enableModule('jahia-oauth', siteKey);

        BUTTONS.forEach(({page, nodeType, variant}) => {
            addNode({
                parentPathOrId: `/sites/${siteKey}/${page}/pagecontent`,
                name: 'renderingProbeButton',
                primaryNodeType: nodeType,
                properties: [
                    {name: 'jcr:title', value: LABEL, language: 'en'},
                    {name: 'cssClass', value: CSS_CLASS},
                    {name: 'htmlId', value: HTML_ID},
                    variant
                ]
            });
        });
        addNode({
            parentPathOrId: `/sites/${siteKey}/google/pagecontent`,
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

    BUTTONS.forEach(({page}) => {
        it(`renders a configured title as the text of the ${page} button label`, () => {
            cy.visit(`/sites/${siteKey}/${page}.html`);
            cy.get(`${PROBE} p.btn-text`).should('have.text', LABEL);
        });

        it(`keeps a configured CSS class and HTML id inside their own attributes on ${page}`, () => {
            cy.visit(`/sites/${siteKey}/${page}.html`);
            cy.get(PROBE).should($button => {
                expect($button.attr('class'), 'class attribute').to.contain('my "quoted" class');
                expect($button.attr('id'), 'id attribute').to.equal(HTML_ID);
                expect($button[0].getAttributeNames(), 'attributes carried by the button')
                    .to.not.include('data-extra');
            });
        });
    });

    it('reports a non-boolean outcome as false rather than as itself', () => {
        cy.request({
            url: `/sites/${siteKey}/google.html`,
            qs: {isAuthenticate: NON_BOOLEAN_OUTCOME}
        }).then(response => {
            expect(response.body).to.contain('isAuthenticate: false}');
            expect(response.body).to.not.contain('window.probe');
        });
    });

    it('reports a genuine outcome unchanged', () => {
        cy.request({
            url: `/sites/${siteKey}/google.html`,
            qs: {isAuthenticate: 'true'}
        }).then(response => {
            expect(response.body).to.contain('isAuthenticate: true}');
        });
    });
});
