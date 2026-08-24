package org.jahia.modules.jahiaoauth.impl;

import org.jahia.modules.jahiaoauth.service.JahiaOAuthException;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * The values that decide which account is signed in come from the protected resource, and not from
 * the identity token. These cases state what ties the two together.
 */
public class SubjectBindingTest {

    private static final String CONNECTOR = "OidcConnector";

    private static void expectRefusal(String because, String verifiedSubject, String responseSubject) {
        try {
            JahiaOAuthServiceImpl.requireSameSubject(CONNECTOR, verifiedSubject, responseSubject);
            fail("Must be refused: " + because);
        } catch (JahiaOAuthException e) {
            // expected
        }
    }

    @Test
    public void shouldAcceptAResponseDescribingTheSubjectOfTheToken() throws JahiaOAuthException {
        JahiaOAuthServiceImpl.requireSameSubject(CONNECTOR, "8f3c1e4a", "8f3c1e4a");
    }

    @Test
    public void shouldRefuseAResponseDescribingAnotherSubject() {
        expectRefusal("the response describes another person", "8f3c1e4a", "0000-victim");
    }

    @Test
    public void shouldRefuseAResponseThatStatesNoSubject() {
        // Accepting this would let whoever writes the response drop the claim to skip the comparison.
        expectRefusal("the response states no subject", "8f3c1e4a", null);
    }

    @Test
    public void shouldAcceptAnyResponseWhenTheFlowReceivedNoIdentityToken() throws JahiaOAuthException {
        // A connector whose provider issues no identity token has nothing to tie the response to. A
        // connector whose scribejava API reads the OpenID extractor has to produce one, so the case this
        // covers is a provider that issues none.
        JahiaOAuthServiceImpl.requireSameSubject(CONNECTOR, null, null);
        JahiaOAuthServiceImpl.requireSameSubject(CONNECTOR, null, "whatever");
    }
}
