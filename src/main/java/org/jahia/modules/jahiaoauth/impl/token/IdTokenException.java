package org.jahia.modules.jahiaoauth.impl.token;

/**
 * Raised when an identity token cannot be read, or when a claim it carries does not hold.
 * <p>
 * A sign-in that raises this never happens. The token names the account the flow signs in, so a
 * client that accepted a token it could not check would sign in whoever the token named.
 */
public class IdTokenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IdTokenException(String message) {
        super(message);
    }

    public IdTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
