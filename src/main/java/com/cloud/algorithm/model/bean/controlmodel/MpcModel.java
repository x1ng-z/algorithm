package com.cloud.algorithm.model.bean.controlmodel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.cloud.algorithm.model.BaseModelImp;
import com.cloud.algorithm.model.bean.ResponTimeSerise;
import com.cloud.algorithm.model.bean.modelproperty.MpcModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/1/8 16:50
 */
@Data
@Slf4j
public class MpcModel extends BaseModelImp {
    private Logger logger = LoggerFactory.getLogger(MpcModel.class);
    private static Pattern pvpattern = Pattern.compile("(^pv\\d+$)");
    private static Pattern ffpattern = Pattern.compile("(^ff\\d+$)");

    public static final Integer RUNSTYLEBYAUTO = 0;//运行方式0-自动分配模式 1-手动分配模式
    public static final Integer RUNSTYLEBYMANUL = 1;//1-手动分配模式

    /**
     * memery
     */
//    private volatile AtomicBoolean javabuildcomplet = new AtomicBoolean(false);//java控制模型是构建完成？
//    private volatile AtomicBoolean pythonbuildcomplet = new AtomicBoolean(false);//python的控制模型是否构建完成

    private String datasource;
    private Map<Integer, MpcModelProperty> indexproperties;//key=modleid
    private String pyproxyexecute;
    private String port;
    private String mpcscript;
    private String runmsg;//脚本运行的错误消息记录


    /**
     * 模型各个类型引脚数量
     */
    private int totalPv = 8;
    private int totalFf = 8;
    private int totalMv = 8;

    /**模型真实运行状态*/
    /**
     * apc反馈的y0的预测值
     */
    private double[][] backPVPrediction;//pv的预测曲线
    /**
     * apc反馈的pv的漏斗的上边界
     */
    private double[][] backPVFunelUp;//PV的漏斗上限
    /**
     * apc反馈的pv的漏斗的下边界
     */
    private double[][] backPVFunelDown;//PV的漏斗下限
    /**
     * apc反馈的dmv增量的预测值shape=(p,m)
     */
    private double[][] backDmvWrite;
    private double[] backrawDmv;
    private double[] backrawDff;
    /**
     * apc反馈的y0与yreal的error预测值
     */
    private double[] backPVPredictionError;//预测误差

    /**
     * 模型计算时候的前馈变换值dff shape=(p,num_ff)
     */
    private double[][] backDff;


//    private OpcServicConstainer opcServicConstainer;//opcserviceconstainer
//    private BaseConf baseConf;//控制器的基本配置，在数据库中所定义，进行注入

    /**
     * 执行apc算法的桥接器
     */
//    private ExecutePythonBridge executePythonBridge;


    /**
     * 原始多目标pv输出数量
     */
    private Integer baseoutpoints_p = 0;//输出个数量
    /**
     * 可运行的的pv引脚
     */
    private Integer numOfRunnablePVPins_pp = 0;

    /**
     * 原始前馈数量
     */
    private Integer basefeedforwardpoints_v = 0;
    /**
     * 对应可运行的pv引脚的可运行ff引脚数量
     */
    private Integer numOfRunnableFFpins_vv = 0;

    /**
     * 原始可控制输入数量
     */
    private Integer baseinputpoints_m = 0;
    /**
     * 对应可运行pv引脚所用的可运行mv引脚数量
     */
    private Integer numOfRunnableMVpins_mm = 0;

    private List<MpcModelProperty> categoryPVmodletag = new ArrayList<>();//已经分类号的PV引脚
    private List<MpcModelProperty> categorySPmodletag = new ArrayList<>();//已经分类号的SP引脚
    private List<MpcModelProperty> categoryMVmodletag = new ArrayList<>();//已经分类号的MV引脚
    private List<MpcModelProperty> categoryFFmodletag = new ArrayList<>();//已经分类号的FF引脚
//    private ModlePin autoEnbalePin = null;//dcs手自动切换引脚


    private double[][][] A_RunnabletimeseriseMatrix = null;//输入响应 shape=[pv][mv][resp_N]
    private double[][][] B_RunnabletimeseriseMatrix = null;//前馈响应 shape=[pv][ff][resp_N]
    private double[][] runnableintegrationInc_mv = null;//mv的积分增量
    private double[][] runnableintegrationInc_ff = null;//ff的积分增量
    private Double[] Q = null;//误差权重矩阵
    private Double[] R = null;//控制权矩阵、正则化
    private Double[] alpheTrajectoryCoefficients = null;//参考轨迹的柔化系数


    private String[] alpheTrajectoryCoefmethod = null;//参考轨迹的柔化系数方法
    private Double[] deadZones = null;//漏斗死区
    private Double[] funelinitvalues = null;//漏斗初始值
    private double[][] funneltype;


    /**
     * DB模型配置了mv1 mv2 mv3.....mvn
     * pv1  1
     * pv2  1
     * pv3
     * pv4
     * ...
     * pvn
     * <p>
     * 指示PV用了哪几个mv
     * pvusemv矩阵shape=(num_pv,num_mv)
     * 如：pv1用了mv1,pv2用了mv1
     * 如[[1,0]，
     * [0,1]]
     */
    private int[][] maskBaseMapPvUseMvMatrix = null;

    /**
     * 基本pv对mv的基本作用比例
     **/
    private float[][] maskBaseMapPvEffectMvMatrix = null;


    /**
     * 激活的pv引脚对应的mv
     */
    private int[][] maskMatrixRunnablePVUseMV = null;


    /**
     * 基本可运行的pv对mv的基本作用比例
     **/
    private float[][] maskMatrixRunnablePvEffectMv = null;


    /**
     * DB模型配置表示Pv使用了哪些ff
     * pvuseff矩阵shape=(num_pv,num_ff)
     * 如pv1用了ff1,pv2用了ff2
     * 如[[1,0],
     * [0,1]]
     */
    private int[][] maskBaseMapPvUseFfMatrix = null;
    /**
     * 激活的pv对应的ff
     */
    private int[][] maskMatrixRunnablePVUseFF = null;


    /**
     * 本次参与控制的FF引脚的标记矩阵，在内容为1的地方就说明改引脚被引用了
     */
    int[] maskisRunnableFFMatrix = null;
    /**
     * 本次参与控制的PV引脚的标记矩阵，在内容为1的地方就说明改引脚被引用了
     */
    int[] maskisRunnablePVMatrix = null;
    /**
     * 本次参与控制的MV引脚的标记矩阵，在内容为1的地方就说明改引脚被引用了
     */
    int[] maskisRunnableMVMatrix = null;


    //方便引脚索引key=pv1.mv2,sp1,ff1等 value=引脚类
    private Map<String, MpcModelProperty> stringmodlePinsMap = new HashMap<>();




    /****db****/
    private Integer predicttime_P;//预测时域
    private Integer controltime_M;//单一控制输入未来控制M步增量(控制域)
    private Integer timeserise_N;//响应序列长度
    private Integer controlAPCOutCycle;//控制周期
    private Integer runstyle = 0;//运行方式0-自动分配模式 1-手动分配模式(2022年3月16日之前)，目前这个属性改为误差需要需要归一化，就是除上误差绝对值的最大值
    /**************/
    private List<ResponTimeSerise> responTimeSeriseList;
}
