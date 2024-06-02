package com.persistent.tictactoe.verticles;

import com.persistent.tictactoe.clients.HzClient;
import com.persistent.tictactoe.dao.MemoryDbDao;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import java.util.Map;

import static io.vertx.core.http.impl.HttpClientConnection.log;

public class PlayerEventsListenerVerticle extends AbstractVerticle {

    MemoryDbDao memoryDbDao = new MemoryDbDao();

    @Override
    public void start() {

        vertx.eventBus().consumer("NEW_GAME", new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> event) {
                vertx.eventBus().consumer(event.body(), new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> event2) {
                        handleUserEvents(event2.body());
                    }
                });
                vertx.setPeriodic(2000, new Handler<Long>() {
                    @Override
                    public void handle(Long eventId) {
                        com.hazelcast.internal.json.JsonObject ackPacket = new com.hazelcast.internal.json.JsonObject();
                        ackPacket.add("type", "ack");
                        ackPacket.add("game_id", event.body());
                        HzClient.getHzClient().getTopic(event.body()).publish(ackPacket);
                    }
                });
            }
        });

    }

    private void handleUserEvents(JsonObject event) {
        String type = event.getString("type");

        switch (type) {
            case "ack": {
                handlePlayerAck(event);
                break;
            }
            case "activity": {
                handlePlayerActivity(event);
                break;
            }
            default: {
                log.error("Invalid packet type received from player, type : " + type);
            }
        }
    }

    private void handlePlayerActivity(JsonObject event) {

    }

    private void handlePlayerAck(JsonObject event) {
        String gameId = event.getString("game_id");
        String userId = event.getString("user_id");
        Map<String, String> gameState = memoryDbDao.getHashRecordAllFields("Game::" + gameId);
        if (gameState.get("p1").equals(userId)) {
            gameState.put("p1_status", "CONNECTED");
            gameState.put("p1_last_ack", String.valueOf(System.currentTimeMillis()));
        } else if (gameState.get("p2").equals(userId)) {
            gameState.put("p2_status", "CONNECTED");
            gameState.put("p2_last_ack", String.valueOf(System.currentTimeMillis()));
        } else {
            log.error("[FATAL] User not part of current game : " + userId);
        }

        updateAndPublishGameState(gameState, gameId);
    }

    private void updateAndPublishGameState(Map<String, String> gameState, String gameId) {
        if (gameState.get("p1_status").equals("CONNECTED") && gameState.get("p2_status").equals("CONNECTED")) {
            gameState.put("game_status", "LIVE");
            gameState.put("updated_at", String.valueOf(System.currentTimeMillis()));
        }

        com.hazelcast.internal.json.JsonObject stateUpdateEvent = new com.hazelcast.internal.json.JsonObject();
        stateUpdateEvent.add("type", "state");
        stateUpdateEvent.add("game_id", gameId);
        stateUpdateEvent.add("state", gameState.get("game_state"));
        stateUpdateEvent.add("status", gameState.get("game_status"));

        if (gameState.get("game_status").equals("LOADING")) {
            stateUpdateEvent.add("loading_reason", String.valueOf("P1 : " + gameState.get("p1_status") +
                    " P2 : " + gameState.get("p2_status")));
        }

        memoryDbDao.upsertHashRecord2("Game::" + gameId, gameState);
        HzClient.getHzClient().getTopic(gameId).publish(stateUpdateEvent);
    }
}
