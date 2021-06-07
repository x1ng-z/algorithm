package com.cloud.algorithm.model.dto;

import lombok.Data;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/4 10:45
 */
@Data
public class DmcDmvDataDto  implements java.io.Serializable{
    /**
     * dmv:{"inputpinname":mvi其中i为mv引脚序号,
     *  * outputpinname:pvi其中i为pv引脚序号,value:0.1}}
     * */
    private String inputpinname;

    private double value;
}
