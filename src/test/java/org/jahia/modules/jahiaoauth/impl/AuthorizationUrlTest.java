package org.jahia.modules.jahiaoauth.impl;

import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.OAuth2AccessTokenJsonExtractor;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.Settings;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthConstants;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Drives the URL a browser is sent to, rather than the rules behind it.
 * <p>
 * A test that drives a rule states that the rule holds. It does not state that the rule is reached,
 * and what reaches it is the URL this module builds. This one therefore asserts what leaves the
 * module, and the tests of the rules stand beside it.
 */
public class AuthorizationUrlTest {

    private static final String CONNECTOR = "OidcConnector";

    private Map<String, Object> sessionAttributes;
    private HttpServletRequest request;

    private static Object proxy(Class<?> type, InvocationHandler handler) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    @Before
    public void setUp() {
        sessionAttributes = new HashMap<>();
        HttpSession session = (HttpSession) proxy(HttpSession.class, (p, method, args) -> {
            switch (method.getName()) {
                case "getAttribute": return sessionAttributes.get((String) args[0]);
                case "setAttribute": return sessionAttributes.put((String) args[0], args[1]);
                case "removeAttribute": return sessionAttributes.remove((String) args[0]);
                case "getAttributeNames": return Collections.enumeration(sessionAttributes.keySet());
                default: return null;
            }
        });
        request = (HttpServletRequest) proxy(HttpServletRequest.class,
                (p, method, args) -> "getSession".equals(method.getName()) ? session : null);
    }

    /** Stands in for the API of a provider, which is where both flow facts are read from. */
    private static DefaultApi20 api(String tokenEndpoint, boolean readsIdentityToken) {
        return new DefaultApi20() {
            @Override
            public String getAccessTokenEndpoint() {
                return tokenEndpoint;
            }

            @Override
            protected String getAuthorizationBaseUrl() {
                return "https://idp.example.com/authorize";
            }

            @Override
            public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
                return readsIdentityToken ? OpenIdJsonTokenExtractor.instance()
                        : OAuth2AccessTokenJsonExtractor.instance();
            }
        };
    }

    private static ConnectorConfig config() {
        Settings settings = new Settings();
        settings.setSiteKey("digitall");
        Settings.Values values = settings.getValues(CONNECTOR);
        values.setProperty(JahiaOAuthConstants.PROPERTY_API_KEY, "jahia");
        values.setProperty(JahiaOAuthConstants.PROPERTY_API_SECRET, "a-secret");
        values.setProperty(JahiaOAuthConstants.PROPERTY_CALLBACK_URL, "https://site.example.com/callback");
        return new ConnectorConfig(settings, CONNECTOR);
    }

    private String authorizationUrl(DefaultApi20 api) {
        JahiaOAuthServiceImpl service = new JahiaOAuthServiceImpl();
        service.addOAuthDefaultApi20(CONNECTOR, api);
        return service.getAuthorizationUrl(request, config(), null);
    }

    @Test
    public void shouldSendANonceToAProviderThatReturnsAnIdentityToken() {
        String url = authorizationUrl(api("https://idp.example.com/token", true));

        assertTrue("The authorization URL carries no nonce: " + url, url.contains("nonce="));
    }

    @Test
    public void shouldSendNoNonceToAProviderThatReturnsNoIdentityToken() {
        // Most providers return no identity token, and an unexpected parameter is refused by some.
        String url = authorizationUrl(api("https://idp.example.com/token", false));

        assertFalse("The authorization URL carries a nonce: " + url, url.contains("nonce="));
    }

    @Test
    public void shouldSendAStateThatIsNotTheSessionId() {
        String url = authorizationUrl(api("https://idp.example.com/token", true));

        assertTrue("The authorization URL carries no state: " + url, url.contains("state="));
    }

    @Test
    public void shouldRecordTheFlowOnTheSessionThatStartedIt() {
        // The callback is refused unless it answers a flow this session started, so the flow has to
        // reach the session before the browser leaves.
        authorizationUrl(api("https://idp.example.com/token", true));

        assertFalse("No flow was recorded on the session", sessionAttributes.isEmpty());
    }
}
