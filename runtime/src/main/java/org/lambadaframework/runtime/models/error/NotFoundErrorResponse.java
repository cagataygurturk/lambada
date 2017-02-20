package org.lambadaframework.runtime.models.error;



public class NotFoundErrorResponse extends ErrorResponse {

    protected String errorMessage;

    public NotFoundErrorResponse() {
        super(404, "Page not found");
    }
}
