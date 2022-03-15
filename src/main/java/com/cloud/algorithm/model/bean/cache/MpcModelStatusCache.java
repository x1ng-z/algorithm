package com.cloud.algorithm.model.bean.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * @author zzx
 * @version 1.0
 * @date 2022/3/15 12:35
 * mpc类型的模型状态缓存
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MpcModelStatusCache extends BaseModleStatusCache {
    /**key=引脚的code，value=在置信区间内保持到的时间点，大于此，将引脚切入控制*/
    private Map<String, Instant> pinClock;
    /**是否本次参与控制 key=code */
    private Map<String,Boolean> pinActiveStatus;
    /**模型是否处于自动状态*/
    private Boolean modelAuto;
    /**模型构建状态*/
    private Boolean modelbuild;
}
