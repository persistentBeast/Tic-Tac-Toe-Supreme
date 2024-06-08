package com.persistent.tictactoe.verticles;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;
import com.hazelcast.internal.json.JsonObject;
import com.persistent.tictactoe.clients.HzClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;

public class HzListenerVerticle extends AbstractVerticle {

    @Override
    public void start() {

        vertx.eventBus().consumer("NEW_GAME", new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> event) {

                ITopic<JsonObject> topic = HzClient.getHzClient().getTopic("SERVER" + "_" + event.body());

                topic.addMessageListener(new MessageListener<>() {
                    @Override
                    public void onMessage(com.hazelcast.core.Message<JsonObject> message) {
                        io.vertx.core.json.JsonObject msg = new io.vertx.core.json.JsonObject(message.getMessageObject().toString());
                        vertx.eventBus().request(event.body(), msg);
                    }
                });

            }
        });

    }

}
