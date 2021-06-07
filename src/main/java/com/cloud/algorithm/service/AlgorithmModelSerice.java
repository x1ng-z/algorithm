package com.cloud.algorithm.service;

import com.cloud.algorithm.model.BaseModelImp;
import com.cloud.algorithm.model.dto.BaseModelResponseDto;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 9:31
 */
public interface AlgorithmModelSerice {


    /**
     *算法运行
     * @param code 模型类型
     * @param modelId 模型id
     * @param model 实例化模型
     * */
    BaseModelResponseDto run(Long modelId,String code, BaseModelImp model);


    /**
     *stop algorithm
     * @param modelId model id
     * @param code model code
     * */
    BaseModelResponseDto stop(Long modelId,String code);
}
