package com.cloud.algorithm.model.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/7 13:34
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CpythonRequestDto extends CallBaseRequestDto {
    private Long modelId;
    private String pythoncontext;
}
