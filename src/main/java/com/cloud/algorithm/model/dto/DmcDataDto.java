package com.cloud.algorithm.model.dto;

import com.alibaba.fastjson.JSONArray;
import com.cloud.algorithm.constant.AlgorithmMpcModelStep;
import lombok.Data;

import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/8 9:42
 */
@Data
public class DmcDataDto {
    private JSONArray  predict;
    private JSONArray e;
    private JSONArray funelupAnddown;
    private JSONArray dmv;
    private JSONArray dff;
    private JSONArray mv;
    private String step;
}
