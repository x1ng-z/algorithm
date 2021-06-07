package com.cloud.algorithm.service.handle;

import com.alibaba.fastjson.JSONObject;
import com.cloud.algorithm.config.AlgorithmRouteConfig;
import com.cloud.algorithm.constant.AlgorithmModelPropertyClazz;
import com.cloud.algorithm.constant.AlgorithmModelPropertyDir;
import com.cloud.algorithm.constant.AlgorithmName;
import com.cloud.algorithm.constant.AlgorithmValueFrom;
import com.cloud.algorithm.model.BaseModelImp;
import com.cloud.algorithm.model.bean.cache.ModleStatusCache;
import com.cloud.algorithm.model.bean.controlmodel.CustomizeModel;
import com.cloud.algorithm.model.bean.modelproperty.BaseModelProperty;
import com.cloud.algorithm.model.dto.*;
import com.cloud.algorithm.model.dto.apcPlant.Adapter;
import com.cloud.algorithm.model.dto.apcPlant.request.customize.PythonAdapter;
import com.cloud.algorithm.model.dto.apcPlant.request.customize.PythonOutParam;
import com.cloud.algorithm.service.Handle;
import com.cloud.algorithm.service.ModelCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 9:39
 */
@Service
@Slf4j
public class CustomizeModelHandle implements Handle {

    @Autowired
    private ModelCacheService modelCacheService;

    @Autowired
    private AlgorithmRouteConfig algorithmRouteConfig;

    @Autowired
    @Qualifier("nocloud")
    private RestTemplate restTemplateNoCloud;


    @Override
    public String getCode() {
        return AlgorithmName.CUSTOMIZE_MODEL.getCode();
    }

    @Override
    public BaseModelResponseDto run(BaseModelImp modle) {
        inprocess(modle);
        return docomputeprocess(modle);
    }

    @Override
    public void stop(Long modelId) {
        modelCacheService.deletModelStatus(modelId);
    }

    @Override
    public BaseModelImp convertModel(Adapter adapter) {
        PythonAdapter pythonAdapter = (PythonAdapter) adapter;
        CustomizeModel customizeModle = new CustomizeModel();

        customizeModle.setModleEnable(1);
        customizeModle.setModleName(pythonAdapter.getBasemodelparam().getModelname());
        customizeModle.setModletype(AlgorithmName.CUSTOMIZE_MODEL.getCode());
        customizeModle.setModleId(pythonAdapter.getBasemodelparam().getModelid());
        customizeModle.setPropertyImpList(new ArrayList<>());
        for (Map.Entry<String, String> pythonInputParam : pythonAdapter.getInputparam().entrySet()) {
            BaseModelProperty propertyImp = new BaseModelProperty();
            propertyImp.setRefmodleId(pythonAdapter.getBasemodelparam().getModelid());
            propertyImp.setModleOpcTag("");
            propertyImp.setModlePinName(pythonInputParam.getKey());
            propertyImp.setOpcTagName("");
            JSONObject resource = new JSONObject();
            resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
            resource.put("value", pythonInputParam.getValue());
            propertyImp.setResource(resource);
            propertyImp.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
            propertyImp.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_BASE.getCode());
            customizeModle.getPropertyImpList().add(propertyImp);
        }


        for (PythonOutParam pythonOutParam : pythonAdapter.getOutputparam()) {
            BaseModelProperty propertyImp = new BaseModelProperty();
            propertyImp.setRefmodleId(pythonAdapter.getBasemodelparam().getModelid());
            propertyImp.setModleOpcTag("");
            propertyImp.setModlePinName(pythonOutParam.getOutputpinname());
            propertyImp.setOpcTagName("");
            JSONObject resource = new JSONObject();
            resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_MEMORY.getCode());
            propertyImp.setResource(resource);
            propertyImp.setResource(resource);
            propertyImp.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_OUTPUT.getCode());
            propertyImp.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_BASE.getCode());
            customizeModle.getPropertyImpList().add(propertyImp);

        }
        //sript
        customizeModle.setPythoncontext(pythonAdapter.getPythoncontext());
        return customizeModle;
    }


