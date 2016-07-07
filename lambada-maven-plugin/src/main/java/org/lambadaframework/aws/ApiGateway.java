package org.lambadaframework.aws;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.apigateway.AmazonApiGateway;
import com.amazonaws.services.apigateway.AmazonApiGatewayClient;
import com.amazonaws.services.apigateway.model.*;
import org.lambadaframework.deployer.Deployment;
import org.lambadaframework.jaxrs.model.Resource;
import org.lambadaframework.jaxrs.model.ResourceMethod;
import org.lambadaframework.jaxrs.JAXRSParser;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;


public class ApiGateway extends AWSTools {


    protected static final int API_LIMIT = 500;

    protected static final String PACKAGE_VARIABLE = "<PACKAGE>";

    protected static final String SLASH_CHARACTER = "/";

    protected static final int[] RESPONSE_CODES = {
            200,
            201,
            202,
            301,
            302,
            400,
            401,
            403,
            500
    };

    protected static final String[] HTTP_METHODS = {
            "GET", "POST", "DELETE", "HEAD", "OPTIONS", "PATCH", "PUT"
    };


    protected final String INPUT_TEMPLATE = "{\n" +
            "  \"package\": \"" + PACKAGE_VARIABLE + "\",\n" +
            "  \"pathtemplate\": \"$context.resourcePath\",\n" +
            "  \"method\": \"$context.httpMethod\",\n" +
            "  \"requestbody\": \"$util.escapeJavaScript($input.json('$'))\",\n" +
            "      #foreach($elem in $input.params().keySet())\n" +
            "        \"$elem\": {\n" +
            "            #foreach($innerElem in $input.params().get($elem).keySet())\n" +
            "        \"$innerElem\": \"$util.urlDecode($input.params().get($elem).get($innerElem))\"#if($foreach.hasNext),#end\n" +
            "      #end\n" +
            "        }#if($foreach.hasNext),#end\n" +
            "      #end\n" +
            "}";


    protected final String OUTPUT_TEMPLATE = "$input.json('$.entity')";

    protected final String AUTHORIZATION_TYPE = "NONE";
    protected final String INVOCATION_METHOD = "POST";

    protected Deployment deployment;

    protected String functionArn;

    protected String roleArn;

    public ApiGateway(Deployment deployment, String functionArn, String roleArn) {
        this.deployment = deployment;
        this.functionArn = functionArn;
        this.roleArn = roleArn;
    }

    protected AmazonApiGateway apiGatewayClient;

    protected RestApi amazonApi;

    protected AmazonApiGateway getApiGatewayClient() {
        if (apiGatewayClient != null) {
            return apiGatewayClient;
        }

        return apiGatewayClient = new AmazonApiGatewayClient(getAWSCredentialsProvideChain()).withRegion(Region.getRegion(Regions.fromName(deployment.getRegion())));
    }

    /**
     * This method scans the compiled JAR package for JAX-RS Annotations and create
     * API Gateway endpoints.
     *
     * @throws IOException General exception while deploying
     */
    public void deployEndpoints()
            throws IOException {
        if (log != null)
            log.info("API Gateway deployment is being initialized.");

        List<Resource> resources = getResources();

        if (log != null)
            log.info(resources.size() + " resources found in JAR File.");


        createOrUpdateApi();
        walkThroughResources(resources);
        createDeployment();
    }

    private void createDeployment() {

        if (log != null) {
            log.info("Creating new deployment");
        }

        CreateDeploymentResult deploymentResult = getApiGatewayClient().createDeployment(new CreateDeploymentRequest()
                .withRestApiId(amazonApi.getId())
                .withDescription(deployment.getProjectName() + " v" + deployment.getVersion())
                .withStageDescription(deployment.getStage())
                .withStageName(deployment.getStage())
        );

        if (log != null) {
            log.info("Created new deployment: " + deploymentResult.getId());
        }


        String apiUrl = "https://" +
                amazonApi.getId() +
                ".execute-api." +
                deployment.getRegion() +
                ".amazonaws.com/" +
                deployment.getStage();


        if (log != null) {
            log.info("Your API is online at: " + apiUrl);
        }

    }

