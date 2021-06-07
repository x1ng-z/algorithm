package com.cloud.algorithm.constant;

import lombok.Getter;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/2 19:44
 */
@Getter
public enum AlgorithmModelPropertyClazz {
    MODEL_PROPERTY_CLAZZ_BASE("basepropery","基本属性"),
    MODEL_PROPERTY_CLAZZ_MPC("mpcproperty","mpc属性")
    ;


    AlgorithmModelPropertyClazz(String code, String name) {
        this.code = code;
        this.name = name;
    }

    private String code;
    private String name;

}
