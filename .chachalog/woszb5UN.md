---
jahia-oauth: patch
---

The OAuth connector now requires an https endpoint, and refuses a JWS OpenID token that declares no signature algorithm. A deployment that still reaches its identity provider over cleartext sets requireSecureEndpoints to false while it migrates. A comma-separated endpoint list now accepts a space after each comma.
