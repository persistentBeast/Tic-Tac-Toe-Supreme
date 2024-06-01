package com.persistent.tictactoe.verticles;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.hazelcast.internal.json.JsonObject;
import com.persistent.tictactoe.clients.HzClient;
import com.persistent.tictactoe.constants.Constants;
import io.vertx.core.AbstractVerticle;

import java.util.HashSet;
import java.util.Set;

public class HzListenerVerticle extends AbstractVerticle {

    Set<String> gameIdsSet = new HashSet<>();

    @Override
    public void start(){

        ITopic<JsonObject> topic = HzClient.getHzClient().getTopic(Constants.VERTX_ADDRESS_NEW_GAME);

        topic.addMessageListener(new MessageListener<JsonObject>() {
            @Override
            public void onMessage(Message<JsonObject> message) {
                io.vertx.core.json.JsonObject msg = new io.vertx.core.json.JsonObject(message.getMessageObject().toString());
                String user_id = msg.getString(Constants.USER_ID);
                String gameId = msg.getString("game_id");
                addHzTopicListenerOnGame(gameId);
                vertx.eventBus().request(Constants.VERTX_ADDRESS_NEW_GAME + "_" + user_id, msg);
            }
        });

    }

    private void addHzTopicListenerOnGame(String gameId){

        if(!gameIdsSet.contains(gameId)){
            ITopic<JsonObject> topic = HzClient.getHzClient().getTopic(gameId);
            topic.addMessageListener(new MessageListener<JsonObject>() {
                @Override
                public void onMessage(Message<JsonObject> message) {
                    io.vertx.core.json.JsonObject msg = new io.vertx.core.json.JsonObject(message.getMessageObject().asString());
                    vertx.eventBus().publish(gameId, msg);
                }
            });
            gameIdsSet.add(gameId);
        }

    }

}
