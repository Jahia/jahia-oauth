---
# Allowed version bumps: patch, minor, major
jahia-oauth: patch
---

Fixed OAuth sign-in on a clustered installation, where the node handling the provider callback could not retrieve the session started on another node.
