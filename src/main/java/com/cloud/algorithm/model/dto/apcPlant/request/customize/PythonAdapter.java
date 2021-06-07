package com.cloud.algorithm.model.dto.apcPlant.request.customize;

import com.alibaba.fastjson.JSONObject;
import com.cloud.algorithm.model.dto.apcPlant.Adapter;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/3/10 0:44
 */
@Data
public class PythonAdapter implements Adapter {
    private PythonBaseParam basemodelparam;
    private String pythoncontext;
    private Map<String,String> inputparam;//输出参数数据，key=参数名称,value=值
    private List<PythonOutParam> outputparam;

}
