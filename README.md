[![Build Status](https://travis-ci.org/lambadaframework/lambadaframework.svg?branch=master)](https://travis-ci.org/lambadaframework/lambadaframework) [![Gitter](https://badges.gitter.im/lambadaframework/lambadaframework.svg)](https://gitter.im/lambadaframework/lambadaframework?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

# Lambada Framework (BETA)

Lambada framework is a REST framework that implements [JAX-RS](https://jax-rs-spec.java.net/)  API and lets you deploy your applications to [AWS Lambda](https://aws.amazon.com/lambda/) and [API Gateway](https://aws.amazon.com/api-gateway/) in a serverless fashion. With Lambada you can migrate the existing JAX-RS applications with a very little effort and build scalable applications without having to deal with servers.

## Features

* Support for the most common JAX-RS annotations.
* XML based configuration for Lambda function including VPC, custom execution role
* Support for multiple stages and regions.

Lambada consists of a runtime module, a local simulator and finally a maven plugin to configure and deploy the whole project to API Gateway.

## Getting started

We prepared an example project to show how to configure a project for Lambada and deploy it to API Gateway. 

1. Download the [example project](https://github.com/lambadaframework/lambadaframework-boilerplate).
2. Make sure your default profile has admin privileges or at least has the following policy:

    ```
        {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Effect": "Allow",
                    "Action": [
                        "cloudformation:*",
                        "s3:*",
                        "lambda:*",
                        "execute-api:*",
                        "apigateway:*",
                        "iam:*",
                        "ec2:DescribeSecurityGroups",
                        "ec2:DescribeVpcs",
                        "ec2:DescribeSubnets"
                    ],
                    "Resource": [
                        "*"
                    ]
                }
            ]
        }
    ```
3. See `pom.xml` of the boilerplate project and edit the variables depending of your needs. (You must definitely change `deployment.bucket` property but you can leave the rest as defaults.)
4. In the root repository execute the following command:

    `mvn deploy`

If the process is successful, the final URL of your API is printed on the screen. You can navigate to the URL and execute your url.

## Architecture

Lambada consists of a runtime module, a maven wagon for uploading to the S3 bucket and finally a maven plugin for deploying. The both modules rely on a classpath scanner that scans the whole project for JAX-RS annotations. When you deploy a project with Lambada,

1. **Package stage:** [Maven shade plugin](https://maven.apache.org/plugins/maven-shade-plugin/) creates an Uber JAR file for your project with all your dependencies and Lambada runtime module. 

2. **Pre-deploy stage:** At this stage `prepare` goal of the maven plugin executes and creates the S3 bucket if it does not exist. (Once you have the S3 bucket you can skip this stage to shorten the deployment process but the recommended way is to leave that as it is.)

3. **Deploy stage:** After creation of the JAR file, maven wagon uploads the JAR to your S3 bucket.

4. **Post deploy stage:** After you have your JAR file in the S3 bucket, Lambada maven plugin executes again with `deploy` goal and scans all the JAR file for any JAX-RS resources. Using this information, it creates the necessary endpoints and methods in API Gateway and set up all the necessary stuff.

Once your API gateway is created, you can open AWS Console to see how Lambada creates endpoints and methods. However, **we strongly recommend** to not to touch method settings because the runtime module heavily relies on the settings, specially the mapping configuration that is created automatically for you.

When your API is invoked for the first time, the runtime module also scans the lambda function's JAR file for JAX-RS resources and creates a router map. Depending of the request's properties such as the path and HTTP method, the Lambada router finds the correct JAX-RS method to call, and serializes its response to JSON and send back to the client.

## Configuration options

Lambada Maven plugin has some configuration options.

The configuration values should be present under maven plugin's configuration.

```
<build>
    <plugins>
        <plugin>
            <groupId>org.lambadaframework</groupId>
            <artifactId>maven-plugin</artifactId>
            <version>0.0.5</version>
            <configuration>
	            ...
				<!-- Configuration options -->
				....
			</configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare</goal>
                        <goal>deploy</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

All the possible parameters are as follows:

| Parameter                     | Default value                        | Required? |
|-------------------------------|--------------------------------------|-----------|
| packageName                   | N/A                                  | Yes       |
| regionToDeploy                | N/A                                  | Yes       |
| stageToDeploy                 | N/A                                  | Yes       |
| lambdaMaximumExecutionTime    | 3                                    | No        |
| lambdaMemorySize              | 128                                  | No        |
| lambdaExecutionRolePolicies   | N/A                                  | No        |
| lambdaSubnetIds               | N/A                                  | No        |
| lambdaSecurityGroups          | N/A                                  | No        |
| lambdaHandler                 | org.lambadaframework.runtime.Handler | No        |

Below you can find detailed information about the configuration parameters:

- **packageName:** The package name to scan for JAX-RS annotations.

- **regionToDeploy:** AWS Region to deploy your API. It should be a region where Lambda and API Gateway are supported.

- **lambdaMaximumExecutionTime:** Maximum execution time for the AWS Lambda function.

- **lambdaMemorySize:** Maximum allowed memory size for the AWS Lambda function.

    *(Refer to [AWS Lambda Documentation](http://docs.aws.amazon.com/lambda/latest/dg/welcome.html) for possible values.)*

- **lambdaExecutionRolePolicies:** For every deployment, a default execution role is created and attached to the Lambda function. This role has the minimum privileges to execute the Lambda in a VPC environment. However, in a typical scenario to make your Lambda function able to access other AWS resources you should attach the required policies to this role. You can then navigate to the [Policies section](https://console.aws.amazon.com/iam/home#policies) of AWS IAM console, create a custom policy or pick a managed one, then specify their ARN's at your `pom.xml`. For instance, to make the Lambda function to able to access the whole SNS features, you can add this line to your `pom.xml`, under :

    ```
    <lambdaExecutionRolePolicies>
        <param>arn:aws:iam::aws:policy/AmazonSNSFullAccess</param>
    </lambdaExecutionRolePolicies>
    
    ```
    
- **lambdaSubnetIds & lambdaSecurityGroups:** You might be wanting to configure your lambda to operate within a VPC. In this case you should set these parameter as follows:

    ```
    
    <lambdaSubnetIds>
        <params>subnet-93efeef6</params>
        <params>subnet-93efdde6</params>
        <params>subnet-93efees2</params>
    </lambdaSubnetIds>
    <lambdaSecurityGroups>
        <params>sg-6c231d09</params>
        <params>sg-6c251d42</params>
    </lambdaSecurityGroups>
    ```
- **lambdaHandler:** The entrypoint for the lambda. You will more than likely want to
leave this as the default value so that integration with API Gateway is automatic. Only
override this if you are using the lambada-maven-plugin to deploy your own Lambda functions.

## Other projects

You might want to look at other projects about serverless architecture:

 - [Serverless](https://github.com/serverless/serverless): Reputable framework to deploy serverless projects in Node.JS
 - [ingenieux/lambada](https://github.com/ingenieux/lambada): Another JAVA framework with a similar name with Lambada Framework. It uses a slightly different approach than Lambada Framework to develop serverless projects.
 - [Zappa](https://github.com/Miserlou/Zappa): Develop and deploy serverless applications with Python.

## Links:

 - [AWS Lambda Documentation](http://docs.aws.amazon.com/lambda/latest/dg/welcome.html)
 - [API Gateway Documentation](http://docs.aws.amazon.com/apigateway/latest/developerguide/welcome.html)
 - [Lambada Framework boilerplate project](https://github.com/lambadaframework/lambadaframework-boilerplate)
 - [How to use Spring IoC in AWS Lambda](https://github.com/cagataygurturk/aws-lambda-java-boilerplate)

## Contributing:

Feel free to send a PR to `develop` branch for any contribution. We'll be publishing a Roadmap in the future.
