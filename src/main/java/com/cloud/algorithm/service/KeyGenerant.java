package com.cloud.algorithm.service;

import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/5 14:37
 */
public interface KeyGenerant {
       String  ALGORITHM_STATUS = "algorithmstatus:";
       String ALGORITHM_LOCK="algorithmlock:";

    /**
     * status key的生成
     * */

    default String generantAlgorithmStatusRedisKey(Long modelid) {
        return ALGORITHM_STATUS + modelid;
    }

    /**
     * 多status key生成
     * */
    default Set<String> generantAlgorithmStatusRedisKeys(Set<Long> modelids) throws RuntimeException {
        if (!CollectionUtils.isEmpty(modelids)) {
            return modelids.stream().map(modelid -> {
                return ALGORITHM_STATUS + modelid;
            }).collect(Collectors.toSet());
        }
        throw new RuntimeException("set of modleids is null");
    }




    /**
     * lock key的生成
     * */

    default String generantAlgorithmLockRedisKey(Long modelid) {
        return ALGORITHM_LOCK + modelid;
    }

    /**
     * 多lock key生成
     * */
    default Set<String> generantAlgorithmLockRedisKeys(Set<Long> modelids) throws RuntimeException {
        if (!CollectionUtils.isEmpty(modelids)) {
            return modelids.stream().map(modelid -> {
                return ALGORITHM_LOCK + modelid;
            }).collect(Collectors.toSet());
        }
        throw new RuntimeException("set of modleids is null");
    }






}
