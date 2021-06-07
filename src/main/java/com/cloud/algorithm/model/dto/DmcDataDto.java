package com.cloud.algorithm.model.dto;

import lombok.Data;

import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/4 10:40
 */
@Data
public class DmcDataDto  implements java.io.Serializable{
    private List<PinDataDto> mvData;
    private List<DmcPvPredictDataDto> pvpredict;
    private List<DmcDmvDataDto> dmv;
}
