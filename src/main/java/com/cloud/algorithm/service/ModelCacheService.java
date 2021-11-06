package com.cloud.algorithm.service;

import com.cloud.algorithm.service.rediz.RedizService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/4 13:03
 */
@Service
@Slf4j
public class ModelCacheService implements KeyGenerant {

    @Autowired
    private RedizService redizServiceImp;


    /**
     * 删除模型的状态缓存
     *
     * @param modelid 模型id
     */
    public void deletModelStatus(Long modelid) {
        log.info("deletModelStatus.id={}",modelid);
        redizServiceImp.delete(generantAlgorithmStatusRedisKey(modelid));
    }


    /**
     * 更新模型的状态缓存
     *
     * @param modelid          模型id
     * @param modleStatusCache 模型缓存
     */
    public void updateModelStatus(Long modelid, Object modleStatusCache) {
        redizServiceImp.update(generantAlgorithmStatusRedisKey(modelid), modleStatusCache);
    }


    /**
     * 更新模型的状态缓存
     *
     * @param modelid 模型id
     */
    public Object getModelStatus(Long modelid) {
        return redizServiceImp.get(generantAlgorithmStatusRedisKey(modelid));
    }



}
