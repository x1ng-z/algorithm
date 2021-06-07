package com.cloud.algorithm.model.dto.apcPlant.request.fpid;

import lombok.Data;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/3/9 9:19
 */
@Data
public class PidInputproperty {
    private double auto;
    private double kp;
    private double ki;
    private double kd;
    private double pv;
    private double sp;
    private double mv;
    private Double ff;
    private double kf;
    private double deadZone;
    private double dmvHigh;
    private double dmvLow;
    private double mvuppinvalue;
    private double mvdownpinvalue;
}
