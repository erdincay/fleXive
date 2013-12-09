package com.flexive.shared.exceptions;

/**
 * Exception thrown when the REST API token is expired or no longer valid.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 * @since 3.2.0
 */
public class FxRestApiTokenExpiredException extends FxApplicationException {
    private final String token;

    public FxRestApiTokenExpiredException(String token) {
        super("ex.account.rest.token.expired");
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
