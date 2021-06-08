package com.cloud.algorithm.model.dto.apcPlant.response.dmc;

import lombok.Data;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/4 10:41
 */
@Data
public class DmcPvPredictData4PlantDto implements java.io.Serializable {
    /**
     * {jsonarray,[里面的内容为pvpinname:pvi其中i引脚名称后面的序号,
     *      *  *  * predictorder:[后续N步pv的预测值，用于绘制预测曲线]],[]..}
     * */
    private String pvpinname;
    private double[] predictorder;
    private double[] upfunnel;
    private double[] downfunnel;
    private double e;
}
