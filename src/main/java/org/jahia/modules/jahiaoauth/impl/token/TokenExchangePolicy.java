package org.jahia.modules.jahiaoauth.impl.token;

import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;
import com.github.scribejava.core.builder.api.DefaultApi20;
import org.apache.commons.lang.StringUtils;

/**
 * Answers two questions about a provider from the API object that talks to it.
 * <p>
 * The API object is the source of both answers, because it is the object that performs the token
 * call. It states its own endpoint, and the extractor it uses says whether the response carries an
 * identity token. Neither answer is something a connector states or omits, so a connector added
 * later is covered without doing anything.
 */
public final class TokenExchangePolicy {

    private static final String OPENID_SCOPE = "openid";

    private TokenExchangePolicy() {
        // Utility class
    }

    /**
     * @param api the API that performs the token call
     * @return whether the token response carries an identity token, read from the extractor the API
     *         uses to read that response
     */
    public static boolean returnsIdentityToken(DefaultApi20 api) {
        return api.getAccessTokenExtractor() instanceof OpenIdJsonTokenExtractor;
    }

    /**
     * Adds the scope an identity token depends on.
     * <p>
     * A provider issues an identity token only when the request asks for the {@code openid} scope. The
     * framework refuses a sign-in that returns no identity token from a provider that reads them, so an
     * administrator who left the scope field empty would get a broken sign-in. The requirement follows
     * from the provider, so the framework states it rather than asking a person to remember it.
     *
     * @param api the API that performs the token call
     * @param configuredScope the scope an administrator configured, may be {@code null} or blank
     * @return the scope to send
     */
    public static String scopeFor(DefaultApi20 api, String configuredScope) {
        if (!returnsIdentityToken(api)) {
            return configuredScope;
        }
        if (configuredScope == null || configuredScope.trim().isEmpty()) {
            return OPENID_SCOPE;
        }
        for (String scope : configuredScope.trim().split("\\s+")) {
            if (OPENID_SCOPE.equals(scope)) {
                return configuredScope;
            }
        }
        return OPENID_SCOPE + " " + configuredScope.trim();
    }

    /**
     * @param api the API that performs the token call
     * @return whether the token endpoint is served over TLS
     */
    public static boolean isSecureTokenEndpoint(DefaultApi20 api) {
        return !StringUtils.startsWithIgnoreCase(api.getAccessTokenEndpoint(), "http://");
    }
}
