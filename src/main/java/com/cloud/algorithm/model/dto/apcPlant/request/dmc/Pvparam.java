package com.cloud.algorithm.model.dto.apcPlant.request.dmc;

import lombok.Data;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/3/8 14:08
 */
@Data
public class Pvparam {
    private String pvpinname;
    private double pvpinvalue;
    private double deadzone;
    private double funelinitvalue;
    private String funneltype;
    private double q;
    private double refertrajectorycoef;

    private Double pvuppinvalue;
    private Double pvdownpinvalue;
    private double sppinvalue;
    private String tracoefmethod;

}