//    /**
//     * 通过参数名称，更新模型引脚数据
//     * 更新引脚数据
//     */
//
//    public void updatemodlevalue(CUSTOMIZEModle customizeModle) {
//        for (Map.Entry<String, String> pythonInputParam : inputparam.entrySet()) {
//            BaseModlePropertyImp selectmodleProperyByPinname = Tool.selectmodleProperyByPinname(pythonInputParam.getKey(), customizeModle.getPropertyImpList(), BaseModlePropertyImp.PINDIRINPUT);
//            if (selectmodleProperyByPinname != null) {
//                JSONObject resource = new JSONObject();
//                resource.put("resource", ModleProperty.SOURCE_TYPE_CONSTANT);
//                resource.put("value", pythonInputParam.getValue());
//                selectmodleProperyByPinname.setResource(resource);
//            }
//        }
//    }


    @Override
    public void inprocess(BaseModelImp baseModelImp) {
        List<BaseModelProperty> baseModelPropertyList = baseModelImp.getPropertyImpList();
        if (!CollectionUtils.isEmpty(baseModelPropertyList)) {
            baseModelPropertyList.forEach(p -> {
                if (p.getPindir().equals(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode())) {
                    if (p.getResource().getString("resource").equals(AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode())) {
                        p.setValue(p.getResource().getDouble("value"));
                    }

                }
            });
        }
    }

    @Override
    public BaseModelResponseDto docomputeprocess(BaseModelImp baseModelImp) {

        /**获取上线文*/
        Object modleStatusCache = modelCacheService.getModelStatus(baseModelImp.getModleId());

        if (ObjectUtils.isEmpty(modleStatusCache)) {
            //初始化并更新模型缓存
            modleStatusCache = ModleStatusCache.builder()
                    .modleId(baseModelImp.getModleId())
                    .code(baseModelImp.getModletype())
                    .algorithmContext(new JSONObject())
                    .build();
            modelCacheService.updateModelStatus(baseModelImp.getModleId(), modleStatusCache);
        }
        //数据组装
        CallBaseRequestDto callBaseRequestDto = buildRequest(baseModelImp, ((ModleStatusCache) modleStatusCache).getAlgorithmContext());
        //请求
        return callCPython(baseModelImp, callBaseRequestDto);
    }


    /**
     * 将算法输出值幅值到输出引脚。
     */
    public void computresulteprocess(BaseModelImp baseModelImp, CPythonDataDto data) {
        if (!CollectionUtils.isEmpty(baseModelImp.getPropertyImpList())) {
            baseModelImp.getPropertyImpList().stream().filter(p -> {
                if (p.getPindir().equals(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_OUTPUT.getCode())) {
                    return true;
                } else {
                    return false;
                }
            }).forEach(p -> {
                if (!CollectionUtils.isEmpty(data.getMvData())) {
                    data.getMvData().forEach(mv -> {
                        if (p.getModlePinName().equals(mv.getPinname())) {
                            p.setValue(mv.getValue());
                        }
                    });
                }
            });

        }
    }


    @Override
    public void init() {

    }

    @Override
    public void destory() {

    }


    private CPythonResponseDto buildResponse(String msg, int status) {
        CPythonResponseDto cPythonResponseDto = new CPythonResponseDto();
        cPythonResponseDto.setMessage(msg);
        cPythonResponseDto.setStatus(status);
        return cPythonResponseDto;
    }

    private CallBaseRequestDto buildRequest(BaseModelImp baseModelImp, Object context) {

        JSONObject scriptinput = new JSONObject();

        if (!CollectionUtils.isEmpty(baseModelImp.getPropertyImpList())) {

            baseModelImp.getPropertyImpList().stream().filter(p -> {
                if (p.getPindir().equals(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode())) {
                    return true;
                } else {
                    return false;
                }
            }).forEach(p -> {
                JSONObject invalue = new JSONObject();
                invalue.put("value", p.getValue());
                scriptinput.put(p.getModlePinName(), invalue);
            });
        }

        CpythonRequestDto dto = new CpythonRequestDto();
        dto.setModelId(baseModelImp.getModleId());
        dto.setPythoncontext(((CustomizeModel) baseModelImp).getPythoncontext());
        dto.setContext(context == null ? new JSONObject() : context);
        dto.setInput(scriptinput);
        return dto;
    }

    private CPythonResponseDto callCPython(BaseModelImp baseModelImp, CallBaseRequestDto callBaseRequestDto) {

        String requestUrl = algorithmRouteConfig.getUrl() + algorithmRouteConfig.getPid();
        ResponseEntity<CPythonResponseDto> responseEntity = null;
        try {
            responseEntity = restTemplateNoCloud.postForEntity(requestUrl,
                    JSONObject.parseObject(JSONObject.toJSONString(callBaseRequestDto)), CPythonResponseDto.class);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return buildResponse(e.getMessage(), 123456);
        }

        if (ObjectUtils.isEmpty(responseEntity) || ObjectUtils.isEmpty(responseEntity.getBody())) {
            //接口失败
            return buildResponse("调用Cpython接口失败，无返回内容", 123456);
        }

        CPythonResponseDto cPythonResponseDto = responseEntity.getBody();

        computresulteprocess(baseModelImp, cPythonResponseDto.getData());

        //更新上下文
        Object modelStatus = modelCacheService.getModelStatus(baseModelImp.getModleId());
        if (!ObjectUtils.isEmpty(modelStatus)) {
            ModleStatusCache modleStatusCache = (ModleStatusCache) modelStatus;
            modleStatusCache.setAlgorithmContext(cPythonResponseDto.getAlgorithmContext());
            modelCacheService.updateModelStatus(baseModelImp.getModleId(), modleStatusCache);
        }

        return cPythonResponseDto;

    }


}
