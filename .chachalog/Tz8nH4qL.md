---
# Allowed version bumps: patch, minor, major
jahia-oauth: major
---

Changed OAuth and OpenID Connect sign-ins so the module owns the values that tie a sign-in to its callback. It checks those values when the provider calls back.

An OpenID Connect sign-in requires a token endpoint served over TLS. It checks the identity token it receives against the client, the expiry, the provider and the sign-in it answers.

A connector no longer declares whether its provider issues an identity token, or where its token endpoint is. Both come from the provider API itself, so every connector is covered.

Jahia's login URL now starts a sign-in through the OAuth connector enabled on the site. A site with no enabled connector keeps the standard login page.

A connector configured with an empty scope field is sent the scope an identity token depends on, so nobody has to configure it by hand.
