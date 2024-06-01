package com.persistent.tictactoe.verticles;

import com.hazelcast.core.ITopic;
import com.hazelcast.internal.json.JsonObject;
import com.persistent.tictactoe.clients.DynamoDb;
import com.persistent.tictactoe.clients.HzClient;
import com.persistent.tictactoe.constants.Constants;
import com.persistent.tictactoe.models.PlayingInterest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

import java.util.*;

import static io.vertx.core.http.impl.HttpClientConnection.log;

public class PlayerMatcherVerticle extends AbstractVerticle {


    @Override
    public void start() {

        vertx.setPeriodic(5000, new Handler<Long>() {
            @Override
            public void handle(Long event) {
                log.info("Matching Players........");
                TableSchema<PlayingInterest> tableSchema = TableSchema.fromBean(PlayingInterest.class);
                List<PlayingInterest> items = DynamoDb.getDynamoDbClient().table(Constants.DDB_TABLE_PLAYING_INTEREST, tableSchema)
                        .scan(ScanEnhancedRequest.builder().limit(10).build())
                        .items()
                        .stream().toList();

                Map<String, List<JsonObject>> matchedGames = new HashMap<>();
                List<String> matchedUsers = new ArrayList<>();
                List<String> gameIds = new ArrayList<>();
                int i = 0, n = items.size();

                while (i + 1 < n) {

                    PlayingInterest p1 = items.get(i), p2 = items.get(i + 1);
                    String gameId = UUID.randomUUID().toString();
                    List<JsonObject> games = new ArrayList<>();

                    JsonObject jsonObject1 = new JsonObject();
                    jsonObject1.add("user_id", p1.getUserId());
                    jsonObject1.add("game_id", gameId);
                    jsonObject1.add("opponent_name", p2.getUserName());
                    jsonObject1.add("symbol", 0);
                    jsonObject1.add("status", "SUCCESS");

                    JsonObject jsonObject2 = new JsonObject();
                    jsonObject2.add("user_id", p2.getUserId());
                    jsonObject2.add("game_id", gameId);
                    jsonObject2.add("opponent_name", p1.getUserName());
                    jsonObject2.add("symbol", 1);
                    jsonObject2.add("status", "SUCCESS");

                    matchedUsers.add(p1.getUserId());
                    matchedUsers.add(p2.getUserId());

                    gameIds.add(gameId);

                    games.add(jsonObject1);
                    games.add(jsonObject2);

                    matchedGames.put(gameId, games);

                    i += 2;

                }

                if (matchedGames.isEmpty()) {
                    log.info("No players matched");
                    return;
                } else {
                    log.info(matchedUsers.size() + " players matched");
                }

                List<WriteBatch> writeBatches = new ArrayList<>();
                DynamoDbTable<PlayingInterest> mappedTable = DynamoDb.getDynamoDbClient().table(Constants.DDB_TABLE_PLAYING_INTEREST,
                        TableSchema.fromBean(PlayingInterest.class));

                for (String userId : matchedUsers) {
                    writeBatches.add(WriteBatch.builder(PlayingInterest.class)
                            .addDeleteItem(Key.builder()
                                    .partitionValue(userId)
                                    .build())
                            .mappedTableResource(mappedTable)
                            .build());
                }
                BatchWriteItemEnhancedRequest batchWriteItemEnhancedRequest = BatchWriteItemEnhancedRequest.builder()
                        .writeBatches(writeBatches)
                        .build();
                BatchWriteResult res = DynamoDb.getDynamoDbClient().batchWriteItem(batchWriteItemEnhancedRequest);
                ITopic<JsonObject> topic = HzClient.getHzClient().getTopic(Constants.VERTX_ADDRESS_NEW_GAME);
                for (String gameId : gameIds) {
                    matchedGames.get(gameId).forEach(topic::publish);
                }

            }
        });

    }

}
