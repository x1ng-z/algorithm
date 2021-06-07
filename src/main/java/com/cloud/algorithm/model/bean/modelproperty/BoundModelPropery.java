package com.cloud.algorithm.model.bean.modelproperty;

import lombok.Data;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/4 14:51
 */
@Data
public class BoundModelPropery extends BaseModelProperty{

    /***memory**/
    private BaseModelProperty feedBack;//反馈
    private BaseModelProperty dcsEnabePin;
    private BoundModelPropery upLmt;//高限
    private BoundModelPropery downLmt;//低限


}
