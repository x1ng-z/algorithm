package com.cloud.algorithm.model.dto.apcPlant.response.customize;

import com.cloud.algorithm.model.dto.BaseModelResponseDto;
import com.cloud.algorithm.model.dto.PinDataDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 18:19
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CPythonResponse4PlantDto extends BaseModelResponseDto {
    private CPythonDataDto data;

}
