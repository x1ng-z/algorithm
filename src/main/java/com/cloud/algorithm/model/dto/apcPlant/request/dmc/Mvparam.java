package com.cloud.algorithm.model.dto.apcPlant.request.dmc;

import lombok.Data;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/3/8 14:11
 */
@Data
public class Mvparam {
    private String mvpinname;
    private double mvpinvalue;
    private double r;
    private double dmvhigh;
    private double dmvlow;
    private double mvuppinvalue;
    private double mvdownpinvalue;
    private double mvfbpinvalue;

}
