package org.lambadaframework.runtime;


import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.log4j.Logger;
import org.glassfish.jersey.server.model.Invocable;
import org.lambadaframework.jaxrs.model.ResourceMethod;
import org.lambadaframework.runtime.models.Request;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResourceMethodInvoker {


    static final Logger logger = Logger.getLogger(ResourceMethodInvoker.class);

    private ResourceMethodInvoker() {
    }

    private static Object toObject(String value, Class<?> clazz) {
        if (clazz == Integer.class || Integer.TYPE == clazz) {
            return Integer.parseInt(value);
        }
        if (clazz == Long.class || Long.TYPE == clazz) {
            return Long.parseLong(value);
        }
        if (clazz == Float.class || Float.TYPE == clazz) {
            return Float.parseFloat(value);
        }
        if (clazz == Boolean.class || Boolean.TYPE == clazz) {
            return Boolean.parseBoolean(value);
        }
        if (clazz == Double.class || Double.TYPE == clazz) {
            return Double.parseDouble(value);
        }
        if (clazz == Byte.class || Byte.TYPE == clazz) {
            return Byte.parseByte(value);
        }
        if (clazz == Short.class || Short.TYPE == clazz) {
            return Short.parseShort(value);
        }
        return value;
    }

    public static Object invoke(ResourceMethod resourceMethod,
                                Request request,
                                Context lambdaContext)
            throws
            InvocationTargetException,
            IllegalAccessException,
            InstantiationException {

        logger.debug("Request object is: " + request);


        Invocable invocable = resourceMethod.getInvocable();

        Method method = invocable.getHandlingMethod();
        logger.debug("Method is " + method.toString());
        
        Class<?> clazz = invocable.getHandler().getHandlerClass();

        Object instance = clazz.newInstance();

        List<Object> varargs = new ArrayList<>();


        /**
         * Get consumes annotation from handler method
         */
        Consumes consumesAnnotation = method.getAnnotation(Consumes.class);

        for (Parameter parameter : method.getParameters()) {

            Class<?> parameterClass = parameter.getType();

            /**
             * Path parameter
             */
            if (parameter.isAnnotationPresent(PathParam.class)) {
                PathParam annotation = parameter.getAnnotation(PathParam.class);
                varargs.add(toObject(
                        request.getPathParameters().get(annotation.value()), parameterClass
                        )
                );

            }


            /**
             * Query parameter
             */
            else if (parameter.isAnnotationPresent(QueryParam.class)) {
                QueryParam annotation = parameter.getAnnotation(QueryParam.class);
                varargs.add(toObject(
                        request.getQueryParams().get(annotation.value()), parameterClass
                        )
                );
            }

            /**
             * Query parameter
             */
            else if (parameter.isAnnotationPresent(HeaderParam.class)) {
                HeaderParam annotation = parameter.getAnnotation(HeaderParam.class);
                varargs.add(toObject(
                        request.getRequestHeaders().get(annotation.value()), parameterClass
                        )
                );
            }

            /**
             * text/plain is treated the same as application/json, in order to avoid pre-flight CORS OPTION request
             * 
             */
            else if (consumesAnnotation != null && consumesSpecificType(consumesAnnotation, 
            			Arrays.asList(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN))) {
                if (parameterClass == String.class) {
                    //Pass raw request body
                    varargs.add(request.getRequestBody());
                } else {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        Object deserializedParameter = mapper.readValue(request.getRequestBody(), parameterClass);
                        varargs.add(deserializedParameter);
                    } catch (IOException ioException) {
                        logger.error("Could not serialized " + request.getRequestBody() + " to " + parameterClass + ":", ioException);
                        varargs.add(null);
                    }
                }
            }


            /**
             * Lambda Context can be automatically injected
             */
            else if (parameter.getType() == Context.class) {
                varargs.add(lambdaContext);
            }
                        
            /**
             * last resort
             */
            else {
            	throw new IllegalArgumentException("Can't handle parameter type [" + parameter.getType() + "]");
            }
        }
        
        logger.debug("Varargs = " + varargs);
        
        return method.invoke(instance, varargs.toArray());
    }

    private static boolean consumesSpecificType(Consumes annotation, List<String> types) {

        String[] consumingTypes = annotation.value();
        for (String consumingType : consumingTypes) {
            if (types.contains(consumingType)) {
                return true;
            }
        }

        return false;
    }
}
