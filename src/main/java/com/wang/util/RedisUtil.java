package com.wang.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * JedisUtil(推荐存Byte数组，存Json字符串效率更慢)
 * @author Wang926454
 * @date 2018/9/4 15:45
 */
@Component
public class RedisUtil {

    /**
     * 静态注入JedisPool连接池
     * 本来是正常注入JedisUtil，可以在Controller和Service层使用，但是重写Shiro的CustomCache无法注入JedisUtil
     * 现在改为静态注入JedisPool连接池，JedisUtil直接调用静态方法即可
     * https://blog.csdn.net/W_Z_W_888/article/details/79979103
     */
    private static RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public static void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        RedisUtil.redisTemplate = redisTemplate;
    }

    public static RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    public static Long dbSize() {
        return redisTemplate.execute(RedisServerCommands::dbSize);
    }


    public static Set<String> scan(String keyPattern) {
        return (Set<String>) redisTemplate.execute((RedisCallback) redisConnection -> {
            Set<String> keySet = new HashSet<>();
            ScanOptions scanOptions = new ScanOptions.ScanOptionsBuilder().match(keyPattern).count(1000).build();
            Cursor<byte[]> cursor = redisConnection.scan(scanOptions);
            while (cursor.hasNext()) {
                keySet.add(new String(cursor.next()));
            }
            return keySet;
        });
    }

    /**
     * 获取redis键值-object
     * @param key
     * @return java.lang.Object
     * @author Wang926454
     * @date 2018/9/4 15:47
     */
    public static Object getObject(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 设置redis键值-object-expiretime
     * @param key
	 * @param value
	 * @param expiretime
     * @return java.lang.String
     * @author Wang926454
     * @date 2018/9/4 15:50
     */
    public static void setObject(String key, Object value, int expiretime) {
        redisTemplate.opsForValue().set(key, value, expiretime, TimeUnit.SECONDS);
    }


    /**
     * 删除key
     * @param key
     * @return java.lang.Long
     * @author Wang926454
     * @date 2018/9/4 15:50
     */
    public static void delKey(String key) {
        redisTemplate.delete(key);
    }

    /**
     * key是否存在
     * @param key
     * @return java.lang.Boolean
     * @author Wang926454
     * @date 2018/9/4 15:51
     */
    public static Boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }
}