    protected String getApiName() {
        return deployment.getProjectName();
    }

    protected String getApiDescription() {
        return "API Gateway for " + deployment.getProjectName();
    }

    /**
     * Creates or updates the API Gateway API
     */
    private void createOrUpdateApi() {


        for (RestApi currentApi : getApiGatewayClient().getRestApis(new GetRestApisRequest().withLimit(API_LIMIT)).getItems()) {
            if (currentApi.getName().equals(getApiName())) {
                amazonApi = currentApi;
                if (log != null) {
                    log.info("Returning API: " + amazonApi.getId());
                }
                return;
            }
        }

        String createdApiId = getApiGatewayClient().createRestApi(new CreateRestApiRequest().withName(getApiName()).withDescription(getApiDescription())).getId();
        if (log != null) {
            log.info("API Gateway created: " + createdApiId);
        }
        createOrUpdateApi();
    }

    /**
     * Gets path param of the resource
     *
     * @param resource Resource
     * @return Path part of the resource
     */
    protected String getPathPartOfResource(Resource resource) {
        String[] pathParts = resource.getPath().split(SLASH_CHARACTER);
        if (pathParts.length > 0) {
            return pathParts[pathParts.length - 1];
        }
        return "";
    }


    /**
     * Gets all path elements of the resource
     *
     * @param resource Resource
     * @return Get path elements
     */
    protected String[] getPathElementsOfResource(Resource resource) {
        String fullPath = resource.getPath();
        if (fullPath.equals(SLASH_CHARACTER)) {
            return new String[]{
                    SLASH_CHARACTER
            };
        }

        String[] parts = fullPath.split(SLASH_CHARACTER);
        parts[0] = SLASH_CHARACTER;
        return parts;
    }


    /**
     * Get parent path of the resource
     *
     * @param resource Resource to find parent path
     * @return Parent path
     */
    protected String getParentPathOfResource(Resource resource) {
        String fullPath = resource.getPath();
        if (fullPath.equals(SLASH_CHARACTER)) {
            return null;
        }

        int lastIndexOfSlash = fullPath.lastIndexOf(SLASH_CHARACTER);

        if (lastIndexOfSlash > 0) {
            return fullPath.substring(0, lastIndexOfSlash);
        }

        return SLASH_CHARACTER;
    }

    /**
     * This method returns REST Resources found in the JAR File.
     * <p>
     * It used JAX-RS Scanner package, and uses the local copy of the JAR file in the target directory
     *
     * @return Found resources
     * @throws IOException
     */
    protected List<Resource> getResources()
            throws IOException {
        String jarFileLocation = deployment.getJarFileLocationOnLocalFileSystem();

        if (log != null)
            log.info("JAR File is being scanned. Used JAR File location: " + jarFileLocation + " Package: " + deployment.getPackageName());

        JAXRSParser parser = new JAXRSParser().withJarFile(jarFileLocation, deployment.getPackageName());
        return parser.scan();
    }


    protected void walkThroughResources(List<Resource> resources) {

        if (resources.size() == 0) {
            if (log != null) {
                log.info("Not found any resources to deploy");
            }

            return;
        }


        if (log != null) {
            log.info("Removing all resources");
        }

        for (com.amazonaws.services.apigateway.model.Resource currentResource : getApiGatewayClient().getResources(new GetResourcesRequest()
                .withLimit(API_LIMIT)
                .withRestApiId(amazonApi.getId())
        ).getItems()) {
            try {
                getApiGatewayClient().deleteResource(new DeleteResourceRequest()
                        .withRestApiId(amazonApi.getId())
                        .withResourceId(currentResource.getId())
                );
            } catch (BadRequestException | NotFoundException e) {
                /**
                 * Tried to remove root resource or a child resource whose parent already removed which is not possible.
                 * Do not do nothing
                 */
            }
        }

        resources.forEach(resourceToDeploy -> deployResource(resourceToDeploy));
    }

