package com.cloud.algorithm.model.dto;

import lombok.Data;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 17:36
 */
@Data
public class PidPinValueDto extends PinDataDto  implements java.io.Serializable{
    private double partkp;
    private double partki;
    private double partkd;
}
