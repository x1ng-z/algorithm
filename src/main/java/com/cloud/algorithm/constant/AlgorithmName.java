package com.cloud.algorithm.constant;

import lombok.Data;
import lombok.Getter;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/2 19:18
 */
@Getter
public enum AlgorithmName {

    INPUT_MODEL("input","输入模块"),
    OUTPUT_MODEL("output","输出模块"),
    FILTE_RMODEL("filter","滤波模块"),
    CUSTOMIZE_MODEL("customize","自定义模块"),
    MPC_MODEL("mpc","mpc模块"),
    PID_MODEL("pid","pid模块");

    AlgorithmName(String code, String name) {
        this.code = code;
        this.name = name;
    }

    private String code;
    private String name;
}
