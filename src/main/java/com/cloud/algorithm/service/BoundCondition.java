package com.cloud.algorithm.service;

import com.cloud.algorithm.model.bean.cache.MpcModelStatusCache;
import com.cloud.algorithm.model.bean.modelproperty.BaseModelProperty;
import com.cloud.algorithm.model.bean.modelproperty.BoundModelPropery;
import com.cloud.algorithm.model.bean.modelproperty.MpcModelProperty;

import java.time.Instant;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/4 8:11
 */
public interface BoundCondition {
    /**
     * 判断引脚是否越界
     */
    boolean isBreakLimit(BoundModelPropery boundModelPropery);

    /**
     * 判断越界以后，比较计时器是否在规定时间内都保持正常，如果是将让他参与本次控制
     * @param code mv1 pv1。。。
     */
    boolean isclockAlarm(MpcModelStatusCache modleStatusCache, Long modelId, String code);

    /**
     * 清除计时器
     */
    void clearRunClock(MpcModelStatusCache modleStatusCache,Long modelId, String code);
}
