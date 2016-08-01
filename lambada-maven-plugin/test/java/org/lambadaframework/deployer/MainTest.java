package org.lambadaframework.deployer;

import org.apache.maven.project.MavenProject;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Properties;

public class MainTest {

    protected LambadaDeployer getExampleProject() {
        MavenProject project = new MavenProject();
        project.getProperties().setProperty("deployment.bucket", "maven.cagataygurturk.com");
        project.setGroupId("com.home24.lambdadeploy");
        project.setArtifactId("runtime-deploy-maven-plugin");
        project.setVersion("0.0.2");

        Properties props = new Properties();
        props.put("ProjectName", "MobileApi");

        LambadaDeployer deployer = new LambadaDeployer();
        deployer.mavenProject = project;
        deployer.stageToDeploy = "dev";
        deployer.regionToDeploy = "eu-west-1";
        return deployer;
    }

    @Test
    public void checkRegion() throws Exception {
        LambadaDeployer deployer = getExampleProject();
        deployer.checkRegion("eu-west-1");
    }

    @Test(expected = RuntimeException.class)
    public void checkInvalidRegion() throws Exception {
        LambadaDeployer deployer = getExampleProject();
        deployer.checkRegion("eu-west-5");
    }
}