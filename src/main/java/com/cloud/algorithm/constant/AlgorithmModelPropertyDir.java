package com.cloud.algorithm.constant;

import lombok.Getter;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/2 19:44
 */
@Getter
public enum AlgorithmModelPropertyDir {
    MODEL_PROPERTYDIR_INPUT("input","输入方向"),
    MODEL_PROPERTYDIR_OUTPUT("output","输出方向")
    ;


    AlgorithmModelPropertyDir(String code, String name) {
        this.code = code;
        this.name = name;
    }

    private String code;
    private String name;

}
