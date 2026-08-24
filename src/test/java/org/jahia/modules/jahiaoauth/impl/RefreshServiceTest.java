package org.jahia.modules.jahiaoauth.impl;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * A refresh asks for the grant it holds.
 * <p>
 * scribejava sends the default scope of the service when a caller names none, so a service built for
 * a refresh has to carry none. RFC 6749 section 6 states that a refresh may not ask for a scope the
 * grant never carried, and the scope this framework derives adds {@code openid}.
 */
public class RefreshServiceTest {

    @Test
    public void shouldBuildTheSignInServiceWithTheDerivedScope() {
        assertNotNull(JahiaOAuthServiceImpl.scopeOfServiceFor(true, "openid email"));
    }

    @Test
    public void shouldBuildTheRefreshServiceWithNoScope() {
        assertNull(JahiaOAuthServiceImpl.scopeOfServiceFor(false, "openid email"));
    }
}
