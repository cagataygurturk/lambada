package org.lambadaframework.runtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.lambadaframework.runtime.models.RequestInterface;

import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class RequestProxy implements Serializable, RequestInterface {

    /**
     * Package name
     */
    protected String packageName;

    /**
     * Request method
     */
    protected RequestMethod method;

    /**
     * Request path
     */
    protected String pathTemplate;

    /**
     * Request body
     */
    protected String requestBody;

    /**
     * Query parameters
     */
    protected Map<String, String> pathParameters = new HashMap<>();

    /**
     * Query parameters
     */
    protected Map<String, String> queryParams = new HashMap<>();

    /**
     * Request headers
     */
    protected Map<String, String> requestHeaders = new HashMap<>();

    /**
     * Consumed media type
     */
    protected MediaType consumedMediaType = MediaType.APPLICATION_JSON_TYPE;

    /**
     * Produced media Type
     */
    protected MediaType producedMediaType = MediaType.APPLICATION_JSON_TYPE;


    private MediaType getMediaTypeFromString(String mimeType) {
        String[] m = mimeType.split("/");
        return new MediaType(m[0], m[1]);
    }


    @JsonProperty("httpMethod")
    public RequestProxy setMethod(RequestMethod method) {
        this.method = method;
        return this;
    }

    @Override
    public RequestMethod getMethod() {
        return method;
    }

    @Override
    public String getPackage() {
        this.packageName =  System.getenv("PackageNameRest");
        return this.packageName;
    }

    @Override
    public String getPathTemplate() {
        return pathTemplate;
    }
    /*
    There is no path template when using proxy. This is the actual request path.
     */
    @JsonProperty("path")
    public RequestProxy setPathtemplate(String pathTemplate) {
        this.pathTemplate = pathTemplate;
        return this;
    }

    @Override
    public String getRequestBody() {
        return requestBody;
    }

    @JsonProperty("body")
    public RequestProxy setRequestbody(String requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    @Override
    public MediaType getConsumedMediaType() {
        return consumedMediaType;
    }

    /*@JsonProperty("consumes")
    public RequestProxy setConsumes(String consumedMediaType) {
        this.consumedMediaType = getMediaTypeFromString(consumedMediaType);
        return this;
    }*/

    @Override
    public MediaType getProducedMediaType() {
        return producedMediaType;
    }

    /*@JsonProperty("produces")
    public RequestProxy setProduces(String producedMediaType) {
        this.producedMediaType = getMediaTypeFromString(producedMediaType);
        return this;
    }*/

    @Override
    public Map<String, String> getPathParameters() {
        return pathParameters;
    }

    @JsonProperty("pathParameters")
    public RequestProxy setPath(Map<String, String> pathParameters) {
        this.pathParameters = pathParameters;
        return this;
    }

    @Override
    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    @JsonProperty("queryStringParameters")
    public RequestProxy setQuerystring(Map<String, String> queryParams) {
        this.queryParams = queryParams;
        if (this.queryParams == null) {
            this.queryParams = new HashMap<>();
        }
        return this;
    }

    @Override
    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    @JsonProperty("headers")
    public RequestProxy setHeader(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
        return this;
    }

    @Override
    public String toString() {
        return "Request{" +
                "packageName='" + packageName + '\'' +
                ", method=" + method +
                ", pathTemplate='" + pathTemplate + '\'' +
                ", requestBody=" + requestBody +
                ", pathParameters=" + pathParameters +
                ", queryParams=" + queryParams +
                ", requestHeaders=" + requestHeaders +
                ", consumedMediaType=" + consumedMediaType +
                ", producedMediaType=" + producedMediaType +
                '}';
    }
}
