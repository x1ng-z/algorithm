package com.cloud.algorithm.model.bean.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 12:59
 * 运算算法模型缓存
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BaseModleStatusCache implements java.io.Serializable {
    /**模型id*/
    private Long modleId;
    /**模型类型  AlgorithmName枚举code值*/
    private String code;
    /**模型上下文*/
    private Object algorithmContext;
}
