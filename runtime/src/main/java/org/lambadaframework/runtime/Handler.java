package org.lambadaframework.runtime;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.lambadaframework.jaxrs.model.ResourceMethod;
import org.lambadaframework.runtime.errorhandling.ErrorHandler;
import org.lambadaframework.runtime.models.RequestInterface;
import org.lambadaframework.runtime.models.ResponseProxy;
import org.lambadaframework.runtime.models.error.ErrorResponse;
import org.lambadaframework.runtime.router.Router;

import java.io.*;


public class Handler implements RequestStreamHandler {

    static final Logger logger = Logger.getLogger(Handler.class);
    private Router router;


    public Handler setRouter(Router router) {
        this.router = router;
        return this;
    }

    public Router getRouter() {
        if (router != null) {
            return router;
        }
        return Router.getRouter();
    }

    /**
     * If request object's "method" field is null or has an invalid
     * HTTP method string it is impossible to process the request
     * thus we throw an exception and 500 HTTP error.
     *
     * @param requestObject Request object
     * @throws Exception
     */
    private void checkHttpMethod(RequestInterface requestObject)
            throws Exception {
        if (requestObject.getMethod() == null) {
            throw new Exception("Method was null");
        }
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {

        logger.debug("Loading Java Lambda handler of ProxyWithStream");

        Object invoke;
        ResponseProxy responseProxy = null;
        try {
            RequestInterface req = getParsedRequest(inputStream);

            if (req == null) {
                logger.debug("Request object is null can not proceed with request.");
            } else {
                checkHttpMethod(req);
                logger.debug("Request check is ok.");
                ResourceMethod matchedResourceMethod = getRouter().route(req);
                invoke = ResourceMethodInvoker.invoke(matchedResourceMethod, req, context);

                responseProxy = ResponseProxy.buildFromJAXRSResponse(invoke);
            }
        } catch (Exception e) {
            logger.debug("Exception: " + e.getMessage() + "\n" + e.getStackTrace());
            responseProxy = ErrorHandler.getErrorResponse(e);
        } catch (Error e) {
            logger.debug("Error: " + e.getMessage());
        }

        try {
            responseProxy.write(new OutputStreamWriter(outputStream, "UTF-8"));
            outputStream.close();
        } catch (Exception e) {
            logger.error("Failed to write response: " + e.getStackTrace());
        }
    }


    /**
     * @return a ObjectMapper configured to ignore if incoming json do have properties unknown to requestProxy.
     */
    private ObjectMapper getConfiguredMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    protected RequestInterface getParsedRequest(InputStream inputStream) throws Exception {
        logger.debug("Starting to parse request stream");

        JsonParser jp = new JsonFactory().createParser(inputStream);
        RequestInterface req = null;

        ObjectMapper configuredMapper = getConfiguredMapper();
        //Can't handle if stream starts with array.
        while (jp.nextToken() == JsonToken.START_OBJECT) {
            req = configuredMapper.readValue(jp, RequestProxy.class);
            logger.debug("Parsed input stream to Request object");
        }

        jp.close();
        inputStream.close();
        return req;

    }
}
