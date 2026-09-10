package org.jahia.modules.jahiaoauth.impl;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.Assert.assertThrows;

public class JahiaOAuthServiceImplTest {

    private static final String SIGNATURE = "c2lnbmF0dXJl";
    private static final String PAYLOAD = encode("{\"sub\":\"root\"}");

    @Test
    public void httpsEndpointIsAccepted() {
        JahiaOAuthServiceImpl.requireSecureEndpoint("https://app.franceconnect.gouv.fr/api/v1/userinfo");
        JahiaOAuthServiceImpl.requireSecureEndpoint("HTTPS://app.franceconnect.gouv.fr/api/v1/userinfo");
    }

    @Test
    public void cleartextEndpointIsRejected() {
        assertRejected(() -> JahiaOAuthServiceImpl.requireSecureEndpoint("http://localhost:18100/oidc/userinfo"));
        assertRejected(() -> JahiaOAuthServiceImpl.requireSecureEndpoint("HTTP://localhost:18100/oidc/userinfo"));
    }

    @Test
    public void endpointWithNoHttpsSchemeIsRejected() {
        assertRejected(() -> JahiaOAuthServiceImpl.requireSecureEndpoint("ftp://example.org/userinfo"));
        assertRejected(() -> JahiaOAuthServiceImpl.requireSecureEndpoint("//example.org/userinfo"));
        assertRejected(() -> JahiaOAuthServiceImpl.requireSecureEndpoint(""));
    }

    @Test
    public void schemeIsReadAtTheStartOnly() {
        assertRejected(() -> JahiaOAuthServiceImpl.requireSecureEndpoint("http://evil.example/?next=https://app.franceconnect.gouv.fr"));
    }

    @Test
    public void signedTokenIsAccepted() {
        JahiaOAuthServiceImpl.requireSignedToken(token("{\"alg\":\"RS256\"}", SIGNATURE));
        JahiaOAuthServiceImpl.requireSignedToken(token("{\"alg\":\"ES256\"}", SIGNATURE));
    }

    @Test
    public void absentTokenIsAccepted() {
        JahiaOAuthServiceImpl.requireSignedToken(null);
        JahiaOAuthServiceImpl.requireSignedToken("");
    }

    @Test
    public void unsignedAlgorithmIsRejected() {
        assertRejected(() -> JahiaOAuthServiceImpl.requireSignedToken(token("{\"alg\":\"none\"}", SIGNATURE)));
        assertRejected(() -> JahiaOAuthServiceImpl.requireSignedToken(token("{\"alg\":\"NONE\"}", SIGNATURE)));
    }

    @Test
    public void emptySignatureSegmentIsRejected() {
        assertRejected(() -> JahiaOAuthServiceImpl.requireSignedToken(token("{\"alg\":\"none\"}", "")));
        assertRejected(() -> JahiaOAuthServiceImpl.requireSignedToken(token("{\"alg\":\"RS256\"}", "")));
    }

    @Test
    public void malformedTokenIsRejected() {
        assertRejected(() -> JahiaOAuthServiceImpl.requireSignedToken(encode("{\"alg\":\"RS256\"}") + "." + PAYLOAD));
        assertRejected(() -> JahiaOAuthServiceImpl.requireSignedToken("not-a-token"));
        assertRejected(() -> JahiaOAuthServiceImpl.requireSignedToken("!!!." + PAYLOAD + "." + SIGNATURE));
    }

    private static void assertRejected(Runnable call) {
        assertThrows(IllegalArgumentException.class, call::run);
    }

    private static String token(String header, String signature) {
        return encode(header) + "." + PAYLOAD + "." + signature;
    }

    private static String encode(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
