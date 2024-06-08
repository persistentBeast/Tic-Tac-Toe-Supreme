package com.persistent.tictactoe.clients;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDb {

    private static DynamoDbEnhancedClient dynamoDbClient = null;

    private static DynamoDbClient ddb = null;

    public static synchronized DynamoDbEnhancedClient getDynamoDbClient(){
        if(dynamoDbClient == null){
            DynamoDbClient ddb = getDdbClient();
            dynamoDbClient = DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(ddb)
                    .build();
        }
        return dynamoDbClient;
    }

    public static synchronized DynamoDbClient getDdbClient(){
        if(dynamoDbClient == null){
            Region region = Region.AP_SOUTH_1;
            ddb = DynamoDbClient.builder()
                    .region(region)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(System.getenv().get("AWS_ACCESS_KEY_ID"),
                            System.getenv().get("AWS_SECRET_KEY"))))
                    .build();
        }
        return ddb;
    }

}
