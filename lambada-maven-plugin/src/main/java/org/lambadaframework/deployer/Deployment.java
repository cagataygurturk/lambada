package org.lambadaframework.deployer;


import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.GetUserRequest;
import com.amazonaws.services.identitymanagement.model.GetUserResult;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.lambadaframework.aws.S3;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

public class Deployment {

    private static String seperator = "/";

    protected String cloudFormationRoleName;

    protected String packageName;

    protected String stage;

    protected String region;

    protected String bucket;

    protected String deploymentS3KeyTemplate;

    protected Properties properties;

    protected MavenProject project;

    private static final String deploymentBucketPropertyName = "deployment.bucket";
    public static final String LAMBDA_MAXIMUM_EXECUTION_TIME_KEY = "LambdaMaximumExecutionTime";
    public static final int LAMBDA_MAXIMUM_EXECUTION_TIME_DEFAULT_VALUE = 3;

    public static final String LAMBDA_MEMORY_SIZE_KEY = "LambdaMemorySize";
    public static final int LAMBDA_MEMORY_SIZE_DEFAULT_VALUE = 128;

    public static final String LAMBDA_EXECUTION_ROLE_POLICY_KEY = "LambdaExecutionRoleManagedPolicyARNs";

    public static final String LAMBDA_VPC_SUBNETS_KEY = "SubnetIds";

    public static final String LAMBDA_VPC_SECURITY_GROUPS_KEY = "SecurityGroupIds";


    public static final String S3_DEPLOYMENT_BUCKET_KEY = "DeploymentS3Bucket";
    public static final String S3_DEPLOYMENT_KEY_KEY = "DeploymentS3Key";
    public static final String LAMBDA_HANDLER_KEY = "LambdaHandler";
    public static final String LAMBDA_HANDLER_DEFAULT_VALUE = "org.lambadaframework.runtime.Handler";
    public static final String STAGE_KEY = "Stage";

    public static final String LAMBDA_DESCRIPTION_KEY = "LambdaDescription";


    protected Log log;

    public Deployment(MavenProject project,
                      String packageName,
                      Properties properties,
                      String region,
                      String stage,
                      String bucket,
                      String deploymentS3KeyTemplate, String cloudformationRoleName) {
        this.project = project;
        this.packageName = packageName;
        this.region = region;
        this.properties = properties;
        this.stage = stage;
        this.bucket = bucket;
        this.deploymentS3KeyTemplate = deploymentS3KeyTemplate;
        this.cloudFormationRoleName = cloudformationRoleName;

        setDefaultParameters();
    }

    public void setLog(Log log) {
        this.log = log;
    }


    public String getPackageName() {
        return packageName;
    }

    public String getProjectName() {
        return project.getGroupId() + "." + project.getArtifactId();
    }

    /**
     * Sets default Cloudformation Parameters
     */
    protected void setDefaultParameters() {

        properties.setProperty(S3_DEPLOYMENT_BUCKET_KEY, getBucketName());
        properties.setProperty(S3_DEPLOYMENT_KEY_KEY, getJarFileLocationOnS3(getVersion()));
        properties.setProperty(LAMBDA_DESCRIPTION_KEY, getLambdaDescription());
        properties.setProperty(STAGE_KEY, getStage());


        if (properties.getProperty(LAMBDA_MAXIMUM_EXECUTION_TIME_KEY) == null) {
            properties.setProperty(LAMBDA_MAXIMUM_EXECUTION_TIME_KEY, Integer.toString(LAMBDA_MAXIMUM_EXECUTION_TIME_DEFAULT_VALUE));
        }

        if (properties.getProperty(LAMBDA_MEMORY_SIZE_KEY) == null) {
            properties.setProperty(LAMBDA_MEMORY_SIZE_KEY, Integer.toString(LAMBDA_MEMORY_SIZE_DEFAULT_VALUE));
        }

        if (properties.getProperty(LAMBDA_HANDLER_KEY) == null) {
            properties.setProperty(LAMBDA_HANDLER_KEY, LAMBDA_HANDLER_DEFAULT_VALUE);
        }


    }

    public String getVersion() {
        return this.project.getArtifact().getVersion();
    }

