package com.persistent.tictactoe.verticles;

import com.persistent.tictactoe.clients.DynamoDb;
import com.persistent.tictactoe.constants.Constants;
import com.persistent.tictactoe.models.PlayingInterest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class PlayVerticle extends AbstractVerticle {

    @Override
    public void start() {

        vertx.eventBus().<JsonObject>consumer(Constants.VERTX_DESTINATION_ST_GAME, msg -> {
            String userId = msg.body().getString(Constants.USER_ID);
            String userName = msg.body().getString(Constants.USER_NAME);

            try {
                DynamoDbEnhancedClient enhancedClient = DynamoDb.getDynamoDbClient();

                DynamoDbTable<PlayingInterest> mappedTable = enhancedClient.table(Constants.DDB_TABLE_PLAYING_INTEREST,
                        TableSchema.fromBean(PlayingInterest.class));

                Date now = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Ensure UTC timezone

                PlayingInterest playingInterest = new PlayingInterest(userId, Constants.DDB_TABLE_PLAYING_INTEREST_STATUS_W,
                        dateFormat.format(now), dateFormat.format(now), userName);

                PutItemEnhancedRequest<PlayingInterest> enReq = PutItemEnhancedRequest.builder(PlayingInterest.class)
                        .item(playingInterest)
                        .build();

                mappedTable.putItem(enReq);
                msg.reply("SUCCESS");
            } catch (Exception e) {
                System.err.println(e.getMessage());
                msg.reply("ERROR");
            }

        });


    }

}
