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
 * @date 2021/6/3 12:59
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModleStatusCache implements java.io.Serializable {
    /**模型id*/
    private Long modleId;
    /**模型类型*/
    private String code;
    /**key=引脚的code，value=在置信区间内保持到的时间点，大于此，将引脚切入控制*/
    private Map<String,Instant> pinClock;
    /**是否本次参与控制 key=code */
    private Map<String,Boolean> pinActiveStatus;
    /**模型是否处于自动状态*/
    private Boolean modelAuto;
    /**模型构建状态*/
    private Boolean modelbuild;
    /**模型上下文*/
    private Object algorithmContext;
}
