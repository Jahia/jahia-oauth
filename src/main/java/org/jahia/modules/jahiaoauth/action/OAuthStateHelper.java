/*
 * Copyright (C) 2002-2026 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.jahiaoauth.action;

import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Manages the OAuth 2.0 {@code state} parameter as an anti-CSRF / anti-session-fixation token (RFC 6749 section 10.12).
 * <p>
 * The state is a cryptographically random, single-use value bound to the HTTP session that initiates the flow. The
 * callback must present back the exact value stored for the session, otherwise the flow is rejected. This guarantees
 * that only the browser that started the authorization can complete it, and prevents the callers from choosing the
 * key under which the resolved identity is cached (which used to be the raw session id sent as state).
 */
final class OAuthStateHelper {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int STATE_BYTE_LENGTH = 32; // 256 bits of entropy
    private static final String SESSION_ATTRIBUTE_PREFIX = "org.jahia.modules.jahiaoauth.state.";

    private OAuthStateHelper() {
        // Utility class
    }

    /**
     * Generates a new random state value, stores it on the initiating HTTP session and returns it so it can be sent to
     * the OAuth provider as the {@code state} parameter.
     *
     * @param request       the request initiating the OAuth flow
     * @param connectorName the connector the flow is started for (a distinct state is kept per connector)
     * @return the generated state value
     */
    static String createState(HttpServletRequest request, String connectorName) {
        byte[] bytes = new byte[STATE_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        request.getSession().setAttribute(attributeName(connectorName), state);
        return state;
    }

    /**
     * Verifies that the state returned by the OAuth provider matches the value issued for this session and, when it
     * does, consumes it so it cannot be replayed. The stored value is left untouched on a mismatch so an in-progress
     * legitimate flow is not invalidated by a forged callback.
     *
     * @param request       the callback request
     * @param connectorName the connector the callback is for
     * @param receivedState the state value returned by the OAuth provider
     * @return {@code true} if the state is valid and was consumed, {@code false} otherwise
     */
    static boolean consumeState(HttpServletRequest request, String connectorName, String receivedState) {
        if (StringUtils.isBlank(receivedState)) {
            return false;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        String attribute = attributeName(connectorName);
        Object expectedState = session.getAttribute(attribute);
        if (expectedState instanceof String && constantTimeEquals((String) expectedState, receivedState)) {
            session.removeAttribute(attribute); // single use
            return true;
        }
        return false;
    }

    private static String attributeName(String connectorName) {
        return SESSION_ATTRIBUTE_PREFIX + connectorName;
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
