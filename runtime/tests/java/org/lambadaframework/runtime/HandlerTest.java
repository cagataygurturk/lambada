package org.lambadaframework.runtime;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.MethodHandler;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lambadaframework.runtime.models.Request;
import org.lambadaframework.runtime.models.RequestInterface;
import org.lambadaframework.runtime.router.Router;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    public void dummyTest() {
        assertTrue(true);
    }

    @Test
    public void testParseRequestWithJacksson() throws Exception {

        InputStream jsonAsInputStream = getJsonAsInputStream();
        loggInput(jsonAsInputStream);

        Handler handler = new Handler();
        RequestInterface req = handler.getParsedRequest(jsonAsInputStream);

        assertEquals("GET", req.getMethod().name());
        assertEquals("/test/hello", req.getPathTemplate());
        assertEquals("me", req.getQueryParams().get("name"));
    }

    private JSONObject parseResponse(String json) {

        JSONObject responseJson = new JSONObject();

        JSONObject responseBody = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(json);
        responseBody.put("input", jsonArray);
        responseJson.put("body", responseBody);

        return responseBody;
    }

    private void loggInput(InputStream inputStream) {

        JSONParser parser = new JSONParser();
        try {
            final JSONObject parse = (JSONObject) parser.parse(new BufferedReader(new InputStreamReader(inputStream)));
            inputStream.reset();
            System.out.println("parse = " + parse);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }

    }

    private InputStream getJsonAsInputStream() {
        String json = "{\n" +
                "        \"path\": \"/test/hello\",\n" +
                "        \"headers\": {\n" +
                "            \"Accept\": \"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\",\n" +
                "            \"Accept-Encoding\": \"gzip, deflate, lzma, sdch, br\",\n" +
                "            \"Accept-Language\": \"en-US,en;q=0.8\",\n" +
                "            \"CloudFront-Forwarded-Proto\": \"https\",\n" +
                "            \"CloudFront-Is-Desktop-Viewer\": \"true\",\n" +
                "            \"CloudFront-Is-Mobile-Viewer\": \"false\",\n" +
                "            \"CloudFront-Is-SmartTV-Viewer\": \"false\",\n" +
                "            \"CloudFront-Is-Tablet-Viewer\": \"false\",\n" +
                "            \"CloudFront-Viewer-Country\": \"US\",\n" +
                "            \"Host\": \"wt6mne2s9k.execute-api.us-west-2.amazonaws.com\",\n" +
                "            \"Upgrade-Insecure-Requests\": \"1\",\n" +
                "            \"User-Agent\": \"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36 OPR/39.0.2256.48\",\n" +
                "            \"Via\": \"1.1 fb7cca60f0ecd82ce07790c9c5eef16c.cloudfront.net (CloudFront)\",\n" +
                "            \"X-Amz-Cf-Id\": \"nBsWBOrSHMgnaROZJK1wGCZ9PcRcSpq_oSXZNQwQ10OTZL4cimZo3g==\",\n" +
                "            \"X-Forwarded-For\": \"192.168.100.1, 192.168.1.1\",\n" +
                "            \"X-Forwarded-Port\": \"443\",\n" +
                "            \"X-Forwarded-Proto\": \"https\"\n" +
                "        },\n" +
                "        \"pathParameters\": {\"id\": \"1234\"},\n" +
                "        \"httpMethod\": \"GET\",\n" +
                "        \"queryStringParameters\": {\"name\": \"me\"} \n" +
                "    }";

        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testWith200Result()
            throws Exception {


        Handler handler = new Handler();
        handler.setRouter(getMockRouter("getEntity", long.class));
        ByteArrayOutputStream boas = new ByteArrayOutputStream();


        handler.handleRequest(getJsonAsInputStream(), boas, getContext());

        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(boas.toString());
        JSONObject body = (JSONObject) json.get("body");
        assertEquals(1234L, body.get("id"));
        assertEquals("cagatay gurturk", body.get("query1"));

    }

/*
    @Test
    public void testWith201Result()
            throws Exception {


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


    }*/

}