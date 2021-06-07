package com.cloud.algorithm.constant;

import lombok.Getter;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/2 19:51
 */
@Getter
public enum AlgorithmValueFrom {

    ALGORITHM_VALUE_FROM_CONSTANT("constant", "常量"),
    ALGORITHM_VALUE_FROM_MEMORY("memory", "内存"),
    ALGORITHM_VALUE_FROM_OPC("opc", "opc"),
    ALGORITHM_VALUE_FROM_MODEL("model", "模型");

    AlgorithmValueFrom(String code, String name) {
        this.code = code;
        this.name = name;
    }

    private String code;
    private String name;

}
