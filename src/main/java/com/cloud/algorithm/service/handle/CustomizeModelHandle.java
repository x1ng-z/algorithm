package com.cloud.algorithm.service.handle;

import com.alibaba.fastjson.JSONObject;
import com.cloud.algorithm.config.AlgorithmRouteConfig;
import com.cloud.algorithm.constant.AlgorithmModelPropertyClazz;
import com.cloud.algorithm.constant.AlgorithmModelPropertyDir;
import com.cloud.algorithm.constant.AlgorithmName;
import com.cloud.algorithm.constant.AlgorithmValueFrom;
import com.cloud.algorithm.model.BaseModelImp;
import com.cloud.algorithm.model.bean.cache.BaseModleStatusCache;
import com.cloud.algorithm.model.bean.controlmodel.CustomizeModel;
import com.cloud.algorithm.model.bean.modelproperty.BaseModelProperty;
import com.cloud.algorithm.model.dto.*;
import com.cloud.algorithm.model.dto.apcPlant.Adapter;
import com.cloud.algorithm.model.dto.apcPlant.request.customize.PythonAdapter;
import com.cloud.algorithm.model.dto.apcPlant.request.customize.PythonOutParam;
import com.cloud.algorithm.model.dto.apcPlant.response.customize.CPythonDataDto;
import com.cloud.algorithm.model.dto.apcPlant.response.customize.CPythonResponse4PlantDto;
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
    private AlgorithmRouteConfig algorithmRouteConfig;

    @Autowired
    @Qualifier("nocloud")
    private RestTemplate restTemplateNoCloud;


    @Override
    public String getCode() {
        return AlgorithmName.CUSTOMIZE_MODEL.getCode();
    }

    @Override
    public BaseModelResponseDto run(BaseModelImp modle,BaseModleStatusCache baseModleStatusCache) {
        inprocess(modle);
        return docomputeprocess(modle, baseModleStatusCache);
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
    public BaseModelResponseDto docomputeprocess(BaseModelImp baseModelImp, BaseModleStatusCache baseModleStatusCache) {
        //数据组装
        CallBaseRequestDto callBaseRequestDto = buildRequest(baseModelImp, baseModleStatusCache.getAlgorithmContext());
        //请求
        return callCPython( baseModleStatusCache,baseModelImp, callBaseRequestDto);
    }


    /**
     * 将算法输出值幅值到输出引脚。
     */
    public void computresulteprocess(BaseModelImp baseModelImp, Map<String, PinValueDto> data) {
        if (!CollectionUtils.isEmpty(baseModelImp.getPropertyImpList())) {
            baseModelImp.getPropertyImpList().stream().filter(p -> {
                if (p.getPindir().equals(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_OUTPUT.getCode())) {
                    return true;
                } else {
                    return false;
                }
            }).forEach(p -> {
                if (!CollectionUtils.isEmpty(data)) {
                    data.forEach((k, v) -> {
                        if (p.getModlePinName().equals(k)) {
                            p.setValue(v.getValue());
                        }
                    });
                }
            });

        }
    }



    private CPythonResponse4PlantDto buildResponse(String msg, int status) {
        CPythonResponse4PlantDto cPythonResponseDto = new CPythonResponse4PlantDto();
        cPythonResponseDto.setMessage(msg);
        cPythonResponseDto.setStatus(status);
        return cPythonResponseDto;
    }

    private CPythonResponse4PlantDto buildResponse(CPythonResponseDto cPythonResponseDto) {
        CPythonResponse4PlantDto cPythonResponse4PlantDto = new CPythonResponse4PlantDto();
        cPythonResponse4PlantDto.setMessage(cPythonResponseDto.getMessage());
        cPythonResponse4PlantDto.setStatus(cPythonResponseDto.getStatus());
        cPythonResponse4PlantDto.setAlgorithmContext(cPythonResponseDto.getAlgorithmContext());
        List<PinDataDto> data = new ArrayList<>();
        if (!CollectionUtils.isEmpty(cPythonResponseDto.getData())) {
            cPythonResponseDto.getData().forEach((k, v) -> {
                PinDataDto pinDataDto = new PinDataDto();
                pinDataDto.setValue(v.getValue());
                pinDataDto.setPinname(k);
                data.add(pinDataDto);
            });
        }
        CPythonDataDto cPythonDataDto = new CPythonDataDto();
        cPythonDataDto.setMvData(data);
        cPythonResponse4PlantDto.setData(cPythonDataDto);
        return cPythonResponse4PlantDto;
    }

    /**
     * 组装请求dao
     * */
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

    private CPythonResponse4PlantDto callCPython(BaseModleStatusCache baseModleStatusCache,BaseModelImp baseModelImp, CallBaseRequestDto callBaseRequestDto) {

        String requestUrl = algorithmRouteConfig.getUrl() + algorithmRouteConfig.getPython();
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
        if (!ObjectUtils.isEmpty(baseModleStatusCache)) {
            baseModleStatusCache.setAlgorithmContext(cPythonResponseDto.getAlgorithmContext());
        }
        return buildResponse(cPythonResponseDto);

    }


}
