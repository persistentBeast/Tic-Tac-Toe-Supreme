package com.persistent.tictactoe.verticles;

import com.hazelcast.core.ITopic;
import com.hazelcast.internal.json.JsonObject;
import com.persistent.tictactoe.clients.DynamoDb;
import com.persistent.tictactoe.clients.HzClient;
import com.persistent.tictactoe.constants.Constants;
import com.persistent.tictactoe.dao.MemoryDbDao;
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
                    jsonObject1.add("user_name", p1.getUserName());
                    jsonObject1.add("game_id", gameId);
                    jsonObject1.add("opponent_name", p2.getUserName());
                    jsonObject1.add("symbol", 0);
                    jsonObject1.add("status", "SUCCESS");

                    JsonObject jsonObject2 = new JsonObject();
                    jsonObject2.add("user_id", p2.getUserId());
                    jsonObject2.add("user_name", p2.getUserName());
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

                //Post Process after matching
                deleteWaitingUsers(matchedUsers);
                createStateInMemoryDb(matchedGames, gameIds);
                attachHzConsumerForPlayerEvents(gameIds);
                publishNewGamesToPlayers(matchedGames, gameIds);

            }
        });

    }

    private void attachHzConsumerForPlayerEvents(List<String> gameIds) {
        for (String gameId : gameIds) {
            vertx.eventBus().publish("NEW_GAME", gameId);
        }
    }

    private void publishNewGamesToPlayers(Map<String, List<JsonObject>> matchedGames, List<String> gameIds) {
        ITopic<JsonObject> topic = HzClient.getHzClient().getTopic(Constants.VERTX_ADDRESS_NEW_GAME);
        for (String gameId : gameIds) {
            matchedGames.get(gameId).forEach(topic::publish);
        }
    }

    private void deleteWaitingUsers(List<String> matchedUsers) {

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

    }

    private void createStateInMemoryDb(Map<String, List<JsonObject>> matchedGames, List<String> gameIds) {

        MemoryDbDao memoryDbDao = new MemoryDbDao();

        for (String gameId : gameIds) {
            Map<String, String> gameStateMap = new HashMap<>();
            gameStateMap.put("p1", matchedGames.get(gameId).getFirst().getString("user_id", null));
            gameStateMap.put("p2", matchedGames.get(gameId).getLast().getString("user_id", null));
            gameStateMap.put("p1_name", matchedGames.get(gameId).getFirst().getString("user_name", null));
            gameStateMap.put("p2_name", matchedGames.get(gameId).getLast().getString("user_name", null));
            gameStateMap.put("p1_status", "DIS-CONNECTED");
            gameStateMap.put("p2_status", "DIS-CONNECTED");
            gameStateMap.put("p1_last_ack", String.valueOf(-1));
            gameStateMap.put("p2_last_ack", String.valueOf(-1));
            gameStateMap.put("p1_symbol", String.valueOf(matchedGames.get(gameId).getFirst().get("symbol")));
            gameStateMap.put("p2_symbol", String.valueOf(matchedGames.get(gameId).getLast().get("symbol")));
            gameStateMap.put("game_turn", matchedGames.get(gameId).getFirst().getString("user_id", null));
            gameStateMap.put("game_state", "---------");
            gameStateMap.put("game_status", "LOADING");
            gameStateMap.put("created_at", String.valueOf(System.currentTimeMillis()));
            gameStateMap.put("updated_at", String.valueOf(System.currentTimeMillis()));

            // add to memoryDb
            memoryDbDao.upsertHashRecord2("Game::" + gameId, gameStateMap);

        }

    }

}
