---
jahia-oauth: patch
---

Harden the OAuth/OIDC login flow: issue an unpredictable, single-use `state` and bind it (with the OIDC `nonce`) to the initiating session id in a cluster-wide store, verifying both on the callback (RFC 6749 §10.12 + OpenID Connect Core §3.1.3.7) instead of deriving the state from the session id. The callback recovers the values without relying on the HTTP session, and the mapper cache remains keyed by the session id, so the SSO login flow is unchanged.
