package org.lambadaframework.runtime.models.error;



public class RuntimeErrorResponse extends ErrorResponse {

    public RuntimeErrorResponse(String msg) {
        super(500, msg);
    }
}