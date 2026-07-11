---
jahia-oauth: patch
---

Issue an unpredictable, single-use OAuth `state` bound to the server-side session and verify it on the callback (RFC 6749 §10.12), instead of deriving it from the session id. The mapper cache remains keyed by the session id, so the SSO login flow is unchanged.
