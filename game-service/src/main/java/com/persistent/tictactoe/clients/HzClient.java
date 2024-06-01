package com.persistent.tictactoe.clients;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;

import static io.vertx.core.http.impl.HttpClientConnection.log;

public class HzClient {

    private static HazelcastInstance hz = null;

    public static synchronized HazelcastInstance getHzClient(){
        if(hz == null){
            String hzServers = System.getenv().get("HZ_SERVERS");
            log.info("HZ_SERVERS : " +  hzServers);
            ClientConfig clientConfig = new ClientConfig();
            clientConfig.getNetworkConfig().addAddress(hzServers);
            hz = HazelcastClient.newHazelcastClient(clientConfig);
        }
        return hz;
    }


}
