package org.lambadaframework.runtime;


import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.log4j.Logger;
import org.glassfish.jersey.server.model.Invocable;
import org.lambadaframework.jaxrs.model.ResourceMethod;
import org.lambadaframework.runtime.models.RequestInterface;

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
                                RequestInterface request,
                                Context lambdaContext)
            throws
            InvocationTargetException,
            IllegalAccessException,
            InstantiationException {

        logger.debug("Request object is: " + request);


        Invocable invocable = resourceMethod.getInvocable();

        Method method = invocable.getHandlingMethod();
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
            if (parameter.isAnnotationPresent(QueryParam.class)) {
                QueryParam annotation = parameter.getAnnotation(QueryParam.class);
                varargs.add(toObject(
                        request.getQueryParams().get(annotation.value()), parameterClass
                        )
                );
            }

            /**
             * Query parameter
             */
            if (parameter.isAnnotationPresent(HeaderParam.class)) {
                HeaderParam annotation = parameter.getAnnotation(HeaderParam.class);
                varargs.add(toObject(
                        request.getRequestHeaders().get(annotation.value()), parameterClass
                        )
                );
            }

            if (consumesAnnotation != null && consumesSpecificType(consumesAnnotation, MediaType.APPLICATION_JSON)) {
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
            if (parameter.getType() == Context.class) {
                varargs.add(lambdaContext);
            }
        }
        //TODO: Catch Invocation target exception and throw the underlying exception.
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
