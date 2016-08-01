package org.lambadaframework.aws;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.lambadaframework.deployer.Deployment;
import org.lambadaframework.deployer.LambadaDeployer;
import org.apache.maven.project.MavenProject;
import org.lambadaframework.jaxrs.model.Resource;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Properties;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Resource.class)
public class ApiGatewayTest {

    protected LambadaDeployer getExampleProject() {
        MavenProject project = new MavenProject();
        project.getProperties().setProperty("deployment.bucket", "maven.cagataygurturk.com");
        project.setGroupId("org.lambadaframework");
        project.setArtifactId("runtime-deploy-maven-plugin");
        project.setVersion("0.0.2");

        Properties props = new Properties();
        props.put("ProjectName", "LambadaTestProject");

        LambadaDeployer deployer = new LambadaDeployer();
        deployer.mavenProject = project;
        deployer.stageToDeploy = "dev";
        deployer.regionToDeploy = "eu-west-1";
        //deployer.cloudFormationParameters = props;
        return deployer;
    }


    protected Deployment getMockDeployment() {

        return mock(Deployment.class);
    }


    @Test
    public void testGetPathPartOfResource() throws Exception {
        String functionArn = "testArn";
        String roleArn = "testArn";

        ApiGateway apiGateway = new ApiGateway(getMockDeployment(), functionArn, roleArn);

        final Resource mockResource = PowerMock.createMock(Resource.class);
        expect(mockResource.getPath())
                .andReturn("/resource1")
                .times(1);

        expect(mockResource.getPath())
                .andReturn("/")
                .times(2);

        PowerMock.replay(mockResource);

        assertEquals("resource1", apiGateway.getPathPartOfResource(mockResource));

        assertEquals("", apiGateway.getPathPartOfResource(mockResource));
    }

    @Test
    public void testGetPathElementsOfResource() throws Exception {
        String functionArn = "testArn";
        String roleArn = "testArn";

        ApiGateway apiGateway = new ApiGateway(getMockDeployment(), functionArn, roleArn);

        final Resource mockRootResource = PowerMock.createMock(Resource.class);
        final Resource mockParentResource = PowerMock.createMock(Resource.class);
        final Resource mockResource = PowerMock.createMock(Resource.class);

        expect(mockRootResource.getPath())
                .andReturn("/")
                .anyTimes();


        expect(mockResource.getPath())
                .andReturn("/resource1/test/{name}")
                .anyTimes();


        PowerMock.replayAll();


        String[] parts = apiGateway.getPathElementsOfResource(mockResource);
        assertEquals(4, parts.length);
        assertEquals("/", parts[0]);
        assertEquals("resource1", parts[1]);
        assertEquals("test", parts[2]);
        assertEquals("{name}", parts[3]);

        String[] rootParts = apiGateway.getPathElementsOfResource(mockRootResource);
        assertEquals(1, rootParts.length);
        assertEquals("/", rootParts[0]);
    }

    @Test
    public void testGetParentPartOfResource() throws Exception {
        String functionArn = "testArn";
        String roleArn = "testArn";

        ApiGateway apiGateway = new ApiGateway(getMockDeployment(), functionArn, roleArn);


        final Resource parentMockResource = PowerMock.createMock(Resource.class);
        expect(parentMockResource.getPath())
                .andReturn("/")
                .anyTimes();
        expect(parentMockResource.getParent())
                .andReturn(null)
                .anyTimes();


        PowerMock.replayAll();

        assertEquals(apiGateway.getParentPathOfResource(parentMockResource),
                null);


        final Resource mockResource = PowerMock.createMock(Resource.class);
        expect(mockResource.getPath())
                .andReturn("/{id}")
                .anyTimes();

        expect(mockResource.getParent())
                .andReturn(parentMockResource)
                .anyTimes();


        final Resource mockSubResource = PowerMock.createMock(Resource.class);

        expect(mockSubResource.getPath())
                .andReturn("/{id}/info")
                .anyTimes();

        expect(mockSubResource.getParent())
                .andReturn(parentMockResource)
                .anyTimes();

        PowerMock.replayAll();


        assertEquals("/",
                apiGateway.getParentPathOfResource(mockResource));


        assertEquals("/{id}",
                apiGateway.getParentPathOfResource(mockSubResource));

    }

}