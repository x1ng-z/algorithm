package com.cloud.algorithm.model.bean.controlmodel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import com.cloud.algorithm.model.BaseModelImp;
import com.cloud.algorithm.model.bean.modelproperty.BaseModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.context.request.async.DeferredResult;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/1/9 11:17
 */
@Slf4j
@Data
public class PidModel extends BaseModelImp {

    /**
     * memery
     */
    private String datasource;
    private Map<Integer, BaseModelProperty> indexproperties;//key=modleid
    private String pyproxyexecute;
    private String port;
    private String pidscript;

    private double backpartkp;
    private double backpartki;
    private double backpartkd;



}
