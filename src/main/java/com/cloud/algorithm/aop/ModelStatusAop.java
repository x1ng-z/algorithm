package com.cloud.algorithm.aop;

import com.alibaba.fastjson.JSONObject;
import com.cloud.algorithm.constant.AlgorithmName;
import com.cloud.algorithm.model.BaseModelImp;
import com.cloud.algorithm.model.bean.cache.BaseModleStatusCache;
import com.cloud.algorithm.model.bean.cache.MpcModelStatusCache;
import com.cloud.algorithm.model.bean.modelproperty.BaseModelProperty;
import com.cloud.algorithm.service.ModelCacheService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zzx
 * @version 1.0
 * @date 2022/3/15 9:21
 * 用于更新redis中的算法状态的切片
 */
@Component
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class ModelStatusAop {


    /**
     * 算法的handler切片定义
     */
    @Pointcut("execution(public * com.cloud.algorithm.service.Handle+.run(..)) && args(baseModelImp,..)")
    public void dealModelStatusPointcut(BaseModelImp baseModelImp) {
    }


    /**
     * 切点around，再对应算法的handler处理之前将算法运行缓存获取出来，然后代替传入，最后算法运行完成后将状态更新
     *
     * @param baseModelImp
     */
    @Around(value = "dealModelStatusPointcut(baseModelImp)", argNames = "pjp,baseModelImp")
    public Object aroudHandlerRunMethod(ProceedingJoinPoint pjp, BaseModelImp baseModelImp) throws Throwable {
        //先将模型缓存获取出来
        //第一步先将算法运行状态获取到
        Object modleStatusCache = modelCacheService.getModelStatus(baseModelImp.getModleId());
        //模型缓存为空，则创建一个缓存
        if (ObjectUtils.isEmpty(modleStatusCache)) {
            //是否未mpc
            if (AlgorithmName.MPC_MODEL.getCode().equals(baseModelImp.getModletype())) {
                //初始化引脚参与激活状态 key=pinname mv1 pv1 value=boolean 是否参与本次控制
                Map<String, Boolean> pinActiveStatus = new HashMap<>();
                if (!CollectionUtils.isEmpty(baseModelImp.getPropertyImpList())) {
                    pinActiveStatus = baseModelImp.getPropertyImpList().stream().collect(Collectors.toMap(BaseModelProperty::getModlePinName, BaseModelProperty::isThisTimeParticipate, (oldvalue, newvalue) -> newvalue));
                }
                //初始化模型状态
                MpcModelStatusCache mpcModleStatusCache = new MpcModelStatusCache();

                modleStatusCache = mpcModleStatusCache;
                mpcModleStatusCache.setModleId(baseModelImp.getModleId());
                mpcModleStatusCache.setCode(baseModelImp.getModletype());
                mpcModleStatusCache.setAlgorithmContext(new JSONObject());
                mpcModleStatusCache.setPinActiveStatus(pinActiveStatus);
                mpcModleStatusCache.setPinClock(new HashMap<>());
                mpcModleStatusCache.setModelAuto(true);
                mpcModleStatusCache.setModelbuild(false);
            } else {
                //非mpc一般的算法
                modleStatusCache = BaseModleStatusCache.builder()
                        .modleId(baseModelImp.getModleId())
                        .code(baseModelImp.getModletype())
                        .algorithmContext(new JSONObject())
                        .build();
            }
        }
        //算法调用
        Object reValue = pjp.proceed(new Object[]{baseModelImp,modleStatusCache});
        //更新模型状态
        modelCacheService.updateModelStatus(baseModelImp.getModleId(), modleStatusCache);
        return reValue;
    }


    public ModelStatusAop(ModelCacheService modelCacheService) {
        this.modelCacheService = modelCacheService;
    }

    private ModelCacheService modelCacheService;
}
