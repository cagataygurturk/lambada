package org.lambadaframework.runtime;


import com.amazonaws.services.lambda.runtime.Context;
import org.apache.log4j.Logger;
import org.glassfish.jersey.server.model.Invocable;
import org.lambadaframework.jaxrs.model.ResourceMethod;
import org.lambadaframework.runtime.models.Request;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class ResourceMethodInvoker {


    static final Logger logger = Logger.getLogger(ResourceMethodInvoker.class);

    private ResourceMethodInvoker() {
    }

    private static Object toObject(String value, Class clazz) {
        if (Integer.class == clazz || Integer.TYPE == clazz) return Integer.parseInt(value);
        if (Long.class == clazz || Long.TYPE == clazz) return Long.parseLong(value);
        if (Float.class == clazz || Float.TYPE == clazz) return Float.parseFloat(value);
        if (Boolean.class == clazz || Boolean.TYPE == clazz) return Boolean.parseBoolean(value);
        if (Double.class == clazz || Double.TYPE == clazz) return Double.parseDouble(value);
        if (Byte.class == clazz || Byte.TYPE == clazz) return Byte.parseByte(value);
        if (Short.class == clazz || Short.TYPE == clazz) return Short.parseShort(value);
        return value;
    }

    public static Object invoke(ResourceMethod resourceMethod,
                                Request request,
                                Context lambdaContext)
            throws
            Exception {

        logger.debug("Request object is: " + request.toString());


        Invocable invocable = resourceMethod.getInvocable();

        Method method = invocable.getHandlingMethod();
        Class clazz = invocable.getHandler().getHandlerClass();

        Object instance = clazz.newInstance();

        List<Object> varargs = new ArrayList<>();

        Object body = request.getRequestBody();

        HashMap map = null;

        if (body != null) {
            logger.debug("Body type is: " + body.getClass().getName());
            if (body instanceof HashMap) {
                map = (HashMap)body;
            }
        } else {
            logger.debug("Body is null");
        }


        /**
         * Get consumes annotation from handler method
         */
        Consumes consumesAnnotation = method.getAnnotation(Consumes.class);

        for (Parameter parameter : method.getParameters()) {

            Class<?> parameterClass = parameter.getType();

            Object paramV = null;

            if (parameter.isAnnotationPresent(DefaultValue.class)) {
                DefaultValue annotation = parameter.getAnnotation(DefaultValue.class);
                paramV = annotation.value();
            }

            /**
             * Path parameter
             */
            if (parameter.isAnnotationPresent(PathParam.class)) {
                PathParam annotation = parameter.getAnnotation(PathParam.class);
                paramV = (toObject((String) request.getPathParameters().get(annotation.value()), parameterClass));
            } else if (parameter.isAnnotationPresent(QueryParam.class)) {
                QueryParam annotation = parameter.getAnnotation(QueryParam.class);
                paramV = (toObject((String) request.getQueryParams().get(annotation.value()), parameterClass));
            } else if (parameter.isAnnotationPresent(HeaderParam.class)) {
                HeaderParam annotation = parameter.getAnnotation(HeaderParam.class);
                paramV = (toObject((String) request.getRequestHeaders().get(annotation.value()), parameterClass));
            } else if (parameter.isAnnotationPresent(FormParam.class)) {
                logger.info("Got Form Parameter");
                FormParam annotation = parameter.getAnnotation(FormParam.class);

                if (body instanceof HashMap) {
                    paramV = map.get(annotation.value());
                }
            } else if (parameter.getType() == Context.class) { /* Lambda Context can be automatically injected */
                paramV = (lambdaContext);
            } else {
                logger.info("Got fallback for Parameter: " + parameter.getName()); // TODO not working.
                if (body instanceof HashMap) {
                    paramV = map.get(parameter.getName());
                } else if (body != null && parameter.getClass() == body.getClass()) {
                    logger.info("Body is param class: " + parameterClass.getName());
                    paramV = body; // TODO fix
                } else {
                    logger.info("Body is param class: " + parameterClass.getName());
                    throw new Exception("Parameter mismatch");
                }

                if (consumesAnnotation != null && consumesSpecificType(consumesAnnotation, MediaType.APPLICATION_JSON)
                    && parameter.getType() == String.class) {
                //Pass raw request body
                varargs.add(request.getRequestBody());
            }


            /**
             * Lambda Context can be automatically injected
             */
            if (parameter.getType() == Context.class) {
                varargs.add(lambdaContext);
            }
            varargs.add(paramV);
        }

        return method.invoke(instance, varargs.toArray());
    }

    private static boolean consumesSpecificType(Consumes annotation, String type) {

        String[] consumingTypes = annotation.value();
        for (String consumingType : consumingTypes) {
            if (type.equals(consumingType)) {
                return true;
            }
        }

        return false;
    }
}
