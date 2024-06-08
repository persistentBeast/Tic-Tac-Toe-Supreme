package com.persistent.tictactoe.clients;

import redis.clients.jedis.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static io.vertx.core.http.impl.HttpClientConnection.log;

public class JedisClusterClient {

    private static JedisCluster jedisCluster = null;

    /**
     * The Amazon MemoryDB for Redis cluster host name.
     */
    private static String hostName = null;

    /**
     * The Amazon MemoryDB for Redis cluster port.
     */
    private static int port = 0;

    /**
     * The client timeout value (in seconds).
     */
    private static int clientTimeoutInSecs = 0;

    /**
     * The connection timeout value (in seconds).
     */
    private static int connectionTimeoutInSecs = 0;

    /**
     * The socket timeout value (in seconds).
     */
    private static int socketTimeoutInSecs = 0;

    /**
     * The blocking socket timeout value (in seconds).
     */
    private static int blockingSocketTimeoutInSecs = 0;

    /**
     * The flag that specifies if SSL should be used in the connection.
     */
    private static boolean useSSL = true;

    /**
     * The name that identifies this specific instance of the Jedis client.
     */
    private static String clientName = null;

    private static String userName = null;

    /**
     * The password corresponding to the username to connect to the Amazon MemoryDB for
     * Redis cluster. This is configured when setting up this username in the ACL.
     */
    private static String password = null;

    /**
     * The maximum retry attempts in case of errors.
     */
    private static int maxAttempts = 0;

    public synchronized static JedisCluster getJedisClusterClient() {

        if (jedisCluster == null) {
            try {
                loadProperties();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            log.info("Initializing Jedis client for Amazon MemoryDB for Redis...");
            JedisClientConfig jedisClientConfig = DefaultJedisClientConfig.builder()
                    .clientName(clientName)
                    .timeoutMillis(clientTimeoutInSecs * 1000).connectionTimeoutMillis(connectionTimeoutInSecs * 1000)
                    .blockingSocketTimeoutMillis(blockingSocketTimeoutInSecs * 1000)
                    .socketTimeoutMillis(socketTimeoutInSecs * 1000).build();
            Set<HostAndPort> hostAndPortSet = new HashSet<>();
            hostAndPortSet.add(new HostAndPort(hostName, port));
            jedisCluster = new JedisCluster(hostAndPortSet, jedisClientConfig, maxAttempts);
            log.info("Completed initializing Jedis client for Amazon MemoryDB for Redis.");
        }
        return jedisCluster;

    }


    private static void loadProperties() throws IOException {
        log.info("Reading config file...");

        // Load the cluster configuration
        hostName = System.getenv().get("MEMORYDB_CLUSTER_ENDPOINT_HOSTNAME");
        port = Integer.parseInt(System.getenv().get("MEMORYDB_CLUSTER_ENDPOINT_PORT"));
        userName = System.getenv().get("MEMORYDB_CLUSTER_ENDPOINT_USERNAME");
        password = System.getenv().get("MEMORYDB_CLUSTER_ENDPOINT_PASSWORD");

        // Load the client configuration
        clientName = System.getenv().get("MEMORYDB_CLIENT_NAME");
        useSSL = Boolean.parseBoolean(System.getenv().get("MEMORYDB_CLIENT_USE_SSL"));
        clientTimeoutInSecs = Integer.parseInt(System.getenv().get("MEMORYDB_CLIENT_TIMEOUT_IN_SECS"));
        connectionTimeoutInSecs = Integer.parseInt(System.getenv().get("MEMORYDB_CLIENT_CONNECTION_TIMEOUT_IN_SECS"));
        blockingSocketTimeoutInSecs = Integer.parseInt(System.getenv().get("MEMORYDB_CLIENT_BLOCKING_SOCKET_TIMEOUT_IN_SECS"));
        socketTimeoutInSecs = Integer.parseInt(System.getenv().get("MEMORYDB_CLIENT_SOCKET_TIMEOUT_IN_SECS"));
        maxAttempts = Integer.parseInt(System.getenv().get("MEMORYDB_CLIENT_MAX_ATTEMPTS"));

        log.info("Completed reading config file.");
    }

    public static void shutdown() {
        stopClient();
    }

    private static void stopClient() {
        log.info("Shutting down Jedis client for Amazon MemoryDB for Redis...");
        jedisCluster.close();
        log.info("Completed shutting down Jedis client for Amazon MemoryDB for Redis.");
    }

}
