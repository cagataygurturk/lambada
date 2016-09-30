package org.lambadaframework.runtime.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Response implements Serializable {

    /**
     * Response headers
     */
    protected Map<String, String> headers;

    /**
     * Status code
     */
    protected int code = 200;

    /**
     * Response entity
     */
    protected Object entity;

    public static Response buildFromJAXRSResponse(Object response) {

        Response outputResponse = new Response();

        if (response instanceof javax.ws.rs.core.Response) {
            javax.ws.rs.core.Response JAXResponse = ((javax.ws.rs.core.Response) response);
            outputResponse.entity = JAXResponse.getEntity();
            outputResponse.code = JAXResponse.getStatus();
            outputResponse.headers = new LinkedHashMap<>();

            for (Map.Entry<String, List<Object>> entry : JAXResponse.getHeaders().entrySet()) {
                outputResponse.headers.put(entry.getKey(), entry.getValue().get(0).toString());
            }

        } else {
            outputResponse.entity = response;
        }

        return outputResponse;
    }

    @JsonProperty("entity")
    public Object getEntity() {
        return entity;
    }

    /**
     * Returns status code as errorMessage Why errorMessage? Because API Gateway
     * only detects status code within errorMessage
     *
     * @return Status code
     */
    @JsonProperty("errorMessage")
    public String getErrorMessage() {
        return String.valueOf(code);
    }

    @JsonProperty("headers")
    public Map<String, String> getHeaders() {
        return headers;
    }
}
