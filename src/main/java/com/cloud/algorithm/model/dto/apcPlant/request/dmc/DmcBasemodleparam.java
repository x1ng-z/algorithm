package com.cloud.algorithm.model.dto.apcPlant.request.dmc;

import lombok.Data;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/3/8 12:41
 */

@Data
public class DmcBasemodleparam {
    private String modelname;
    private String modeltype;
    private long modelid;
    private String predicttime_P;
    private String timeserise_N;
    private String controltime_M;
    private int runstyle;
    private double auto;
    private double controlapcoutcycle;

}
