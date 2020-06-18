(function() {
    window.jahia.i18n.loadNamespaces('jahia-oauth');
    window.jahia.uiExtender.registry.add('adminRoute', 'jahia-oauth', {
        targets: ['administration-sites:99'],
        icon: null,
        label: 'jahia-oauth:label',
        isSelectable: true,
        requireModuleInstalledOnSite: 'jahia-oauth',
        iframeUrl: window.contextJsParameters.contextPath + '/cms/editframe/default/$lang/sites/$site-key.oauth-connector-site-settings.html?redirect=false'
    });
})();
