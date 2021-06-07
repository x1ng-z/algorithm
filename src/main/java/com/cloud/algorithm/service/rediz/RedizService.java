package com.cloud.algorithm.service.rediz;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 10:24
 */

public interface RedizService {

    RedisTemplate<String, Object> getRedisTemplate();

    void insert(String key,Object value);

    void batchInsert(Map<String,Object> keyAndValues);


    void delete(String key);

    void batchDelete(Set<String> keys);

    void fuzzyDelet(String pattern);


    void update(String key,Object value);

    void batchUpdate(Map<String,Object> keyAndValues);


    Object get(String key);

    List<Object> batchGet(Set<String> keys);

    List<Object> fuzzyGet(String pattern);

}