    /**
     * Finds a resource in the API Gateway by path
     *
     * @param path Path to search
     * @return Found API Gateway resource
     */
    protected com.amazonaws.services.apigateway.model.Resource getResourceByPath(String path) {

        List<com.amazonaws.services.apigateway.model.Resource> resources = getApiGatewayClient().getResources(new GetResourcesRequest()
                .withRestApiId(amazonApi.getId())
                .withLimit(API_LIMIT)
        ).getItems();


        for (com.amazonaws.services.apigateway.model.Resource currentResource : resources) {
            if (path.equals(currentResource.getPath())) {
                return currentResource;
            }
        }
        return null;
    }


    protected String createResource(Resource jerseyResource) {

        com.amazonaws.services.apigateway.model.Resource rootResource = getResourceByPath(SLASH_CHARACTER);
        String parentResource = rootResource.getId();

        String[] paths = getPathElementsOfResource(jerseyResource);
        String createdPath = "";

        for (String path : paths) {

            if (path.equals(SLASH_CHARACTER)) {
                continue;
            }

            createdPath += SLASH_CHARACTER + path;

            try {
                CreateResourceRequest createResourceInput = new CreateResourceRequest();
                createResourceInput.withRestApiId(amazonApi.getId());
                createResourceInput.withPathPart(path);
                createResourceInput.withParentId(parentResource);
                parentResource = getApiGatewayClient().createResource(createResourceInput).getId();

            } catch (ConflictException e) {
                /**
                 * Resource already exists, only get its id and assign to parentResource
                 */
                parentResource = getResourceByPath(createdPath).getId();
            }
        }

        return parentResource;
    }


    protected boolean deployResource(Resource jerseyResource) {

        com.amazonaws.services.apigateway.model.Resource amazonApiResource;
        String fullPath = jerseyResource.getPath();

        if (log != null) {
            log.info("Resource is being created: " + fullPath);
        }

        String createdId = createResource(jerseyResource);

        if (log != null) {
            log.info("Resource created: " + fullPath + " (" + createdId + ")");
        }

        amazonApiResource = getResourceByPath(fullPath);
        deployMethods(jerseyResource, amazonApiResource);
        return true;
    }


    /**
     * Gets Function ARN for API Gateway.
     * <p>
     * This is one of the undocumented stuff of API Gateway
     *
     * @return Function ARN formatted for API Gateway
     */
    protected String getFunctionArnForApiGateway() {
        return "arn:aws:apigateway:"
                + deployment.getRegion()
                + ":lambda:path/2015-03-31/functions/"
                + functionArn
                + "/invocations";
    }


    protected void deployMethods(Resource jerseyResource, com.amazonaws.services.apigateway.model.Resource apiGatewayResource) {

        if (log != null) {
            log.info("Methods are being deployed");
            log.info("Removing all methods");
        }


        for (String methodToDelete : HTTP_METHODS) {
            try {
                getApiGatewayClient().deleteMethod(
                        new DeleteMethodRequest()
                                .withHttpMethod(methodToDelete)
                                .withRestApiId(amazonApi.getId())
                                .withResourceId(apiGatewayResource.getId()));


                /**
                 * To prevent TooManyRequestsException errors from API Gateway
                 */
                Thread.sleep(1000);

                if (log != null) {
                    log.info(methodToDelete + " method deleted on resource id " + apiGatewayResource.getId());
                }
            } catch (NotFoundException e) {
                /**
                 * Do nothing, continue
                 */
            } catch (InterruptedException e) {

                if (log != null) {
                    log.error("A system error occured. Recovering");
                }

                deployMethods(jerseyResource, apiGatewayResource);
            }
        }


        jerseyResource.getResourceMethods().forEach(method -> {

            String httpMethod = method.getHttpMethod();

            if (log != null) {
                log.info("Creating " + httpMethod + " method on resource " + apiGatewayResource.getId());
            }


            /**
             * Creating method
             */
            getApiGatewayClient().putMethod(new PutMethodRequest()
                    .withRestApiId(amazonApi.getId())
                    .withResourceId(apiGatewayResource.getId())
                    .withHttpMethod(httpMethod)
                    .withApiKeyRequired(false)
                    .withAuthorizationType(AUTHORIZATION_TYPE)
                    .withRequestParameters(getRequestParameters(method))
            );


            getApiGatewayClient().putIntegration(new PutIntegrationRequest()
                    .withRestApiId(amazonApi.getId())
                    .withResourceId(apiGatewayResource.getId())
                    .withHttpMethod(httpMethod)
                    .withType(IntegrationType.AWS)
                    .withUri(getFunctionArnForApiGateway())
                    .withIntegrationHttpMethod(INVOCATION_METHOD)
                    .withRequestTemplates(getInputTemplate(method))
                    .withRequestParameters(getRequestParametersIntegration(method))
            );


            /**
             * Put response codes
             */
            for (int responseCode : RESPONSE_CODES) {
                getApiGatewayClient().putMethodResponse(new PutMethodResponseRequest()
                        .withRestApiId(amazonApi.getId())
                        .withResourceId(apiGatewayResource.getId())
                        .withHttpMethod(httpMethod)
                        .withStatusCode(String.valueOf(responseCode))
                );


                String selectionPattern = (responseCode != 200 ? String.valueOf(responseCode) + ".*" : "");

                getApiGatewayClient().putIntegrationResponse(new PutIntegrationResponseRequest()
                        .withRestApiId(amazonApi.getId())
                        .withResourceId(apiGatewayResource.getId())
                        .withHttpMethod(httpMethod)
                        .withSelectionPattern(selectionPattern)
                        .withResponseTemplates(getResponseTemplate())
                        .withStatusCode(String.valueOf(responseCode))
                );

            }

        });
    }


