package org.lambadaframework.runtime.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseProxy implements Serializable {

    private static final Logger logger = Logger.getLogger(ResponseProxy.class);

    /**
     * Response headers
     */
    protected Map<String, String> headers = new LinkedHashMap<>();

    /**
     * Status code
     */
    protected int code = 200;

    /**
     * Response entity
     */
    protected Object entity;

    public ResponseProxy() {
    }

    public ResponseProxy(int code, Object entity) {
        this.code = code;
        this.entity = entity;
        setCors();
    }


    public static ResponseProxy buildFromJAXRSResponse(Object response) throws RuntimeException, IOException {

        ResponseProxy outputResponse = new ResponseProxy();

        if (response instanceof javax.ws.rs.core.Response) {

            javax.ws.rs.core.Response JAXResponse = ((javax.ws.rs.core.Response) response);

            int status = JAXResponse.getStatus();

            outputResponse.entity = JAXResponse.getEntity();
            outputResponse.code = status;
            outputResponse.setCors();

            for (Map.Entry<String, List<Object>> entry : JAXResponse.getHeaders().entrySet()) {
                outputResponse.headers.put(entry.getKey(), (String) entry.getValue().get(0));
            }


        } else {
            outputResponse.entity = response;
        }

        return outputResponse;
    }

    public void setCors() {
        headers.put("Access-Control-Allow-Origin", "*");
    }

    @JsonProperty("body")
    public Object getEntity() {
        return entity;
    }

    @JsonProperty("headers")
    public Map<String, String> getHeaders() {
        return headers;
    }

    @JsonProperty("statusCode")
    public int getStatusCode() {
        return this.code;
    }

    public void write(Writer writer) {
        try {
            new ObjectMapper().writeValue(writer, this);
            writer.close();
        } catch (IOException e) {
            logger.error(e.getStackTrace());
        }
    }

}
