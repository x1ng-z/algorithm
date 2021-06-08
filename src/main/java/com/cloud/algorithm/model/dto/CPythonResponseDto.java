package com.cloud.algorithm.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 18:19
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPythonResponseDto extends BaseModelResponseDto{
    private Map<String,PinValueDto> data;

}
