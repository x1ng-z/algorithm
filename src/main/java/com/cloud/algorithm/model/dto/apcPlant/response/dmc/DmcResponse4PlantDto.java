package com.cloud.algorithm.model.dto.apcPlant.response.dmc;

import com.cloud.algorithm.model.dto.BaseModelResponseDto;
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
public class DmcResponse4PlantDto extends BaseModelResponseDto {
    private DmcData4PlantDto data;

}
