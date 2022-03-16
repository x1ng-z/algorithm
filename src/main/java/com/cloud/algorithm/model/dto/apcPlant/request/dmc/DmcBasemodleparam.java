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
    //设置运行方式，这里有原来的计算方式有最小误差和自动分配，改为是否归一化和不归一化，除不除上偏差绝对值的最大值
    private int runstyle;
    private double auto;
    private double controlapcoutcycle;

}
