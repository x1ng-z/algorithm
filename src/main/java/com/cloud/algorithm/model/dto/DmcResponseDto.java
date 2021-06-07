package com.cloud.algorithm.model.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/4 10:40
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DmcResponseDto extends BaseModelResponseDto {
    private DmcDataDto data;

}
