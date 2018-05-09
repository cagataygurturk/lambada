package org.lambadaframework.runtime;


import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.log4j.Logger;
import org.glassfish.jersey.server.model.Invocable;
import org.lambadaframework.jaxrs.model.ResourceMethod;
import org.lambadaframework.runtime.models.Request;
import org.lambadaframework.runtime.exceptions.InvalidParameterException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;

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
            InstantiationException,
            InvalidParameterException,
            IOException {

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
        ObjectMapper objectMapper = new ObjectMapper();

        for (Parameter parameter : method.getParameters()) {

            Class<?> parameterClass = parameter.getType();

            logger.info("Parameter: "+parameter.getName()+" type: "+parameterClass.getName()+".");

            Object paramV = null;

            if (parameter.isAnnotationPresent(DefaultValue.class)) {
                DefaultValue annotation = parameter.getAnnotation(DefaultValue.class);
                paramV = annotation.value();
                logger.info("Found default value for parameter.");
            }

            /**
             * Path parameter
             */
            if (parameter.isAnnotationPresent(PathParam.class)) {
                PathParam annotation = parameter.getAnnotation(PathParam.class);
                paramV = toObject(request.getPathParameters().get(annotation.value()), parameterClass);
                logger.info("Path param found.");
            } else if (parameter.isAnnotationPresent(QueryParam.class)) {
                QueryParam annotation = parameter.getAnnotation(QueryParam.class);
                paramV = toObject(request.getQueryParams().get(annotation.value()), parameterClass);
                logger.info("Query param found.");
            } else if (parameter.isAnnotationPresent(HeaderParam.class)) {
                HeaderParam annotation = parameter.getAnnotation(HeaderParam.class);
                paramV = toObject(request.getRequestHeaders().get(annotation.value()), parameterClass);
                logger.info("Header param found.");
            } else if (parameter.isAnnotationPresent(FormParam.class)) {
                logger.info("Got Form Parameter");
                FormParam annotation = parameter.getAnnotation(FormParam.class);
                paramV = getFormValue(request, parameterClass, paramV, annotation.value());
            } else if (parameter.getType() == Context.class) { /* Lambda Context can be automatically injected */
                paramV = (lambdaContext);
                logger.info("Context insert.");
            } else if (consumesAnnotation != null) {
                logger.info("Using Consumer type to populate parameter.");
                //Pass raw request body
                paramV = consumeAnnotation(request, consumesAnnotation, objectMapper, parameter, parameterClass, paramV);
            } else {
                logger.info("Got fallback for Parameter: " + parameter.getName()); // TODO not working.
                logger.info("Body is param class: " + parameterClass.getName());
                throw new InvalidParameterException("Parameter mismatch");
            }
            varargs.add(paramV);
        }

        return method.invoke(instance, varargs.toArray());
    }

    private static Object consumeAnnotation(Request request, Consumes consumesAnnotation, ObjectMapper objectMapper, Parameter parameter, Class<?> parameterClass, Object paramV) throws IOException {
        if (consumesSpecificType(consumesAnnotation, MediaType.APPLICATION_JSON)) {
            logger.info("Consume json: " + request.getRequestBody());
            paramV = objectMapper.readValue(request.getRequestBody(), parameterClass);
        } else if (consumesSpecificType(consumesAnnotation, MediaType.TEXT_PLAIN)) {
            logger.info("Consume plain text");
            paramV = request.getRequestBody();
        } else if (consumesSpecificType(consumesAnnotation, MediaType.APPLICATION_FORM_URLENCODED)) {
            String name = parameter.getName(); // TODO fixme this doesn't work
            logger.info("Consume form url encoded data with parameter name: " + name);
            paramV = getFormValue(request, parameterClass, paramV, name);
        }
        return paramV;
    }

    private static Object getFormValue(Request request, Class<?> parameterClass, Object paramV, String name) throws IOException {
        logger.info("Form value decode from url encoded from data: " + (String) request.getRequestBody());
        logger.info("looking for key: " + name);
        // Seems due to the api gateway change it's coming in as JSON regardless.
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBodyDejsoned = objectMapper.readValue(request.getRequestBody(), String.class);
        List<NameValuePair> formParams = URLEncodedUtils.parse(requestBodyDejsoned, Charset.forName("UTF-8"));
        List<String> strings = new ArrayList<String>();
        for (NameValuePair each : formParams) {
            if (each.getName().equals(name)) {
                strings.add(each.getValue());
            }
        }
        if (!Collection.class.isAssignableFrom(parameterClass)) {
            if (strings.size() > 0) {
                paramV = objectMapper.convertValue(strings.get(0), parameterClass);
            }
        } else {
            // Jackson's Object mapper comes with a good transformer
            paramV = objectMapper.convertValue(strings, parameterClass);
        }
        return paramV;
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
