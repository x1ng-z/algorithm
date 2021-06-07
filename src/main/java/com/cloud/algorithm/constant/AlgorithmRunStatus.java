package com.cloud.algorithm.constant;

import lombok.Getter;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/2 19:27
 */
@Getter
public enum AlgorithmRunStatus {

    STATUS_RUNNING(1, "正在运行中"),
    STATUS_COMPLETE(2, ""),
    STATUS_INITE(3, "初始状态"),
    STATUS_PYTHON_MODLE_BUILD_COMPLET(4, "python模型构建状态"),
    STATUS_JAVA_MODEL_BUILD_COMPLETE(5, "java模型构建完成状态"),
    STATUS_PYTHON_FAILD(6, "python运行报错"),
    STATUS_DISCONNECT(7, "断线");

    AlgorithmRunStatus(int code, String name) {
        this.code = code;
        this.name = name;
    }

    private int code;
    private String name;
}
