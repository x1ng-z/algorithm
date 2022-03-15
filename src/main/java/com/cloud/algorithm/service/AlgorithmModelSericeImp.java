package com.cloud.algorithm.service;

import com.cloud.algorithm.annotation.RedizDistributeLock;
import com.cloud.algorithm.model.BaseModelImp;
import com.cloud.algorithm.model.bean.cache.BaseModleStatusCache;
import com.cloud.algorithm.model.dto.BaseModelResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 9:33
 */
@Service
@Slf4j
public class AlgorithmModelSericeImp implements AlgorithmModelSerice {


    @Autowired
    private List<Handle> algorithmHandles;

    @Autowired
    private ModelCacheService modelCacheService;


    /**
     * 方便索引的hanle集合
     */
    private Map<String, Handle> indexHandles;


    @RedizDistributeLock
    @Override
    public BaseModelResponseDto run(Long modelId, String code, BaseModelImp model) {

        Handle handle = getMatchHandles(code);

        if (handle != null) {
            return handle.run(model, null/**切面中会进行注入*/);
        }
        return BaseModelResponseDto.builder().message("找不到匹配的算法").status(123456).build();
    }


    @RedizDistributeLock
    @Override
    public BaseModelResponseDto stop(Long modelId, String code) {
        Object modelStatus = modelCacheService.getModelStatus(modelId);
        try {
            if (!ObjectUtils.isEmpty(modelStatus)) {
                BaseModleStatusCache baseModleStatusCache = (BaseModleStatusCache) modelStatus;
                Handle handle = getMatchHandles(baseModleStatusCache.getCode());
                if (handle != null) {
                    handle.stop(modelId);
                    return BaseModelResponseDto.builder().message("停止成功").status(200).build();
                }
                return BaseModelResponseDto.builder().message("找不到匹配的算法").status(123456).build();
            }
        } finally {
            modelCacheService.deletModelStatus(modelId);
        }

        return BaseModelResponseDto.builder().message("停止时找不到模型缓存").status(123456).build();
    }


    public Handle getMatchHandles(String code) {
        synchronized (this) {
            if (CollectionUtils.isEmpty(indexHandles)) {

                indexHandles = new ConcurrentHashMap<>();

                if (!CollectionUtils.isEmpty(algorithmHandles)) {

                    algorithmHandles.forEach(h -> {
                        indexHandles.put(h.getCode(), h);
                    });
                }
            }
        }
        return indexHandles.get(code);
    }


}
