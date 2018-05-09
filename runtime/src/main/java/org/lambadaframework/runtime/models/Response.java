package org.lambadaframework.runtime.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Response implements Serializable {

    private static final Logger logger = Logger.getLogger(Response.class);

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


    public static Response buildFromJAXRSResponse(Object response) throws RuntimeException{

        Response outputResponse = new Response();

        if (response instanceof javax.ws.rs.core.Response) {

            javax.ws.rs.core.Response JAXResponse = ((javax.ws.rs.core.Response) response);

            int status = JAXResponse.getStatus();
            //AWS needs to receive a Exception to return different response code then 200. It will parse the error, the lambda error RegEx will get a match on the status code.
            //Throws exception for not 2xx
            if (status < 200 || status > 299) {
                throw new RuntimeException(Integer.toString(status).concat(JAXResponse.getEntity().toString()));
            }

            outputResponse.entity = JAXResponse.getEntity();
            outputResponse.code = status;
            outputResponse.headers = new LinkedHashMap<>();

            for (Map.Entry<String, List<Object>> entry : JAXResponse.getHeaders().entrySet()) {
                outputResponse.headers.put(entry.getKey(), (String) entry.getValue().get(0));
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
     * Returns status code as errorMessage
     * Why errorMessage? Because API Gateway only detects status code within errorMessage
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
