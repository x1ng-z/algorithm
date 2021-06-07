package com.cloud.algorithm.model.bean.modelproperty;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.time.Instant;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/1/8 16:54
 */

@Data
public class BaseModelProperty  implements java.io.Serializable{

    /**memory*/
    private Double value;

    /****db****/
    private int modlepinsId;
    private long refmodleId;
    private String modleOpcTag;
    private String modlePinName;
    private String opcTagName;//中文注释
    private JSONObject resource;
    private int pinEnable=1;
    private Instant updateTime;
    private String pindir;//引脚方向//in/out?
    private String modlepropertyclazz;//引脚类型 base/mpc?
    private double dmvHigh;
    private double deadZone;
    private double dmvLow;
    private String pintype;//pv mv
    /**由于计算环境的变化，有些不在置信区间内的引脚需要移除控制，
     * 改标识符是用于查看现在引脚是否参与控制
     * */
    private volatile boolean thisTimeParticipate=true;






}
