/*
 * Copyright (C) 2002-2025 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.jahiaoauth.service;

/**
 * The values bound to a single authorization-flow {@code state} token at initiation, recovered on the
 * callback from the cluster-wide flow-state cache. Lets the callback resolve the initiating session id
 * (the mapper-cache key) and the OIDC nonce without relying on the HTTP session being present.
 */
public final class OAuthFlowState {
    private final String sessionId;
    private final String nonce;

    public OAuthFlowState(String sessionId, String nonce) {
        this.sessionId = sessionId;
        this.nonce = nonce;
    }

    /** The id of the session that initiated the flow; used as the mapper-cache key. */
    public String getSessionId() {
        return sessionId;
    }

    /** The OIDC nonce sent at initiation, or {@code null} for a non-OIDC flow. */
    public String getNonce() {
        return nonce;
    }
}
