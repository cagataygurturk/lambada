package org.lambadaframework.runtime;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.lambadaframework.jaxrs.model.ResourceMethod;
import org.lambadaframework.runtime.errorhandling.ErrorHandler;
import org.lambadaframework.runtime.models.Request;
import org.lambadaframework.runtime.models.Response;
import org.lambadaframework.runtime.router.Router;
import org.apache.log4j.Logger;


public class Handler
        implements RequestHandler<Request, Response> {

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
    private void checkHttpMethod(Request requestObject)
            throws Exception {
        if (requestObject.getMethod() == null) {
            throw new Exception("Method was null");
        }
    }


    @Override
    public Response handleRequest(Request request, Context context) {
        Object invoke;
        try {
            logger.debug("Request started with " + request + " and " + context);

            checkHttpMethod(request);
            logger.debug("Request check is ok.");

            logger.debug("Matching request to a resource handler.");
            ResourceMethod matchedResourceMethod = getRouter().route(request);
            invoke = ResourceMethodInvoker.invoke(matchedResourceMethod, request, context);

        } catch (Exception ex) {
            return ErrorHandler.getErrorResponse(ex);
        }
        logger.debug("Returning result.");
        return Response.buildFromJAXRSResponse(invoke);
    }
}