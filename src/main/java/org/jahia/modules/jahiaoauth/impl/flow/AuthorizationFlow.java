package org.jahia.modules.jahiaoauth.impl.flow;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * One sign-in through an identity provider, from the moment the browser leaves to the moment it comes
 * back.
 * <p>
 * A flow carries two secrets. Both are created when the flow starts, both are used once, and both are
 * checked when the identity provider calls back. One flow holds the two, so neither is created
 * without the other, and neither is checked without the other.
 * <p>
 * The two secrets stay two distinct values, because they come back by different routes. The state
 * returns in the query string of the callback, and the nonce returns inside the identity token. One
 * value used for both would publish the nonce in a URL, so in an access log and in a referrer, and
 * the binding the nonce is there to prove would be worth nothing.
 * <p>
 * This class sits in a package the bundle does not export, so no other module can start or consume a
 * flow through it. That bounds who can call this class, and it does not bound who can read the value
 * it stores: the session attribute holds a document any code holding the request can read or replace.
 * A design that has to resist that reads the state from a signed value instead of from the session.
 * <p>
 * The session holds the flow as a JSON document, and not as an instance of this class. A string carries
 * no class identity, so the document survives a redeployment of this module. An instance does not: after
 * a redeployment the stored object belongs to the classloader of the previous bundle, an
 * {@code instanceof} test against the new class fails, and every callback is then refused. The mapper
 * results of the framework travel as a document for the same reason.
 * <p>
 * A flow lives on the session that started it. A callback that arrives with no session cookie
 * therefore finds no flow and is refused, which is the case a POST from another site produces under a
 * {@code SameSite=Lax} cookie. Read the plan of the rework before you move this state elsewhere.
 */
public final class AuthorizationFlow {

    static final String SESSION_ATTRIBUTE_PREFIX = "org.jahia.modules.jahiaoauth.flow.";

    private static final int SCHEMA_VERSION = 1;
    private static final String SCHEMA_VERSION_FIELD = "schemaVersion";
    private static final String STATE_FIELD = "state";
    private static final String NONCE_FIELD = "nonce";
    private static final int SECRET_BYTE_LENGTH = 32;  // 256 bits of entropy
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationFlow.class);

    private final String state;
    private final String nonce;

    private AuthorizationFlow(String state, String nonce) {
        this.state = state;
        this.nonce = nonce;
    }

    /**
     * Starts a flow and records it on the session of the request.
     *
     * @param request the request that starts the flow
     * @param connectorName the connector the flow runs through, so two connectors do not share a flow
     * @param withNonce whether this flow asks for an identity token and therefore needs a nonce
     * @return the flow, whose secrets travel to the identity provider
     */
    public static AuthorizationFlow start(HttpServletRequest request, String connectorName, boolean withNonce) {
        AuthorizationFlow flow = new AuthorizationFlow(secret(), withNonce ? secret() : null);
        request.getSession().setAttribute(attributeName(connectorName), flow.toDocument());
        return flow;
    }

    /**
     * Reads back the flow the state belongs to, and consumes it so it cannot serve twice.
     * <p>
     * The recorded flow is left alone when the state does not match, so a callback answering no
     * recorded state leaves a sign-in that is under way in place.
     *
     * @param request the callback request
     * @param connectorName the connector the callback is for
     * @param receivedState the state the identity provider returned
     * @return the flow, or {@code null} when no flow of this session answers that state
     */
    public static AuthorizationFlow consume(HttpServletRequest request, String connectorName, String receivedState) {
        if (StringUtils.isBlank(receivedState)) {
            return null;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            logger.warn("Rejected a callback for connector {}: the request carries no session, so it names no flow.",
                    connectorName);
            return null;
        }
        String attribute = attributeName(connectorName);
        Object recorded = session.getAttribute(attribute);
        if (!(recorded instanceof String)) {
            logger.warn("Rejected a callback for connector {}: this session started no sign-in through it.",
                    connectorName);
            return null;
        }
        AuthorizationFlow flow = fromDocument((String) recorded);
        if (flow == null) {
            logger.warn("Rejected a callback for connector {}: the recorded sign-in could not be read, so"
                    + " it is removed and a new sign-in starts clean.", connectorName);
            session.removeAttribute(attribute);
            return null;
        }
        if (!constantTimeEquals(flow.state, receivedState)) {
            logger.warn("Rejected a callback for connector {}: the state does not answer the sign-in this"
                    + " session started. The recorded sign-in is left alone.", connectorName);
            return null;
        }
        session.removeAttribute(attribute);
        return flow;
    }

    private String toDocument() {
        try {
            JSONObject document = new JSONObject();
            document.put(SCHEMA_VERSION_FIELD, SCHEMA_VERSION);
            document.put(STATE_FIELD, state);
            if (nonce != null) {
                document.put(NONCE_FIELD, nonce);
            }
            return document.toString();
        } catch (JSONException e) {
            // A JSONObject refuses a null name and a non-finite number, and this method writes neither.
            throw new IllegalStateException("Could not write the sign-in", e);
        }
    }

    private static AuthorizationFlow fromDocument(String document) {
        try {
            JSONObject parsed = new JSONObject(document);
            if (parsed.getInt(SCHEMA_VERSION_FIELD) != SCHEMA_VERSION) {
                return null;
            }
            return new AuthorizationFlow(parsed.getString(STATE_FIELD), parsed.optString(NONCE_FIELD, null));
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * @return the value to send as the {@code state} parameter
     */
    public String getState() {
        return state;
    }

    /**
     * @return the value to send as the {@code nonce} parameter, and to expect in the identity token,
     *         or {@code null} when this flow asks for no identity token
     */
    public String getNonce() {
        return nonce;
    }

    private static String secret() {
        byte[] bytes = new byte[SECRET_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String attributeName(String connectorName) {
        return SESSION_ATTRIBUTE_PREFIX + connectorName;
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
