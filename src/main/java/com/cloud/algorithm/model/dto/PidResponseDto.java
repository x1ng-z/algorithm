package com.cloud.algorithm.model.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 13:47
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PidResponseDto extends BaseModelResponseDto{
    private PidDataDto data;
}
