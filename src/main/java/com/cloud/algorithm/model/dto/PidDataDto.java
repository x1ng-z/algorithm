package com.cloud.algorithm.model.dto;

import lombok.Data;

import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 14:09
 */
@Data
public class PidDataDto  implements java.io.Serializable{
    private List<PinDataDto> mvData;
    private double partkp;
    private double partki;
    private double partkd;


}
