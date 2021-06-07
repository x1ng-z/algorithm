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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/1/9 11:28
 */
@Slf4j
@Data
public class CustomizeModel extends BaseModelImp {
    public static Pattern scriptpattern = Pattern.compile("^(.*).py$");
    /**
     * memery
     */
    private String datasource;
    private Map<Integer, BaseModelProperty> indexproperties;//key=modleid
    private String pyproxyexecute;
    private String port;
    private String pythoncontext;


    public void toBeRealModle(String nettyport, String pyproxyexecute) {
        this.port = nettyport;
        this.pyproxyexecute = pyproxyexecute;
    }


//    public String noscripNametail() {
//        Matcher matcher = scriptpattern.matcher(customizepyname);
//        if (matcher.find()) {
//            return matcher.group(1);
//        }
//        return null;
//    }

}
