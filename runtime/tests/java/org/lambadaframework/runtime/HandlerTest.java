package org.lambadaframework.runtime;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.MethodHandler;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lambadaframework.runtime.models.Request;
import org.lambadaframework.runtime.models.Response;
import org.lambadaframework.runtime.router.Router;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Invocable.class, ResourceMethod.class, Router.class, org.lambadaframework.jaxrs.model.ResourceMethod.class})
public class HandlerTest {


    public static class Entity {
        public long id;
        public String query1;
        public String requestBody;
    }


    public static class NewEntityRequest {
        public long id;
    }

    @Path("/")
    public static class DummyController {
        @GET
        @Path("{id}")
        public javax.ws.rs.core.Response getEntity(
                @PathParam("id") long id
        ) {
            Entity entity = new Entity();
            entity.id = id;
            entity.query1 = "cagatay gurturk";
            return javax.ws.rs.core.Response
                    .status(200)
                    .entity(entity)
                    .build();
        }

        @POST
        @Path("{id}")
        public javax.ws.rs.core.Response createEntity(
                @PathParam("id") long id,
                @QueryParam("query1") String query1
        ) {
            Entity entity = new Entity();
            entity.id = id;
            entity.query1 = query1;

            return javax.ws.rs.core.Response
                    .status(201)
                    .header("Location", "http://www.google.com")
                    .entity(entity)
                    .build();
        }

        @POST
        @Path("{id}/jsonstring")
        @Consumes(MediaType.APPLICATION_JSON)
        public javax.ws.rs.core.Response createEntityWithJsonBody(
                String jsonString
        ) {

            return javax.ws.rs.core.Response
                    .status(201)
                    .entity(jsonString)
                    .build();
        }

        @POST
        @Path("{id}/jsonobject")
        @Consumes(MediaType.APPLICATION_JSON)
        public javax.ws.rs.core.Response createEntityWithJsonObject(
                NewEntityRequest jsonEntity
        ) {

            return javax.ws.rs.core.Response
                    .status(201)
                    .entity(jsonEntity)
                    .build();
        }

        @POST
        @Path("{id}/error")
        @Consumes(MediaType.APPLICATION_JSON)
        public javax.ws.rs.core.Response createEntityWithStatus401(
                String jsonString
        ) {

            return javax.ws.rs.core.Response
                    .status(401)
                    .entity(jsonString)
                    .build();
        }
    }

    private Router getMockRouter(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {

        Invocable mockInvocable = PowerMock.createMock(Invocable.class);
        expect(mockInvocable.getHandlingMethod())
                .andReturn(DummyController.class.getDeclaredMethod(methodName, parameterTypes))
                .anyTimes();

        expect(mockInvocable.getHandler())
                .andReturn(MethodHandler.create(DummyController.class))
                .anyTimes();

        org.lambadaframework.jaxrs.model.ResourceMethod mockResourceMethod = PowerMock.createMock(org.lambadaframework.jaxrs.model.ResourceMethod
                .class);
        expect(mockResourceMethod.getInvocable())
                .andReturn(mockInvocable)
                .anyTimes();

        Router mockRouter = PowerMock.createMock(Router.class);
        expect(mockRouter.route(anyObject()))
                .andReturn(mockResourceMethod)
                .anyTimes();

        PowerMock.replayAll();
        return mockRouter;
    }

    private Request getRequest(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Request.class);
    }

    private Context getContext() {
        return new Context() {
            @Override
            public String getAwsRequestId() {
                return "23234234";
            }

            @Override
            public String getLogGroupName() {
                return null;
            }

            @Override
            public String getLogStreamName() {
                return null;
            }

            @Override
            public String getFunctionName() {
                return null;
            }

            @Override
            public String getFunctionVersion() {
                return null;
            }

            @Override
            public String getInvokedFunctionArn() {
                return null;
            }

            @Override
            public CognitoIdentity getIdentity() {
                return null;
            }

            @Override
            public ClientContext getClientContext() {
                return null;
            }

            @Override
            public int getRemainingTimeInMillis() {
                return 5000;
            }

            @Override
            public int getMemoryLimitInMB() {
                return 128;
            }

            @Override
            public LambdaLogger getLogger() {
                return null;
            }
        };
    }