    private Map<String, String> getResponseTemplate() {

        Map<String, String> responseTemplate = new LinkedHashMap<>();

        responseTemplate.put(MediaType.APPLICATION_JSON, OUTPUT_TEMPLATE);

        return responseTemplate;
    }


    protected Map<String, String> getInputTemplate(ResourceMethod jerseyMethod) {
        String packageName = jerseyMethod.getInvocable().getHandler().getHandlerClass().getPackage().getName();
        Map<String, String> requestTemplates = new LinkedHashMap<>();
        requestTemplates.put(MediaType.APPLICATION_JSON, INPUT_TEMPLATE.replace(PACKAGE_VARIABLE, packageName));
        return requestTemplates;
    }


    private Map<String, Boolean> getRequestParameters(ResourceMethod method) {

        Map<String, Boolean> requestParameters = new LinkedHashMap<>();

        method.getInvocable().getParameters().forEach(parameter -> {
            if (parameter.isAnnotationPresent(QueryParam.class)) {
                QueryParam annotation = parameter.getAnnotation(QueryParam.class);
                requestParameters.put("method.request.querystring." + annotation.value(), true);
            }

            if (parameter.isAnnotationPresent(HeaderParam.class)) {
                HeaderParam annotation = parameter.getAnnotation(HeaderParam.class);
                requestParameters.put("method.request.header." + annotation.value(), true);
            }

            if (parameter.isAnnotationPresent(PathParam.class)) {
                PathParam annotation = parameter.getAnnotation(PathParam.class);
                requestParameters.put("method.request.path." + annotation.value(), true);
            }
        });

        return requestParameters;
    }

    private Map<String, String> getRequestParametersIntegration(ResourceMethod method) {
        Map<String, String> requestParameters = new LinkedHashMap<>();

        method.getInvocable().getParameters().forEach(parameter -> {

            /**
             * Path parameter
             */
            if (parameter.isAnnotationPresent(PathParam.class)) {
                PathParam annotation = parameter.getAnnotation(PathParam.class);
                requestParameters.put("integration.request.path." + annotation.value(), "method.request.path." + annotation.value());
            }


            /**
             * Query parameter
             */
            if (parameter.isAnnotationPresent(QueryParam.class)) {
                QueryParam annotation = parameter.getAnnotation(QueryParam.class);
                requestParameters.put("integration.request.querystring." + annotation.value(), "method.request.querystring." + annotation.value());
            }

            /**
             * Header parameter
             */
            if (parameter.isAnnotationPresent(HeaderParam.class)) {
                HeaderParam annotation = parameter.getAnnotation(HeaderParam.class);
                requestParameters.put("integration.request.header." + annotation.value(), "method.request.header." + annotation.value());
            }


        });


        return requestParameters;
    }

}
