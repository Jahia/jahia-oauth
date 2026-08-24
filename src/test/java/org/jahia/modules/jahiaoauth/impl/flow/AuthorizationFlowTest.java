package org.jahia.modules.jahiaoauth.impl.flow;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AuthorizationFlowTest {

    private Map<String, Object> attributes;
    private HttpServletRequest request;

    private static Object proxy(Class<?> type, InvocationHandler handler) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    @Before
    public void setUp() {
        attributes = new HashMap<>();
        HttpSession session = (HttpSession) proxy(HttpSession.class, (p, method, args) -> {
            switch (method.getName()) {
                case "getAttribute": return attributes.get((String) args[0]);
                case "setAttribute": return attributes.put((String) args[0], args[1]);
                case "removeAttribute": return attributes.remove((String) args[0]);
                default: return null;
            }
        });
        request = (HttpServletRequest) proxy(HttpServletRequest.class,
                (p, method, args) -> "getSession".equals(method.getName()) ? session : null);
    }

    @Test
    public void shouldCarryTwoDistinctSecrets() {
        // One value for both would publish the nonce in the callback URL, and the nonce is there to
        // prove a binding a URL cannot keep.
        AuthorizationFlow flow = AuthorizationFlow.start(request, "OidcConnector", true);

        assertNotNull(flow.getState());
        assertNotNull(flow.getNonce());
        assertNotEquals(flow.getState(), flow.getNonce());
        assertTrue(flow.getState().length() > 32);
    }

    @Test
    public void shouldCarryNoNonceForAFlowThatAsksForNoIdentityToken() {
        AuthorizationFlow flow = AuthorizationFlow.start(request, "GithubApi", false);

        assertNotNull(flow.getState());
        assertNull(flow.getNonce());
    }

    @Test
    public void shouldReadBackTheFlowItsStateNames() {
        AuthorizationFlow started = AuthorizationFlow.start(request, "OidcConnector", true);

        AuthorizationFlow consumed = AuthorizationFlow.consume(request, "OidcConnector", started.getState());

        assertNotNull(consumed);
        assertEquals(started.getNonce(), consumed.getNonce());
    }

    @Test
    public void shouldServeOnlyOnce() {
        AuthorizationFlow started = AuthorizationFlow.start(request, "OidcConnector", true);
        AuthorizationFlow.consume(request, "OidcConnector", started.getState());

        assertNull(AuthorizationFlow.consume(request, "OidcConnector", started.getState()));
    }

    @Test
    public void shouldRefuseAStateItNeverIssued() {
        AuthorizationFlow started = AuthorizationFlow.start(request, "OidcConnector", true);

        assertNull(AuthorizationFlow.consume(request, "OidcConnector", "a-state-from-somewhere-else"));
        // The flow this session started is untouched, so the state from elsewhere consumed nothing.
        assertNotNull(AuthorizationFlow.consume(request, "OidcConnector", started.getState()));
    }

    @Test
    public void shouldRefuseAnAbsentState() {
        AuthorizationFlow.start(request, "OidcConnector", true);

        assertNull(AuthorizationFlow.consume(request, "OidcConnector", null));
        assertNull(AuthorizationFlow.consume(request, "OidcConnector", ""));
    }

    @Test
    public void shouldKeepOneFlowPerConnector() {
        AuthorizationFlow oidc = AuthorizationFlow.start(request, "OidcConnector", true);
        AuthorizationFlow keycloak = AuthorizationFlow.start(request, "KeycloakApi", true);

        assertNotNull(AuthorizationFlow.consume(request, "KeycloakApi", keycloak.getState()));
        assertNotNull(AuthorizationFlow.consume(request, "OidcConnector", oidc.getState()));
    }

    @Test
    public void shouldRefuseAStateOfAnotherConnector() {
        AuthorizationFlow oidc = AuthorizationFlow.start(request, "OidcConnector", true);

        assertNull(AuthorizationFlow.consume(request, "KeycloakApi", oidc.getState()));
    }

    @Test
    public void shouldSurviveARedeploymentOfThisModule() {
        // The session holds a document, not an instance. After a redeployment an instance would belong
        // to the classloader of the previous bundle, and every callback would then be refused.
        AuthorizationFlow started = AuthorizationFlow.start(request, "OidcConnector", true);

        Object recorded = attributes.get(AuthorizationFlow.SESSION_ATTRIBUTE_PREFIX + "OidcConnector");

        assertTrue(recorded instanceof String);
        assertTrue(((String) recorded).contains(started.getState()));
    }

    @Test
    public void shouldRefuseAndRemoveADocumentItCannotRead() {
        attributes.put(AuthorizationFlow.SESSION_ATTRIBUTE_PREFIX + "OidcConnector", "{\"schemaVersion\":99}");

        assertNull(AuthorizationFlow.consume(request, "OidcConnector", "any-state"));
        // Removed, so the next sign-in starts clean rather than meeting the same document.
        assertNull(attributes.get(AuthorizationFlow.SESSION_ATTRIBUTE_PREFIX + "OidcConnector"));
    }

    @Test
    public void shouldRefuseSomethingThatIsNotADocument() {
        attributes.put(AuthorizationFlow.SESSION_ATTRIBUTE_PREFIX + "OidcConnector", new Object());

        assertNull(AuthorizationFlow.consume(request, "OidcConnector", "any-state"));
    }
}
