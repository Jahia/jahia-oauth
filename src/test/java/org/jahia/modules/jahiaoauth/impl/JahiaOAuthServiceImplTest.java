package org.jahia.modules.jahiaoauth.impl;

import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.model.OAuth2AccessToken;
import org.jahia.modules.jahiaoauth.config.JahiaOAuthConfiguration;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthConstants;
import org.junit.Test;

import java.util.Collections;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class JahiaOAuthServiceImplTest {

    private static final String SIGNATURE = "c2lnbmF0dXJl";
    private static final String PAYLOAD = encode("{\"sub\":\"root\"}");

    @Test
    public void httpsEndpointIsAccepted() {
        assertTrue(accepted(() -> JahiaOAuthServiceImpl.requireSecureEndpoint("https://app.franceconnect.gouv.fr/api/v1/userinfo")));
        assertTrue(accepted(() -> JahiaOAuthServiceImpl.requireSecureEndpoint("HTTPS://app.franceconnect.gouv.fr/api/v1/userinfo")));
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
    public void tokenWithNoAlgorithmIsRejected() {
        assertRejected(() -> JahiaOAuthServiceImpl.refuseUnsignedJws(token("{\"kid\":\"k1\"}", SIGNATURE)));
        assertRejected(() -> JahiaOAuthServiceImpl.refuseUnsignedJws(token("{\"alg\":\"\"}", SIGNATURE)));
    }

    @Test
    public void tokenThatIsNotAJwsIsLeftAlone() {
        assertTrue(accepted(() -> JahiaOAuthServiceImpl.refuseUnsignedJws("opaque-token")));
        assertTrue(accepted(() -> JahiaOAuthServiceImpl.refuseUnsignedJws(encode("{\"alg\":\"none\"}") + "." + PAYLOAD)));
        assertTrue(accepted(() -> JahiaOAuthServiceImpl.refuseUnsignedJws(
                encode("{\"alg\":\"none\"}") + "." + PAYLOAD + "." + SIGNATURE + "." + SIGNATURE + "." + SIGNATURE)));
    }

    @Test
    public void signedTokenIsAccepted() {
        assertTrue(accepted(() -> JahiaOAuthServiceImpl.refuseUnsignedJws(token("{\"alg\":\"RS256\"}", SIGNATURE))));
        assertTrue(accepted(() -> JahiaOAuthServiceImpl.refuseUnsignedJws(token("{\"alg\":\"ES256\"}", SIGNATURE))));
    }

    @Test
    public void absentTokenIsAccepted() {
        assertTrue(accepted(() -> JahiaOAuthServiceImpl.refuseUnsignedJws(null)));
        assertTrue(accepted(() -> JahiaOAuthServiceImpl.refuseUnsignedJws("")));
    }

    @Test
    public void unsignedAlgorithmIsRejected() {
        assertRejected(() -> JahiaOAuthServiceImpl.refuseUnsignedJws(token("{\"alg\":\"none\"}", SIGNATURE)));
        assertRejected(() -> JahiaOAuthServiceImpl.refuseUnsignedJws(token("{\"alg\":\"NONE\"}", SIGNATURE)));
    }

    @Test
    public void emptySignatureSegmentIsRejected() {
        assertRejected(() -> JahiaOAuthServiceImpl.refuseUnsignedJws(token("{\"alg\":\"none\"}", "")));
        assertRejected(() -> JahiaOAuthServiceImpl.refuseUnsignedJws(token("{\"alg\":\"RS256\"}", "")));
    }

    @Test
    public void jwsWithAnUnreadableHeaderIsRejected() {
        assertRejected(() -> JahiaOAuthServiceImpl.refuseUnsignedJws("!!!." + PAYLOAD + "." + SIGNATURE));
        assertRejected(() -> JahiaOAuthServiceImpl.refuseUnsignedJws(encode("not json") + "." + PAYLOAD + "." + SIGNATURE));
    }

    @Test
    public void absentConfigurationRequiresHttps() {
        assertTrue(JahiaOAuthServiceImpl.requireSecureEndpoints(null));
    }

    @Test
    public void configurationDecidesWhenPresent() {
        assertTrue(JahiaOAuthServiceImpl.requireSecureEndpoints(configuration(true)));
        assertFalse(JahiaOAuthServiceImpl.requireSecureEndpoints(configuration(false)));
    }

    private static JahiaOAuthConfiguration configuration(boolean requireSecureEndpoints) {
        return new JahiaOAuthConfiguration() {
            @Override
            public boolean isRequireSecureEndpoints() {
                return requireSecureEndpoints;
            }

            @Override
            public java.util.List<String> getFacebookUserInfoEndpoints() {
                return Collections.emptyList();
            }

            @Override
            public java.util.List<String> getGitHubUserInfoEndpoints() {
                return Collections.emptyList();
            }

            @Override
            public java.util.List<String> getLinkedInUserInfoEndpoints() {
                return Collections.emptyList();
            }

            @Override
            public java.util.List<String> getGoogleUserInfoEndpoints() {
                return Collections.emptyList();
            }
        };
    }

    @Test
    public void unsignedOpenIdTokenIsRefusedBeforeStorage() {
        OpenIdOAuth2AccessToken accessToken = openIdAccessToken(token("{\"alg\":\"none\"}", ""));
        assertRejected(() -> new JahiaOAuthServiceImpl().extractAccessTokenData(accessToken));
    }

    @Test
    public void signedOpenIdTokenIsStored() {
        String openIdToken = token("{\"alg\":\"RS256\"}", SIGNATURE);
        Map<String, Object> tokenData = new JahiaOAuthServiceImpl().extractAccessTokenData(openIdAccessToken(openIdToken));
        assertEquals(openIdToken, tokenData.get(JahiaOAuthConstants.OPEN_ID_TOKEN));
    }

    @Test
    public void aPlainAccessTokenCarriesNoOpenIdToken() {
        Map<String, Object> tokenData = new JahiaOAuthServiceImpl().extractAccessTokenData(new OAuth2AccessToken("bearer-only"));
        assertFalse(tokenData.containsKey(JahiaOAuthConstants.OPEN_ID_TOKEN));
    }

    private static OpenIdOAuth2AccessToken openIdAccessToken(String openIdToken) {
        OpenIdOAuth2AccessToken accessToken = new OpenIdOAuth2AccessToken("bearer", openIdToken, null);
        assertEquals(openIdToken, accessToken.getOpenIdToken());
        return accessToken;
    }

    private static void assertRejected(Runnable call) {
        assertThrows(IllegalArgumentException.class, call::run);
    }

    private static boolean accepted(Runnable call) {
        try {
            call.run();
            return true;
        } catch (IllegalArgumentException refused) {
            return false;
        }
    }

    private static String token(String header, String signature) {
        return encode(header) + "." + PAYLOAD + "." + signature;
    }

    private static String encode(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
