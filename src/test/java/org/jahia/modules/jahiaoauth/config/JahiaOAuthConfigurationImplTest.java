package org.jahia.modules.jahiaoauth.config;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class JahiaOAuthConfigurationImplTest {

    @Test
    public void surroundingSpaceIsRemoved() {
        assertEquals(Arrays.asList("https://a/userinfo", "https://b/userinfo"),
                JahiaOAuthConfigurationImpl.readConfiguration("https://a/userinfo, https://b/userinfo"));
        assertEquals(Arrays.asList("https://a/userinfo", "https://b/userinfo"),
                JahiaOAuthConfigurationImpl.readConfiguration("  https://a/userinfo ,https://b/userinfo  "));
    }

    @Test
    public void aSingleValueIsKept() {
        assertEquals(Arrays.asList("https://a/userinfo"),
                JahiaOAuthConfigurationImpl.readConfiguration("https://a/userinfo"));
    }
}
