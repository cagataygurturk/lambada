package org.lambadaframework.deployer;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.lambadaframework.AbstractMojoPlugin;
import org.lambadaframework.aws.LambdaFunction;
import org.lambadaframework.aws.ApiGateway;
import org.lambadaframework.aws.Cloudformation;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;


@Mojo(name = "deploy",
        defaultPhase = LifecyclePhase.DEPLOY,
        requiresOnline = true
)
public class LambadaDeployer extends AbstractMojoPlugin {


    protected static final String SUPPORTED_PACKAGING = "jar";

    /**
     * Checks region for valid values.
     * <p>
     * We do not need the return value, so we just check if an exception is thrown or no.
     *
     * @param region Region to check
     */
    public void checkRegion(String region) {
        try {
            Region.getRegion(Regions.fromName(region));
        } catch (Exception e) {
            throw new RuntimeException(region + " is not a AWS region. Please select a valid one.");
        }
    }


    /**
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException {
        try {

            if (!mavenProject.getPackaging().equals(SUPPORTED_PACKAGING)) {
                getLog().info(LOG_SEPERATOR);
                getLog().info("Project packaging is not JAR (potentially a parent project), skipping deployment.");
                return;
            }

            Deployment deployment = getDeployment();
            printLogo();
            getLog().info("Deployment to AWS Lambda and Gateway is starting.");

            checkRegion(regionToDeploy);
            getLog().info("Group Id: " + mavenProject.getGroupId());
            getLog().info("Artifact Id: " + mavenProject.getArtifactId());
            getLog().info("Version to deploy: " + deployment.getVersion());
            getLog().info("Stage to deploy: " + stageToDeploy);
            getLog().info("Region to deploy: " + regionToDeploy);
            getLog().info(LOG_SEPERATOR);

            getLog().info("CLOUDFORMATION");
            Cloudformation.CloudFormationOutput cloudFormationOutput = applyCloudFormation(deployment);
            getLog().info("Deployed IAM Role: " + cloudFormationOutput.getLambdaExecutionRole());
            getLog().info("Deployed Lambda Function ARN: " + cloudFormationOutput.getLambdaFunctionArn());
            getLog().info(LOG_SEPERATOR);

            /**
             * Set up VPC of Lambda, create new version
             */
            /*getLog().info("LAMBDA");
            LambdaFunction lambdaFunction = new LambdaFunction(cloudFormationOutput.getLambdaFunctionArn(), deployment);
            lambdaFunction.setLog(getLog());
            String functionArn = lambdaFunction.deployLatestVersion();
            getLog().info(LOG_SEPERATOR);

            getLog().info("API GATEWAY");
            ApiGateway apiGateway = new ApiGateway(deployment, functionArn, cloudFormationOutput.getLambdaExecutionRole());
            apiGateway.setLog(getLog());
            apiGateway.deployEndpoints();
            getLog().info(LOG_SEPERATOR);*/


        } catch (Exception e) {
            throw new MojoExecutionException("Exception at deployment", e);
        }
    }


}


