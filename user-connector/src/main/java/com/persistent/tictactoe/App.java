package com.persistent.tictactoe;

import com.persistent.tictactoe.clients.HzClient;
import com.persistent.tictactoe.constants.Constants;
import com.persistent.tictactoe.verticles.HzListenerVerticle;
import com.persistent.tictactoe.verticles.PlayVerticle;
import io.vertx.core.*;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.UUID;

import static io.vertx.core.http.impl.HttpClientConnection.log;

/**
 * Hello world!
 */
public class App extends AbstractVerticle {

    @Override
    public void start() throws Exception {

        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        router.post("/api/v1/play").handler(ctx -> {


            JsonObject reqBody = new JsonObject(ctx.body().asString());

            String userName = reqBody.getString(Constants.USER_NAME);
            String userId = UUID.randomUUID().toString();
            JsonObject vertxReq = new JsonObject();
            vertxReq.put(Constants.USER_ID, userId);
            vertxReq.put(Constants.USER_NAME, userName);

            // This handler will be called for every request
            HttpServerResponse response = ctx.response();
            response.putHeader("content-type", "application/json");

            vertx.eventBus().request(Constants.VERTX_DESTINATION_ST_GAME, vertxReq, (res) -> {
                if (res.failed() || res.result().body().toString().equals("ERROR")) {
                    JsonObject apiResponse = new JsonObject();
                    apiResponse.put("status", "FAILED");
                    response.setStatusCode(200);
                    response.send(apiResponse.toString());
                } else if (res.succeeded() && res.result().body().toString().equals("SUCCESS")) {
                    vertx.eventBus().consumer(Constants.VERTX_ADDRESS_NEW_GAME + "_" + userId, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> event) {
                            JsonObject apiResponse = event.body().copy();
                            apiResponse.put("status", "SUCCESS");
                            response.setStatusCode(200);
                            response.send(apiResponse.toString());
                        }
                    });
                }
            });


        });

        router.route("/live/:game_id").handler((rctx) -> {
            String gameId = rctx.pathParam("game_id");
            rctx.request().toWebSocket().onComplete((res) -> {
                vertx.eventBus().consumer(gameId, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> event) {
                        res.writeTextMessage(event.body().toString());
                    }
                });
                String id = UUID.randomUUID().toString();
                log.info("WebSocket connected, id: " + id);
                webSocketHandler(res, id);
            },
            (err) -> {
                log.error("WebSocket failed", err);
            }
            );
        });

        vertx.createHttpServer().requestHandler(router).listen(8080);
    }

    private void webSocketHandler(ServerWebSocket ws, String id) {

        ws.textMessageHandler((msg) -> {
            JsonObject obj = new JsonObject(msg);
            com.hazelcast.internal.json.JsonObject clientMessage = new com.hazelcast.internal.json.JsonObject();
            clientMessage.add("type", obj.getString("type"));
            clientMessage.add("game_id", obj.getString("game_id"));
            clientMessage.add("user_id", obj.getString("user_id"));
            HzClient.getHzClient().getTopic("SERVER" + "_" + obj.getString("game_id")).publish(clientMessage);
        });

    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        log.info("Starting User Connector Service..........");
        log.info("Deploying vertx verticles.........");

        Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(new HzListenerVerticle(), new DeploymentOptions().setInstances(1).setThreadingModel(ThreadingModel.WORKER));
        vertx.deployVerticle(new PlayVerticle(), new DeploymentOptions().setInstances(1).setThreadingModel(ThreadingModel.WORKER));
        vertx.deployVerticle(new App());

        log.info("App started, all verticles deployed. Time taken : " + (System.currentTimeMillis() - start));
    }



}
