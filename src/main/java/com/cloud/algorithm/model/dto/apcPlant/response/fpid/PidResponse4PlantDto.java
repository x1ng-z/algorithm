package com.cloud.algorithm.model.dto.apcPlant.response.fpid;

import com.cloud.algorithm.model.dto.BaseModelResponseDto;
import lombok.AllArgsConstructor;
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
public class PidResponse4PlantDto extends BaseModelResponseDto {
    private PidData4PlantDto data;
}
