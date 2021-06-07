package com.cloud.algorithm.constant;

import lombok.Getter;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/2 19:36
 */
@Getter
public enum AlgorithmModelProperty {

    MODEL_PROPERTY_PV("pv","pv"),
    MODEL_PROPERTY_PVDOWN("pvdown","pv下限"),
    MODEL_PROPERTY_PVUP("pvup","pv上限"),
    MODEL_PROPERTY_SP("sp","sp"),

    MODEL_PROPERTY_MV("mv","mv"),
    MODEL_PROPERTY_MVFB("mvfb","mv反馈"),
    MODEL_PROPERTY_MVUP("mvup","mv上限"),
    MODEL_PROPERTY_MVDOWN("mvdown","mv下限"),

    MODEL_PROPERTY_FF("ff","前馈"),
    MODEL_PROPERTY_FFDOWN("ffdown","前馈下限"),
    MODEL_PROPERTY_FFUP("ffup","前馈上限"),

    MODEL_PROPERTY_MODELAUTO("auto","模型自动"),

    MODEL_PROPERTY_PVENABLE("pvenable","pv使能"),
    MODEL_PROPERTY_FFENABLE("ffenable","ff使能"),
    MODEL_PROPERTY_MVENABLE("mvenable","mv使能"),

    MODEL_PROPERTY_PID_KP("kp","pid p"),
    MODEL_PROPERTY_PID_KI("ki","pid i"),
    MODEL_PROPERTY_PID_KD("kd","pid d"),
    MODEL_PROPERTY_PID_KF("kf","pid 前馈增量系数")
    ;


    private String code;
    private String name;

    AlgorithmModelProperty(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
