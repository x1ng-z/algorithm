package com.cloud.algorithm.model.bean.modelproperty;



import com.cloud.algorithm.constant.AlgorithmMpcFunnelType;
import lombok.Data;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/1/8 16:54
 */
@Data
public class MpcModelProperty extends BoundModelPropery {
    private final Pattern pvenablepattern = Pattern.compile("(^pvenable\\d+$)");




    /**运行时钟，用于判断引脚越界后是否进行run*/
//    private Instant runClock;



    /******db*****/
    private double Q;
    private double funelinitValue;
    private double R;
    private double referTrajectoryCoef;
    private AlgorithmMpcFunnelType funneltype;

    private String tracoefmethod;
    /*************/










}
