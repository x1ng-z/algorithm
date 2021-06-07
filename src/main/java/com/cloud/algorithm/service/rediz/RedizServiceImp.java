package com.cloud.algorithm.service.rediz;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 10:24
 */
@Slf4j
@Service
@Getter
public class RedizServiceImp implements RedizService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    @Override
    public void insert(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void batchInsert(Map<String, Object> keyAndValues) {
        redisTemplate.executePipelined(
                new SessionCallback<Object>() {
                    @Override
                    public Object execute(RedisOperations redisOperations) throws DataAccessException {
                        if (!CollectionUtils.isEmpty(keyAndValues)) {
                            keyAndValues.forEach((k, v) -> {
                                redisOperations.opsForValue().set(k, v);
                            });
                        }
                        return null;
                    }
                }
        );
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void batchDelete(Set<String> keys) {
        redisTemplate.delete(keys);
    }

    @Override
    public void fuzzyDelet(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (!CollectionUtils.isEmpty(keys)) {
            redisTemplate.delete(keys);
        }
    }

    @Override
    public void update(String key, Object value) {
        insert(key, value);
    }

    @Override
    public void batchUpdate(Map<String, Object> keyAndValues) {
        batchInsert(keyAndValues);
    }

    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public List<Object> batchGet(Set<String> keys) {
        return redisTemplate.opsForValue().multiGet(keys);
    }

    @Override
    public List<Object> fuzzyGet(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern + "*");
        return batchGet(keys);
    }
}
