package com.persistent.tictactoe.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persistent.tictactoe.clients.JedisClusterClient;
import com.persistent.tictactoe.clients.JedisStandaloneClient;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.Jedis;
import java.util.Map;

public class MemoryDbDao {

    private JedisCluster jedisCluster = null;

    private Jedis jedisClient = JedisStandaloneClient.getJedisStandaloneClient();

    /**
     * Gets a record from MemoryDB based on the key.
     *
     * @param key the key
     *
     * @return the value for the specified key
     */
    public String getRecord(String key) {
        System.out.println("Retrieving record for key '" + key + "'...");
        Object value = jedisCluster.get(key);
        if (value == null) {
            System.out.println("No record found.");
            return null;
        } else {
            return value.toString();
        }
    }

    /**
     * Inserts or updates a JSON record to MemoryDB based on the key.
     *
     * @param key   the key
     * @param value the value
     *
     * @return the response from the set operation
     */
    public String upsertJSONRecord(String key, String value) {
        System.out.println("Upserting JSON record for key '" + key + "'...");
        return jedisCluster.jsonSet(key, value);
    }

    /**
     * Gets data from a JSON record in MemoryDB based on the key and optionally a
     * path.
     *
     * @param key  the key
     * @param path the JSON path
     *
     * @return the value for the specified key
     */
    public String getJSONRecord(String key, String path) {
        Object value = null;
        if (path == null) {
            System.out.println("Retrieving JSON record for key '" + key + "'...");
            value = jedisCluster.jsonGet(key);
        } else {
            System.out.println("Retrieving JSON record for key '" + key + "' and path '" + path + "'...");
            value = jedisCluster.jsonGet(key, new Path(path));
        }
        if (value == null) {
            System.out.println("No record found.");
            return null;
        } else {
            return value.toString();
        }
    }

    /**
     * Inserts or updates a Hash record to MemoryDB based on the key.
     *
     * @param key   the key
     * @param value the value
     *
     * @return the response from the set operation
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    public String upsertHashRecord(String key, String value) throws JsonMappingException, JsonProcessingException {
        System.out.println("Upserting Hash record for key '" + key + "'...");
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> map = mapper.readValue(value, new TypeReference<Map<String, String>>() {
        });
        return Long.toString(jedisCluster.hset(key, map));
    }

    public void upsertHashRecord2(String key, Map<String, String> value) {
        System.out.println("Upserting Hash record for key '" + key + "'...");
        jedisClient.hset(key, value);
    }

    /**
     * Gets data from a Hash record in MemoryDB based on the key and optionally a
     * field.
     *
     * @param key   the key
     * @param field the field
     *
     * @return the value for the specified key
     */
    public String getHashRecord(String key, String field) {
        Object value = null;
        if (field == null) {
            System.out.println("Retrieving Hash record for key '" + key + "'...");
            value = jedisCluster.hgetAll(key);
        } else {
            System.out.println("Retrieving Hash record for key '" + key + "' and field '" + field + "'...");
            value = jedisCluster.hget(key, field);
        }
        if (value == null) {
            System.out.println("No record found.");
            return null;
        } else {
            return value.toString();
        }
    }

    public Map<String, String> getHashRecordAllFields(String key) {
        Map<String, String> value = jedisClient.hgetAll(key);
        if (value == null) {
            System.out.println("No record found.");
            return null;
        } else {
            return value;
        }
    }

    /**
     * Deletes a record from MemoryDB based on the key.
     *
     * @param key the key
     *
     * @return the response from the delete operation
     */
    public String deleteRecord(String key) {
        System.out.println("Deleting record for key '" + key + "'...");
        if (jedisCluster.del(key) == 0) {
            return "No record found.";
        } else {
            return "Record for key '" + key + "' deleted.";
        }
    }

}
