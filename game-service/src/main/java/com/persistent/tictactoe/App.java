package com.persistent.tictactoe;

import com.persistent.tictactoe.verticles.PlayerMatcherVerticle;
import io.vertx.core.*;


import static io.vertx.core.http.impl.HttpClientConnection.log;



public class App {


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        log.info("Starting Game Service..........");
        log.info("Deploying vertx verticles.........");
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new PlayerMatcherVerticle());
        log.info("App started, all verticles deployed. Time taken : " + (System.currentTimeMillis() - start));
    }
}