    public String getStage() {
        return stage;
    }

    public String getRegion() {
        return region;
    }

    /**
     * Stack name is a combination of Artifact Id and stage
     * For every stage we have a different CF Stack, thus different Lambda functions and roles.
     *
     * @return CF Stack Name
     */
    public String getCloudFormationStackName() {
        return project.getArtifactId() + "-" + getStage();
    }

    public Collection<Parameter> getCloudFormationParameters() {
        Collection<Parameter> parameters = new ArrayList<>();
        for (Map.Entry<Object, Object> e : properties.entrySet()) {
            parameters.add(new Parameter().withParameterKey((String) e.getKey()).withParameterValue((String) e.getValue()));
        }
        return parameters;
    }

    public String getJarFileLocationOnLocalFileSystem() {
        return project.getBuild().getDirectory() + seperator + project.getBuild().getFinalName() + "." + project.getPackaging();
    }

    public String getBucketName() {
        return this.bucket;
    }

    /**
     * Get JAR file location of the latest version
     */
    public String getJarFileLocationOnS3() {
        return getJarFileLocationOnS3(this.getVersion());
    }

    /**
     * Get JAR file location by version
     *
     * @param version
     */
    public String getJarFileLocationOnS3(String version) {

        String bucketKey = this.project.getGroupId().replace(".", seperator) + seperator
                + this.project.getArtifactId() + seperator
                + this.project.getArtifact().getBaseVersion() + seperator
                + this.project.getArtifactId() + "-" + version + ".jar";

        if (this.project.getArtifact().getBaseVersion().contains("SNAPSHOT")) {
            bucketKey = "snapshots/" + bucketKey;
        } else {
            bucketKey = "releases/" + bucketKey;
        }

        /*
        if (!(new S3(this.getBucketName(), bucketKey)).isFileExists()) {
            throw new RuntimeException("Required version (" + version + ") does not exist on S3 bucket (s3://" + this.getBucketName() + "/" + bucketKey + ". Aborting.");
        }
        */

        return bucketKey;
    }


    public static Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    /**
     * Get latest version number using maven-metadata.xml file
     * <p>
     * This information is used to determine which version deploy when a specific version is not given as a parameter
     *
     * @return String
     */
    protected String getLatestVersion() {
        try {

            String bucketKey = "releases" + seperator + this.project.getGroupId().replace(".", seperator)
                    + seperator
                    + this.project.getArtifactId()
                    + seperator;

            String metadataFileName = bucketKey + "maven-metadata.xml";

            if (log != null)
                log.info("Getting the latest project info from s3://" + getBucketName() + metadataFileName);


            Document doc = loadXMLFromString(S3.getFile(getBucketName(), metadataFileName));
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("/metadata/versioning/latest");
            return ((Node) expr.evaluate(doc, XPathConstants.NODE)).getTextContent();
        } catch (Exception e) {
            throw new RuntimeException("Error at get maven metadata. Did you deploy the compiled project to S3 Bucket?", e);
        }
    }

    public String getLambdaDescription() {
        return "Lambada function for " + project.getName();
    }

    public String getS3TemplateUrl() {

        String key = this.deploymentS3KeyTemplate;
        String bucketName = this.getBucketName();
        if ((bucketName == null) || (key == null)) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("https://s3-eu-west-1.amazonaws.com/");
            sb.append(bucketName);
            sb.append("/");
            sb.append(key);
            return sb.toString();
        }

    }

    public String getCloudFormationRoleName() {
        return this.cloudFormationRoleName;
    }

    public String getAccountId() {

        GetUserResult user = new AmazonIdentityManagementClient().getUser(new GetUserRequest());
        String[] arnParts = user.getUser().getArn().split(":");
        log.debug("AccountID parsed from ARN: " + arnParts[4]);
        return arnParts[4];
    }

    public String getRoleARN() {

        String role = null;
        if (this.cloudFormationRoleName != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("arn:aws:iam::");
            sb.append(getAccountId());
            sb.append(":");
            sb.append("role/");
            sb.append(this.cloudFormationRoleName);
            role = sb.toString();
        }

        return role;
    }


}
