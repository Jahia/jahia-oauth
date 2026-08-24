package org.jahia.modules.jahiaoauth.impl.token;

import java.util.List;

/**
 * Checks that an identity token was issued for this client, for this flow, and is still valid.
 * <p>
 * Four claims carry that, and no transport gives any of them:
 * <ul>
 * <li>{@code aud} names the client the token was issued for, so a token minted for another client
 * cannot be replayed here.</li>
 * <li>{@code exp} bounds how long the token stands.</li>
 * <li>{@code nonce} binds the token to the one sign-in that asked for it, which is what stops a token
 * obtained elsewhere from being presented on this flow. A token this flow cannot bind is refused, so
 * a flow that sends no nonce cannot sign a user in at all.</li>
 * <li>{@code iss} names the identity provider, checked when the connector states which one it
 * expects.</li>
 * </ul>
 */
public final class IdTokenValidator {

    /** Tolerated difference between the clock of the issuer and this one. */
    static final long CLOCK_SKEW_SECONDS = 60L;

    private IdTokenValidator() {
        // Utility class
    }

    /**
     * @param token the token returned by the token endpoint
     * @param clientId the client id this connector authenticates with, expected in {@code aud}
     * @param expectedIssuer the issuer the connector expects, or {@code null} to accept any
     * @param expectedNonce the nonce this flow sent, or {@code null} when it sent none
     * @param nowSeconds the current instant, in seconds since the epoch
     * @throws IdTokenException when a claim does not hold
     */
    public static void validate(IdToken token, String clientId, String expectedIssuer, String expectedNonce,
            long nowSeconds) {
        validateRefreshed(token, clientId, expectedIssuer, nowSeconds);

        // A token that arrives has to be bindable to this flow. When the flow recorded no nonce there is
        // nothing to compare it against, so the token is refused instead of accepted unbound. That choice
        // also decides how a later defect fails: a connector that stops sending a nonce breaks its
        // sign-in visibly, rather than accepting a token that answers some other flow.
        if (expectedNonce == null) {
            throw new IdTokenException("This flow sent no nonce, so the identity token cannot be bound to it");
        }
        String nonce = token.getClaim("nonce");
        if (nonce == null || !constantTimeEquals(expectedNonce, nonce)) {
            throw new IdTokenException("The identity token answers another sign-in");
        }
    }

    /**
     * The checks that hold for a token which answers no authorization request.
     * <p>
     * A refresh names a grant rather than a request, so no nonce binds the token it returns, and OpenID
     * Connect Core 12.2 states that such a token carries none. The audience, the expiry and the issuer
     * are checked as they are on a sign-in.
     *
     * @param token the identity token to check
     * @param clientId the client this deployment is registered as
     * @param expectedIssuer the issuer the connector states, or {@code null} to check none
     * @param nowSeconds the current time, in seconds since the epoch
     */
    public static void validateRefreshed(IdToken token, String clientId, String expectedIssuer,
            long nowSeconds) {
        List<String> audiences = token.getAudiences();
        if (audiences.isEmpty() || !audiences.contains(clientId)) {
            throw new IdTokenException("The identity token was issued for another client");
        }

        long expiry = token.getExpiry();
        if (expiry < 0) {
            throw new IdTokenException("The identity token states no expiry");
        }
        if (expiry + CLOCK_SKEW_SECONDS < nowSeconds) {
            throw new IdTokenException("The identity token expired");
        }

        if (expectedIssuer != null && !expectedIssuer.equals(token.getClaim("iss"))) {
            throw new IdTokenException("The identity token was issued by another identity provider");
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return java.security.MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                actual.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
