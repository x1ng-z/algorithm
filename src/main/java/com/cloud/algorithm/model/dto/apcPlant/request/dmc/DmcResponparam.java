package com.cloud.algorithm.model.dto.apcPlant.request.dmc;

import lombok.Data;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/3/8 14:13
 */
@Data
public class DmcResponparam {
    private String inputpinname;
    private String outputpinname;
    private Double k;
    private Double t;
    private Double tau;
    private Double ki;

}
