package org.lambadaframework.runtime.models.error;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.lambadaframework.runtime.models.Response;
import org.lambadaframework.runtime.models.ResponseProxy;

public class ErrorResponse extends ResponseProxy {

    protected String errorMessage;


    public ErrorResponse() {
        super(500, "Internal Server Error");
    }

    public ErrorResponse(int code, Object entity) {
        super(code, entity);
    }
}
