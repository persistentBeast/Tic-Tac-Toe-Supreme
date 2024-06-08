package com.persistent.tictactoe.clients;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

import static io.vertx.core.http.impl.HttpClientConnection.log;

public class JedisStandaloneClient {

    private static Jedis jedisCleint;

    private static String hostName = null;

    private static int port = 0;


    public synchronized static Jedis getJedisStandaloneClient() {

        if (jedisCleint == null) {
            loadProperties();
            jedisCleint = new Jedis(new HostAndPort(hostName, port));
        }
        return jedisCleint;
    }


    private static void loadProperties() {
        log.info("Reading config file...");
        // Load the cluster configuration
        hostName = System.getenv().get("MEMORYDB_STANDALONE_ENDPOINT_HOSTNAME");
        port = Integer.parseInt(System.getenv().get("MEMORYDB_STANDALONE_ENDPOINT_PORT"));
    }

}
