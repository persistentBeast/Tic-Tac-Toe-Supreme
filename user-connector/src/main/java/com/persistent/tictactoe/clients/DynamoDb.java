package com.persistent.tictactoe.clients;

import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDb {

    private static DynamoDbEnhancedClient dynamoDbClient = null;

    public static synchronized DynamoDbEnhancedClient getDynamoDbClient(){
        if(dynamoDbClient == null){
            Region region = Region.AP_SOUTH_1;
            DynamoDbClient ddb = DynamoDbClient.builder()
                    .region(region)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(System.getenv().get("AWS_ACCESS_KEY_ID"),
                            System.getenv().get("AWS_SECRET_KEY"))))
                    .build();
            dynamoDbClient = DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(ddb)
                    .build();
        }
        return dynamoDbClient;
    }

}
