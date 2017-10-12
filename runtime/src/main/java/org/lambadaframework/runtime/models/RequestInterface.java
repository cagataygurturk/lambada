package org.lambadaframework.runtime.models;

import javax.ws.rs.core.MediaType;
import java.util.Map;

public interface RequestInterface {

    enum RequestMethod {
        GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE;
    }

    RequestMethod getMethod();

    String getPathTemplate();

    String getPackage();

    MediaType getConsumedMediaType();

    MediaType getProducedMediaType();

    Map<String, String> getPathParameters();

    Map<String, String> getQueryParams();

    Map<String, String> getRequestHeaders();

    String getRequestBody();

}
