package com.cloud.algorithm.constant;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/5 8:38
 */
@Getter
public enum AlgorithmMpcFunnelType {
    MPC_FUNNEL_FULL("fullfunnel", "全漏斗"),
    MPC_FUNNEL_UP("upfunnel", "上漏斗"),
    MPC_FUNNEL_DOWN("downfunnel", "下漏斗");


    public static Map<String,AlgorithmMpcFunnelType> getCodeMap(){

        return Arrays.stream(values()).collect(Collectors.toMap(t->t.getCode(),t->t,(o,n)->n));

    }

    AlgorithmMpcFunnelType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    private final String code;
    private final String name;
}
