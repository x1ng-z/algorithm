package com.cloud.algorithm.service;

import com.cloud.algorithm.model.BaseModelImp;
import com.cloud.algorithm.model.bean.cache.BaseModleStatusCache;
import com.cloud.algorithm.model.bean.modelproperty.BaseModelProperty;
import com.cloud.algorithm.model.bean.modelproperty.MpcModelProperty;
import com.cloud.algorithm.model.dto.BaseModelResponseDto;
import com.cloud.algorithm.model.dto.apcPlant.Adapter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 11:23
 */
public interface Handle {


    /**
     * 模块入参组装
     * */
    void inprocess(BaseModelImp baseModelImp);
    /**
     * 模块计算处理
     * */
    BaseModelResponseDto docomputeprocess(BaseModelImp baseModelImp, BaseModleStatusCache baseModleStatusCache);

    /**
     * 模块初始化处理
     * */
   default void init(){};

    /**
     * 模型销毁
     * */
    default void destory(){};


    /**模型运行
     * */
    public BaseModelResponseDto run(BaseModelImp modle,BaseModleStatusCache baseModleStatusCache);

    /**
     * model stop
     * @param modelId model id
     * */
     default public void stop(Long modelId){}


    BaseModelImp convertModel(Adapter adapter);



    String getCode();



    /**
     * 返回指定引脚名称和方向的引脚
     * @param pindir 方向
     * @param pinname 引脚名称
     * @param properties
     * */
    default BaseModelProperty selectModelProperyByPinname(String pinname, List<BaseModelProperty> properties, String pindir) {
        List<BaseModelProperty> propertyList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(properties)) {
            propertyList = properties.stream().filter(p -> (p.getModlePinName().equals(pinname) && p.getPindir().equals(pindir))).collect(Collectors.toList());
        }
        if (propertyList.size() != 0) {
            return propertyList.get(0);
        }
        return null;
    }


    /***
     * 获取没有使用的引脚索引
     * @param maxindex 最大引脚索引
     * @param pvpattern
     * @param usepinscope
     * */
    default List<Integer> getUnUserPinScope(Pattern pvpattern, List<MpcModelProperty> usepinscope, int maxindex) {
        List<Integer> usepvpinindex = new LinkedList<>();
        List<Integer> allpinindex = new LinkedList<>();
        for (MpcModelProperty usepin : usepinscope) {
            Matcher matcher = pvpattern.matcher(usepin.getModlePinName());
            if (matcher.find()) {
                usepvpinindex.add(Integer.parseInt(matcher.group(2)));
            }
        }

        for (int indexpv = 1; indexpv <= maxindex; indexpv++) {
            allpinindex.add(indexpv);
        }
        allpinindex.removeAll(usepvpinindex);
        return allpinindex;
    }

    default List<MpcModelProperty> getSpecialPinTypeByMpc(String pintype, List<MpcModelProperty> mpcModleProperties) {
        List<MpcModelProperty> specialpintypeproperties = new ArrayList<>();
        for (MpcModelProperty modleProperty : mpcModleProperties) {
            if (!StringUtils.isEmpty(modleProperty.getPintype())) {
                if (modleProperty.getPintype().equals(pintype)) {
                    specialpintypeproperties.add(modleProperty);
                }
            }

        }
        return specialpintypeproperties;
    }

}
