package com.cloud.algorithm.model.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 15:21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallBaseRequestDto  implements java.io.Serializable{
    private JSONObject input;
    private Object context;
}
