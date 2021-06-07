package com.cloud.algorithm.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 18:19
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPythonResponseDto extends BaseModelResponseDto{
    private CPythonDataDto data;

}
