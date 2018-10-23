package com.rackspace.idm.domain.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DynamoDBClientConfiguration {

    public static final String PASSWORD_BLACKLIST_TABLE_NAME = "pwd_blacklist";
    public static final String PASSWORD_BLACKLIST_PASWORDHASH_COLUMN_NAME = "pwd_hash";
    public static final String PASSWORD_BLACKLIST_COUNT_COLUMN_NAME = "count";

    @Autowired
    IdentityConfig identityConfig;


    @Bean
    public AmazonDynamoDB buildDynamoDBClient(AwsClientBuilder.EndpointConfiguration endpointConfiguration,
                                              AWSCredentials awsCredentials){

        return AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withClientConfiguration(new ClientConfiguration().withRequestTimeout((int)identityConfig.getStaticConfig().getDynamoDbRequestTimeout().toMillis()))
                .build();
    }

    @Bean
    public AwsClientBuilder.EndpointConfiguration buildEndpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration(
                identityConfig.getStaticConfig().getDynamoDbEndpoint(),
                identityConfig.getStaticConfig().getDynamoDbRegion()
        );
    }

    @Bean
    public AWSCredentials buildAwsCredentials() {
        return new BasicAWSCredentials(
                identityConfig.getStaticConfig().getDynamoDbCredentialsKeyId(),
                identityConfig.getStaticConfig().getDynamoDbCredentialsSecret()
        );
    }
}
