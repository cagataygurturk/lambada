package org.lambadaframework.runtime.models.error;


public class BadRequestResponse extends ErrorResponse {


    public BadRequestResponse() {
        super(400, "Bad request");
    }
}