    @Test
    public void testWith200Result()
            throws Exception {

        Request exampleRequest = getRequest("{\n" +
                "  \"package\": \"org.lambadaframework\",\n" +
                "  \"pathTemplate\": \"/{id}\",\n" +
                "  \"method\": \"GET\",\n" +
                "  \"requestBody\": \"{}\",\n" +
                "  \"path\": {\n" +
                "    \"id\": \"123\"\n" +
                "  },\n" +
                "  \"querystring\": {\n" +
                "        \"query1\": \"test3\",\n" +
                "    \"query2\": \"test\"\n" +
                "  },\n" +
                "  \"header\": {}\n" +
                "}");

        Handler handler = new Handler();
        handler.setRouter(getMockRouter("getEntity", long.class));
        Response response = handler.handleRequest(exampleRequest, getContext());

        assertEquals("200", response.getErrorMessage());
        assertEquals("cagatay gurturk", ((Entity) response.getEntity()).query1);
        assertEquals(123, ((Entity) response.getEntity()).id);

    }


    @Test
    public void testWith201Result()
            throws Exception {

        Request exampleRequest = getRequest("{\n" +
                "  \"package\": \"org.lambadaframework\",\n" +
                "  \"pathTemplate\": \"/{id}/jsonstring\",\n" +
                "  \"method\": \"POST\",\n" +
                "  \"requestBody\": \"{}\",\n" +
                "  \"path\": {\n" +
                "    \"id\": \"123\"\n" +
                "  },\n" +
                "  \"querystring\": {\n" +
                "        \"query1\": \"test3\",\n" +
                "    \"query2\": \"test\"\n" +
                "  },\n" +
                "  \"header\": {}\n" +
                "}");


        Handler handler = new Handler();
        handler.setRouter(getMockRouter("createEntity", long.class, String.class));
        Response response = handler.handleRequest(exampleRequest, getContext());

        assertEquals("201", response.getErrorMessage());
        assertEquals("test3", ((Entity) response.getEntity()).query1);
        assertEquals(123, ((Entity) response.getEntity()).id);
        assertEquals("http://www.google.com", response.getHeaders().get("Location"));

    }


    @Test
    public void testWithJsonBodyAsString201Result()
            throws Exception {

        Request exampleRequest = getRequest("{\n" +
                "  \"package\": \"org.lambadaframework\",\n" +
                "  \"pathTemplate\": \"/{id}\",\n" +
                "  \"method\": \"POST\",\n" +
                "  \"requestBody\": \"test\",\n" +
                "  \"path\": {\n" +
                "    \"id\": \"123\"\n" +
                "  },\n" +
                "  \"querystring\": {\n" +
                "        \"query1\": \"test3\",\n" +
                "    \"query2\": \"test\"\n" +
                "  },\n" +
                "  \"header\": {}\n" +
                "}");


        Handler handler = new Handler();
        handler.setRouter(getMockRouter("createEntityWithJsonBody", String.class));
        Response response = handler.handleRequest(exampleRequest, getContext());

        assertEquals("201", response.getErrorMessage());
        assertEquals("test", response.getEntity());
    }


    @Test
    public void testWithJsonAsObject201Result()
            throws Exception {

        Request exampleRequest = getRequest("{\n" +
                "  \"package\": \"org.lambadaframework\",\n" +
                "  \"pathTemplate\": \"/{id}\",\n" +
                "  \"method\": \"POST\",\n" +
                "  \"requestBody\": \"{\\\"id\\\":1}\",\n" +
                "  \"path\": {\n" +
                "    \"id\": \"123\"\n" +
                "  },\n" +
                "  \"querystring\": {\n" +
                "        \"query1\": \"test3\",\n" +
                "    \"query2\": \"test\"\n" +
                "  },\n" +
                "  \"header\": {}\n" +
                "}");


        Handler handler = new Handler();
        handler.setRouter(getMockRouter("createEntityWithJsonObject", NewEntityRequest.class));
        Response response = handler.handleRequest(exampleRequest, getContext());

        assertEquals("201", response.getErrorMessage());
        assertEquals(1, ((NewEntityRequest) response.getEntity()).id);
    }

    @Test
    public void testWith401ExceptionResult()
            throws Exception {

        Request exampleRequest = getRequest("{\n" +
                "  \"package\": \"org.lambadaframework\",\n" +
                "  \"pathTemplate\": \"/{id}/error\",\n" +
                "  \"method\": \"POST\",\n" +
                "  \"requestBody\": \"{}\",\n" +
                "  \"path\": {\n" +
                "    \"id\": \"123\"\n" +
                "  },\n" +
                "  \"querystring\": {\n" +
                "        \"query1\": \"test3\",\n" +
                "    \"query2\": \"test\"\n" +
                "  },\n" +
                "  \"header\": {}\n" +
                "}");


        Handler handler = new Handler();
        handler.setRouter(getMockRouter("createEntityWithStatus401", String.class));
        try {
            handler.handleRequest(exampleRequest, getContext());
            fail("Should have thrown an excpetion");
        } catch(RuntimeException e) {
            assertTrue(e.getMessage().contains("401"));
        }


    }

}