package org.jahia.modules.jahiaoauth.action;

import org.jahia.api.Constants;
import org.jahia.api.content.JCRTemplate;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.params.valves.LoginUrlProvider;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.sites.JahiaSitesService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Answers the URL a sign-in starts from, for every OAuth connector, without a connector writing one.
 * <p>
 * Jahia asks a {@link LoginUrlProvider} for two different things through one method: a URL to render
 * a link, and a URL to redirect a request. A URL carrying the secrets of a flow would therefore mean
 * creating a flow for a render as well, and a rendered link is markup Jahia caches per fragment. This
 * provider answers the URL of a connect action, so a flow is created when a browser follows the link.
 * <p>
 * The action states which connector it serves and the name it answers to, so nothing has to be
 * declared twice. A connector that ships no connect action gets no login URL from here.
 * <p>
 * Jahia skips a provider that answers blank and tries the next one, so this provider is inert on a
 * site with no enabled OAuth connector. A connector may still register a {@link LoginUrlProvider} of
 * its own, and which of the two answers then depends on OSGi start order, because Jahia keeps the
 * last one bound at the head of its list.
 */
@Component(service = LoginUrlProvider.class, immediate = true)
public class OAuthLoginUrlProvider implements LoginUrlProvider {
    private static final Logger logger = LoggerFactory.getLogger(OAuthLoginUrlProvider.class);

    private SettingsService settingsService;
    private JahiaSitesService jahiaSitesService;
    private JCRTemplate jcrTemplate;
    private BundleContext bundleContext;

    @Reference
    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Reference
    public void setJahiaSitesService(JahiaSitesService jahiaSitesService) {
        this.jahiaSitesService = jahiaSitesService;
    }

    @Reference
    public void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public boolean hasCustomLoginUrl() {
        return true;
    }

    @Override
    public String getLoginUrl(HttpServletRequest request) {
        String siteKey = siteKey(request);
        if (siteKey == null) {
            return null;
        }
        ConnectToOAuthProvider action = connectAction(siteKey);
        if (action == null) {
            return null;
        }
        String home = homePath(siteKey);
        if (home == null) {
            logger.warn("Site {} has no home page, so no login URL names a page of it", siteKey);
            return null;
        }
        return request.getContextPath() + "/cms/render/live/" + language(siteKey) + home + "."
                + action.getName() + ".do?site=" + siteKey + "&"
                + ConnectToOAuthProvider.REDIRECT_PARAMETER + "=true";
    }

    /**
     * The connect action of the one OAuth connector enabled on a site.
     * <p>
     * A Jahia carries one connector type in practice. When several are enabled, the first by connector
     * name answers and the log names the others, so the answer never depends on the order services
     * happen to be registered in.
     */
    private ConnectToOAuthProvider connectAction(String siteKey) {
        List<ConnectToOAuthProvider> enabled = new ArrayList<>();
        try {
            ServiceReference<?>[] refs =
                    bundleContext.getAllServiceReferences(org.jahia.bin.Action.class.getName(), null);
            if (refs == null) {
                return null;
            }
            for (ServiceReference<?> ref : refs) {
                Object service = bundleContext.getService(ref);
                if (!(service instanceof ConnectToOAuthProvider)) {
                    continue;
                }
                ConnectToOAuthProvider candidate = (ConnectToOAuthProvider) service;
                if (candidate.getConnectorName() != null && isEnabled(siteKey, candidate.getConnectorName())) {
                    enabled.add(candidate);
                }
            }
        } catch (org.osgi.framework.InvalidSyntaxException e) {
            logger.error("Cannot read the registered actions", e);
            return null;
        }
        if (enabled.isEmpty()) {
            return null;
        }
        enabled.sort(Comparator.comparing(ConnectToOAuthProvider::getConnectorName));
        if (enabled.size() > 1) {
            logger.info("Site {} has {} OAuth connectors enabled, and the login URL names {}",
                    siteKey, enabled.size(), enabled.get(0).getConnectorName());
        }
        return enabled.get(0);
    }

    private boolean isEnabled(String siteKey, String connectorName) {
        ConnectorConfig config = settingsService.getConnectorConfig(siteKey, connectorName);
        return config != null && config.getBooleanProperty(JahiaAuthConstants.PROPERTY_IS_ENABLED);
    }

    private String siteKey(HttpServletRequest request) {
        try {
            return jcrTemplate.doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, null, session -> {
                JahiaSite site = jahiaSitesService.getSiteByServerName(request.getServerName(), session);
                if (site == null) {
                    site = jahiaSitesService.getDefaultSite(session);
                }
                return site == null ? JahiaSitesService.SYSTEM_SITE_KEY : site.getSiteKey();
            });
        } catch (RepositoryException e) {
            logger.debug("No site for server name {}", request.getServerName(), e);
            return null;
        }
    }

    /**
     * The path of the home page of a site. The connect action reads the site off the resource it runs
     * on, so the URL reaching it has to name a page of that site.
     */
    private String homePath(String siteKey) {
        try {
            return jcrTemplate.doExecuteWithSystemSessionAsUser(null, Constants.LIVE_WORKSPACE, null, session -> {
                JCRSiteNode site = (JCRSiteNode) session.getNode("/sites/" + siteKey);
                JCRNodeWrapper home = site.getHome();
                return home == null ? null : home.getPath();
            });
        } catch (RepositoryException e) {
            logger.debug("No home page for site {}", siteKey, e);
            return null;
        }
    }

    private String language(String siteKey) {
        try {
            return jcrTemplate.doExecuteWithSystemSessionAsUser(null, Constants.LIVE_WORKSPACE, null, session -> {
                JCRSiteNode site = (JCRSiteNode) session.getNode("/sites/" + siteKey);
                String language = site.getDefaultLanguage();
                return language == null ? "en" : language;
            });
        } catch (RepositoryException e) {
            return "en";
        }
    }
}
