---
jahia-oauth: patch
---

Harden the OAuth/OIDC login flow: issue an unpredictable, single-use `state` bound to the server-side session and verify it on the callback (RFC 6749 §10.12), instead of deriving it from the session id; and, when an OpenID Connect `nonce` is used, verify that the returned id_token carries the same value (OpenID Connect Core §3.1.3.7). The mapper cache remains keyed by the session id, so the SSO login flow is unchanged.
