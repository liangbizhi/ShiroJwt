package com.wang.config.shiro.cache;

import com.wang.model.common.Constant;
import com.wang.util.*;
import com.wang.util.common.PropertiesUtil;
import org.apache.shiro.cache.*;

import java.util.*;

/**
 * 重写Shiro的Cache保存读取
 * @author Wang926454
 * @date 2018/9/4 17:31
 */
public class CustomCache<K,V> implements Cache<K,V> {

    /**
     * 缓存的key名称获取为shiro:cache:account
     * @param key
     * @return java.lang.String
     * @author Wang926454
     * @date 2018/9/4 18:33
     */
    private String getKey(Object key){
        return Constant.PREFIX_SHIRO_CACHE + JwtUtil.getClaim(key.toString(), Constant.ACCOUNT);
    }

    /**
     * 获取缓存
     */
    @Override
    public Object get(Object key) throws CacheException {
        if(!RedisUtil.exists(this.getKey(key))){
            return null;
        }
        return RedisUtil.getObject(this.getKey(key));
    }

    /**
     * 保存缓存
     */
    @Override
    public Object put(Object key, Object value) throws CacheException {
        // 读取配置文件，获取Redis的Shiro缓存过期时间
        PropertiesUtil.readProperties("config.properties");
        String shiroCacheExpireTime = PropertiesUtil.getProperty("shiroCacheExpireTime");
        // 设置Redis的Shiro缓存
        RedisUtil.setObject(this.getKey(key), value, Integer.parseInt(shiroCacheExpireTime));
        return null;
    }

    /**
     * 移除缓存
     */
    @Override
    public Object remove(Object key) throws CacheException {
        if(!RedisUtil.exists(this.getKey(key))){
            return null;
        }
        RedisUtil.delKey(this.getKey(key));
        return null;
    }

    @Override
    public void clear() throws CacheException {
    }

    /**
     * 缓存的个数
     */
    @Override
    public int size() {
        return RedisUtil.dbSize().intValue();
    }

    /**
     * 获取所有的key
     */
    @Override
    public Set keys() {
        return RedisUtil.scan(Constant.PREFIX_SHIRO_CACHE + "*");
    }

    /**
     * 获取所有的value
     */
    @Override
    public Collection values() {
        Set keys = this.keys();
        List<Object> values = new ArrayList<Object>();
        for (Object key : keys) {
            values.add(RedisUtil.getObject(this.getKey(key)));
        }
        return values;
    }
}
