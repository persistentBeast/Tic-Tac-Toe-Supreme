package com.persistent.tictactoe.verticles;

import com.persistent.tictactoe.clients.HzClient;
import com.persistent.tictactoe.dao.MemoryDbDao;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
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
        com.hazelcast.internal.json.JsonObject activityAckObject = new com.hazelcast.internal.json.JsonObject();
        String gameId = event.getString("game_id");
        String userId = event.getString("user_id");
        String moveId = event.getString("move_id");
        activityAckObject.add("type", "activity-ack");
        activityAckObject.add("move_id", moveId);
        activityAckObject.add("user_id", userId);
        activityAckObject.add("game_id", gameId);

        Map<String, String> gameState = memoryDbDao.getHashRecordAllFields("Game::" + gameId);
        String turnOfUserId = gameState.get("game_turn");

        List<Integer> gameBoard = new ArrayList<>(9);
        String gameBoardStateString = gameState.get("game_state");
        for (int i = 0; i < 9; i++) {
            if (gameBoardStateString.charAt(i) == '-') {
                gameBoard.add(i, -1);
            } else {
                gameBoard.add(i, Integer.valueOf(gameBoardStateString.substring(i, i + 1)));
            }
        }

        if (gameState.get("game_status").equals("LOADING")) {
            activityAckObject.add("status", "FAIL");
            activityAckObject.add("fail_reason", "Game is in loading state!");
        } else if (gameState.get("game_status").equals("COMPLETED")) {
            activityAckObject.add("status", "FAIL");
            activityAckObject.add("fail_reason", "Game is over!");
        } else if (!userId.equals(turnOfUserId)) {
            activityAckObject.add("status", "FAIL");
            activityAckObject.add("fail_reason", "Its not your turn!");
        } else {
            int move = event.getInteger("move");
            if (move < 0 || move > 8 || gameBoard.get(move) != -1) {
                activityAckObject.add("status", "FAIL");
                activityAckObject.add("fail_reason", "Invalid move!");
            } else {
                gameBoard.add(move, getPlayerSymbol(gameState, userId));
                StringBuilder newGameStateString = new StringBuilder(9);
                for (int i = 0; i < 9; i++) {
                    if (gameBoard.get(i) == -1) newGameStateString.append("-");
                    else {
                        newGameStateString.append(gameBoard.get(i));
                    }
                }
                gameState.put("game_turn", getOpponentPlayerId(gameState, userId));
                gameState.put("game_state", newGameStateString.toString());
                activityAckObject.add("status", "SUCCESS");
            }
        }
        HzClient.getHzClient().getTopic(gameId).publish(activityAckObject);
        updateAndPublishGameState(gameState, gameId);

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

        int winner = checkWinner(gameState.get("game_state"));
        if (winner != -1) {
            gameState.put("game_status", "COMPLETED");
            gameState.put("winner", String.valueOf(winner));
        }

        com.hazelcast.internal.json.JsonObject stateUpdateEvent = new com.hazelcast.internal.json.JsonObject();
        stateUpdateEvent.add("type", "state");
        stateUpdateEvent.add("game_id", gameId);
        stateUpdateEvent.add("state", gameState.get("game_state"));
        stateUpdateEvent.add("status", gameState.get("game_status"));

        if (gameState.get("game_status").equals("COMPLETED")) {
            stateUpdateEvent.add("winner", gameState.get("winner"));

        }

        if (gameState.get("game_status").equals("LOADING")) {
            stateUpdateEvent.add("loading_reason", String.valueOf("P1 : " + gameState.get("p1_status") +
                    " P2 : " + gameState.get("p2_status")));
        }

        memoryDbDao.upsertHashRecord2("Game::" + gameId, gameState);
        HzClient.getHzClient().getTopic(gameId).publish(stateUpdateEvent);
    }

    private int getPlayerSymbol(Map<String, String> gameState, String userId) {
        if (gameState.get("p1").equals(userId)) {
            return Integer.parseInt(gameState.get("p1_symbol"));
        } else if (gameState.get("p2").equals(userId)) {
            return Integer.parseInt(gameState.get("p2_symbol"));
        }
        throw new RuntimeException("Invalid condition occured!");
    }

    private String getOpponentPlayerId(Map<String, String> gameState, String userId) {
        if (gameState.get("p1").equals(userId)) {
            return gameState.get("p2");
        } else if (gameState.get("p2").equals(userId)) {
            return gameState.get("p1");
        }
        throw new RuntimeException("Invalid condition occured!");
    }

    // 0 or 1 : some player won, -1 : no one won game can proceed, 2 : game drawn, can't proceed
    private int checkWinner(String gameState) {
        List<Integer> gameBoard = new ArrayList<>(9);
        List<List<Integer>> matrix = new ArrayList<>(3);
        for (int i = 0; i < 9; i++) {
            if (gameState.charAt(i) == '-') {
                gameBoard.add(i, -1);
            } else {
                gameBoard.add(i, Integer.valueOf(gameState.substring(i, i + 1)));
            }
        }
        matrix.add(0, gameBoard.subList(0, 3));
        matrix.add(1, gameBoard.subList(3, 6));
        matrix.add(2, gameBoard.subList(6, 9));
        boolean cantProceed = true;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (matrix.get(i).get(j) == -1) cantProceed = false;
                else {
                    int num = matrix.get(i).get(j);
                    boolean hor = (isPosMatchingOnBoard(matrix, i, j - 1, num) && isPosMatchingOnBoard(matrix, i, j - 2, num)) ||
                            (isPosMatchingOnBoard(matrix, i, j + 1, num) && isPosMatchingOnBoard(matrix, i, j + 2, num));
                    boolean ver = (isPosMatchingOnBoard(matrix, i - 1, j, num) && isPosMatchingOnBoard(matrix, i - 2, j, num)) ||
                            (isPosMatchingOnBoard(matrix, i + 1, j, num) && isPosMatchingOnBoard(matrix, i + 2, j, num));
                    boolean diag = (isPosMatchingOnBoard(matrix, i - 1, j - 1, num) && isPosMatchingOnBoard(matrix, i - 2, j - 2, num)) ||
                            (isPosMatchingOnBoard(matrix, i + 1, j + 1, num) && isPosMatchingOnBoard(matrix, i + 2, j + 2, num)) ||
                            (isPosMatchingOnBoard(matrix, i - 1, j + 1, num) && isPosMatchingOnBoard(matrix, i - 2, j + 2, num)) ||
                            (isPosMatchingOnBoard(matrix, i + 1, j - 1, num) && isPosMatchingOnBoard(matrix, i + 2, j - 2, num));
                    if (hor || ver || diag) {
                        return num;
                    }
                }
            }
        }
        if (cantProceed) return 2;
        return -1;
    }

    private boolean isPosMatchingOnBoard(List<List<Integer>> matrix, int i, int j, int num) {
        int n = matrix.size(), m = matrix.getFirst().size();
        return (i >= 0 && i < n && j >= 0 && j < m) &&
                matrix.get(i).get(j).equals(num);
    }
}
