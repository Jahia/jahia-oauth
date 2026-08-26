package org.jahia.modules.jahiaoauth.impl.token;

import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IdTokenValidatorTest {

    private static final long NOW = 1_800_000_000L;
    private static final String CLIENT = "jahia";

    private static String segment(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    /** Builds a token the way an identity provider does. The signature is not checked, the header is. */
    private static String serialize(String algorithm, JSONObject claims) {
        return segment("{\"alg\":\"" + algorithm + "\",\"typ\":\"JWT\"}")
                + "." + segment(claims.toString()) + ".c2lnbmF0dXJl";
    }

    private static IdToken token(JSONObject claims) {
        return IdToken.parse(serialize("RS256", claims));
    }

    private static JSONObject validClaims() {
        JSONObject claims = new JSONObject();
        claims.put("iss", "http://keycloak:8180/realms/jahialab");
        claims.put("aud", CLIENT);
        claims.put("exp", NOW + 300);
        claims.put("sub", "8f3c1e4a");
        claims.put("nonce", "the-nonce-of-this-flow");
        return claims;
    }

    private static void expectRefusal(String because, Runnable validation) {
        try {
            validation.run();
            fail("Must be refused: " + because);
        } catch (IdTokenException e) {
            // expected
        }
    }

    @Test
    public void shouldAcceptATokenIssuedForThisClientAndThisFlow() {
        IdTokenValidator.validate(token(validClaims()), CLIENT,
                "http://keycloak:8180/realms/jahialab", "the-nonce-of-this-flow", NOW);
    }

    @Test
    public void shouldRefuseATokenIssuedForAnotherClient() {
        JSONObject claims = validClaims();
        claims.put("aud", "another-client");

        expectRefusal("the audience names another client",
                () -> IdTokenValidator.validate(token(claims), CLIENT, null, "the-nonce-of-this-flow", NOW));
    }

    @Test
    public void shouldRefuseATokenThatAnswersAnotherSignIn() {
        // The nonce is what ties a token to one sign-in, so a token naming another flow's nonce is
        // refused even when every other claim stands.
        JSONObject claims = validClaims();
        claims.put("nonce", "the-nonce-of-another-flow");

        expectRefusal("the nonce answers another flow",
                () -> IdTokenValidator.validate(token(claims), CLIENT, null, "the-nonce-of-this-flow", NOW));
    }

    @Test
    public void shouldRefuseATokenThatCarriesNoNonceWhenTheFlowSentOne() {
        JSONObject claims = validClaims();
        claims.remove("nonce");

        expectRefusal("the token carries no nonce",
                () -> IdTokenValidator.validate(token(claims), CLIENT, null, "the-nonce-of-this-flow", NOW));
    }

    @Test
    public void shouldRefuseATokenThisFlowCannotBind() {
        // The flow recorded no nonce, so no comparison here can tie the token to this sign-in. Accepting
        // such a token is the defect this validator exists for. Refusing it also decides how a bug fails:
        // a connector whose flow stops sending a nonce breaks the sign-in visibly, rather than signing a
        // user in with a token that answers some other flow.
        expectRefusal("the flow sent no nonce, so the token is bound to nothing",
                () -> IdTokenValidator.validate(token(validClaims()), CLIENT, null, null, NOW));
    }

    @Test
    public void shouldRefuseATokenThisFlowCannotBindEvenWhenItCarriesNoNonce() {
        JSONObject claims = validClaims();
        claims.remove("nonce");

        expectRefusal("neither side states a nonce, so the token is bound to nothing",
                () -> IdTokenValidator.validate(token(claims), CLIENT, null, null, NOW));
    }

    @Test
    public void shouldRefuseAnExpiredToken() {
        JSONObject claims = validClaims();
        claims.put("exp", NOW - IdTokenValidator.CLOCK_SKEW_SECONDS - 1);

        expectRefusal("the token expired",
                () -> IdTokenValidator.validate(token(claims), CLIENT, null, "the-nonce-of-this-flow", NOW));
    }

    @Test
    public void shouldTolerateAClockThatDiffersALittle() {
        JSONObject claims = validClaims();
        claims.put("exp", NOW - 1);

        IdTokenValidator.validate(token(claims), CLIENT, null, "the-nonce-of-this-flow", NOW);
    }

    @Test
    public void shouldRefuseATokenWithNoExpiry() {
        JSONObject claims = validClaims();
        claims.remove("exp");

        expectRefusal("the token states no expiry",
                () -> IdTokenValidator.validate(token(claims), CLIENT, null, "the-nonce-of-this-flow", NOW));
    }

    @Test
    public void shouldRefuseATokenFromAnotherIssuer() {
        expectRefusal("the issuer is not the expected one", () -> IdTokenValidator.validate(
                token(validClaims()), CLIENT, "http://another-idp/realms/x", "the-nonce-of-this-flow", NOW));
    }

    @Test
    public void shouldAcceptAnAudienceGivenAsAnArray() {
        // The specification allows aud to be one string or an array of them.
        JSONObject claims = validClaims();
        claims.put("aud", new org.json.JSONArray().put("another-client").put(CLIENT));

        IdTokenValidator.validate(token(claims), CLIENT, null, "the-nonce-of-this-flow", NOW);
    }

    @Test
    public void shouldReadTheClaimsOfAToken() {
        IdToken parsed = token(validClaims());

        assertEquals("8f3c1e4a", parsed.getClaim("sub"));
        assertEquals(NOW + 300, parsed.getExpiry());
        assertTrue(parsed.getAudiences().contains(CLIENT));
        assertNull(parsed.getClaim("absent"));
    }

    @Test
    public void shouldRefuseSomethingThatIsNotAToken() {
        expectRefusal("the value is not a token", () -> IdToken.parse("not-a-token"));
        expectRefusal("the value is empty", () -> IdToken.parse(""));
        expectRefusal("the value is absent", () -> IdToken.parse(null));
        expectRefusal("the claim set is not JSON",
                () -> IdToken.parse(segment("{\"alg\":\"RS256\"}") + "." + segment("not json") + ".sig"));
    }

    @Test
    public void shouldRefuseATokenThatCarriesNoSignature() {
        // The exemption from checking a signature covers a token whose signature this flow chose not to
        // verify. It does not cover a token that never carried one.
        expectRefusal("the algorithm is none", () -> IdToken.parse(serialize("none", validClaims())));
        expectRefusal("the algorithm is spelled differently",
                () -> IdToken.parse(serialize("NONE", validClaims())));
        expectRefusal("the token carries two segments only",
                () -> IdToken.parse(segment("{\"alg\":\"RS256\"}") + "." + segment(validClaims().toString())));
    }

    @Test
    public void shouldRefuseATokenWithNoAlgorithmInItsHeader() {
        expectRefusal("the header states no algorithm", () -> IdToken.parse(
                segment("{\"typ\":\"JWT\"}") + "." + segment(validClaims().toString()) + ".sig"));
    }

    // A refresh asks for the grant it holds, and the token it returns is checked.

    @Test
    public void shouldAcceptARefreshedTokenThatCarriesNoNonce() {
        // A refreshed token answers no authorization request, so no nonce binds it to one. OpenID
        // Connect Core 12.2 states that such a token carries none. Every other claim is still checked.
        JSONObject claims = validClaims();
        claims.remove("nonce");

        IdTokenValidator.validateRefreshed(token(claims), CLIENT,
                "http://keycloak:8180/realms/jahialab", NOW);
    }

    @Test
    public void shouldRefuseARefreshedTokenIssuedForAnotherClient() {
        JSONObject claims = validClaims();
        claims.put("aud", "another-client");

        expectRefusal("issued for another client", () -> IdTokenValidator.validateRefreshed(
                token(claims), CLIENT, "http://keycloak:8180/realms/jahialab", NOW));
    }

    @Test
    public void shouldRefuseARefreshedTokenThatExpired() {
        JSONObject claims = validClaims();
        claims.put("exp", NOW - 3600);

        expectRefusal("expired", () -> IdTokenValidator.validateRefreshed(
                token(claims), CLIENT, "http://keycloak:8180/realms/jahialab", NOW));
    }

    @Test
    public void shouldRefuseARefreshedTokenFromAnotherIssuer() {
        expectRefusal("another identity provider", () -> IdTokenValidator.validateRefreshed(
                token(validClaims()), CLIENT, "http://elsewhere.example/realms/other", NOW));
    }
}
