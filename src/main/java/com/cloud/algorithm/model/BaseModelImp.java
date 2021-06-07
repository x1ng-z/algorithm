package com.cloud.algorithm.model;

import com.cloud.algorithm.constant.AlgorithmRunStatus;
import com.cloud.algorithm.model.bean.modelproperty.BaseModelProperty;
import lombok.Data;
import org.springframework.web.context.request.async.DeferredResult;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/1/8 16:46
 */
@Data
public abstract class BaseModelImp  implements java.io.Serializable{

    /**memory*/
    private volatile AlgorithmRunStatus modlerunlevel= AlgorithmRunStatus.STATUS_INITE;
    private String errormsg="";
    private long errortimestamp;
    private int autovalue=1;
    private Instant beginruntime;//模型开始运行时间，用于重置模型运行状态
    private Instant activetime;//用于判断模型是否已经离线

    private LinkedBlockingQueue<DeferredResult<String>> syneventLinkedBlockingQueue = new LinkedBlockingQueue();
    private  AtomicBoolean cancelrun = new AtomicBoolean(false);
    private AtomicInteger busyNum=new AtomicInteger(0);//请求繁忙次数
    private static final int MAX_BUSY_NUM=3;//最大繁忙次数

    /***db***/
    private long modleId;//模型id主键
    private String modleName;//模型名称
    private int modleEnable=1;//模块使能，用于设置算法是否运行，算法是否运行
    private String modletype;
    private int refprojectid;
    /*****/

    private List<BaseModelProperty> propertyImpList;


}
