package org.jahia.modules.jahiaoauth.impl.token;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * The claims of an identity token, read from the token endpoint.
 * <p>
 * This class does not check the signature of the token, and that is deliberate. OpenID Connect Core
 * section 3.1.3.7 states that a client which receives the token straight from the token endpoint may
 * use the TLS validation of that call in place of a signature check. The authorization code flow this
 * module runs does exactly that: the token arrives on a back channel call the module makes itself,
 * over TLS, authenticated with the client secret. A signature check would need the key set of the
 * issuer and a fetch of its own, and it would add no guarantee this flow lacks.
 * <p>
 * That exemption rests on a validated TLS connection, so two things are refused here. A token whose
 * header declares {@code alg: none} is refused, because such a token carries no signature at all and
 * the exemption is about not checking one, not about accepting one that was never made. And a token
 * endpoint that is not {@code https} is refused by the caller, because the exemption has no ground
 * without TLS.
 * <p>
 * What the signature cannot replace is the set of checks in {@link IdTokenValidator}: the audience,
 * the expiry, the issuer and the nonce. Those bind the token to this client and to this flow, and no
 * transport gives them.
 */
public final class IdToken {

    private static final String NO_SIGNATURE_ALGORITHM = "none";

    private final JSONObject claims;

    private IdToken(JSONObject claims) {
        this.claims = claims;
    }

    /**
     * @param token the serialized token, as three segments separated by a dot
     * @return the claims it carries
     * @throws IdTokenException when the token has no readable claim set
     */
    public static IdToken parse(String token) {
        if (token == null || token.isEmpty()) {
            throw new IdTokenException("The identity provider returned no identity token");
        }
        String[] segments = token.split("\\.");
        if (segments.length != 3) {
            throw new IdTokenException("The identity token does not carry three segments");
        }
        try {
            JSONObject header = new JSONObject(new String(Base64.getUrlDecoder().decode(segments[0]),
                    StandardCharsets.UTF_8));
            String algorithm = header.optString("alg", null);
            if (algorithm == null || NO_SIGNATURE_ALGORITHM.equalsIgnoreCase(algorithm)) {
                throw new IdTokenException("The identity token declares no signature algorithm");
            }
            byte[] payload = Base64.getUrlDecoder().decode(segments[1]);
            return new IdToken(new JSONObject(new String(payload, StandardCharsets.UTF_8)));
        } catch (JSONException | IllegalArgumentException e) {
            throw new IdTokenException("The claim set of the identity token could not be read", e);
        }
    }

    /**
     * @param name the name of the claim
     * @return its value, or {@code null} when the token carries no such claim
     */
    public String getClaim(String name) {
        return claims.isNull(name) ? null : claims.optString(name, null);
    }

    /**
     * @return every value of the {@code aud} claim, which the specification allows to be one string
     *         or an array of them
     */
    public List<String> getAudiences() {
        if (claims.isNull("aud")) {
            return Collections.emptyList();
        }
        Object aud = claims.opt("aud");
        if (aud instanceof String) {
            return Collections.singletonList((String) aud);
        }
        List<String> audiences = new ArrayList<>();
        org.json.JSONArray array = claims.optJSONArray("aud");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                audiences.add(array.optString(i));
            }
        }
        return audiences;
    }

    /**
     * @return the instant the token expires, in seconds since the epoch, or {@code -1} when it
     *         carries no expiry
     */
    public long getExpiry() {
        return claims.optLong("exp", -1L);
    }
}
