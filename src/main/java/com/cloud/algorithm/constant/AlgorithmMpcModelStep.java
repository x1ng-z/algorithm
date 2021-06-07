package com.cloud.algorithm.constant;

import lombok.Data;
import lombok.Getter;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 13:02
 */
@Getter
public enum AlgorithmMpcModelStep {
    MPC_MODEL_STEP_BUILD("build", "构建阶段"),
    MPC_MODEL_STEP_COMPUTE("compute", "计算阶段");

    AlgorithmMpcModelStep(String step, String name) {
        this.step = step;
        this.name = name;
    }

    private String step;
    private String name;
}
