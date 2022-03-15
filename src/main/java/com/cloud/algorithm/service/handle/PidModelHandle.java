package com.cloud.algorithm.service.handle;

import com.alibaba.fastjson.JSONObject;
import com.cloud.algorithm.config.AlgorithmRouteConfig;
import com.cloud.algorithm.constant.*;
import com.cloud.algorithm.model.BaseModelImp;
import com.cloud.algorithm.model.bean.cache.BaseModleStatusCache;
import com.cloud.algorithm.model.bean.controlmodel.PidModel;
import com.cloud.algorithm.model.bean.modelproperty.BaseModelProperty;
import com.cloud.algorithm.model.bean.modelproperty.MpcModelProperty;
import com.cloud.algorithm.model.dto.*;
import com.cloud.algorithm.model.dto.apcPlant.Adapter;
import com.cloud.algorithm.model.dto.apcPlant.request.fpid.PidModleAdapter;
import com.cloud.algorithm.model.dto.apcPlant.request.fpid.PidOutPutproperty;
import com.cloud.algorithm.model.dto.apcPlant.response.fpid.PidData4PlantDto;
import com.cloud.algorithm.model.dto.apcPlant.response.fpid.PidResponse4PlantDto;
import com.cloud.algorithm.service.Handle;
import com.cloud.algorithm.service.ModelCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 9:39
 */
@Service
@Slf4j
public class PidModelHandle implements Handle {

    @Autowired
    private AlgorithmRouteConfig algorithmRouteConfig;

    @Autowired
    @Qualifier("nocloud")
    private RestTemplate restTemplateNoCloud;


    @Override
    public String getCode() {
        return AlgorithmName.PID_MODEL.getCode();
    }

    @Override
    public void inprocess(BaseModelImp baseModelImp) {
        List<BaseModelProperty> baseModelPropertyList = baseModelImp.getPropertyImpList();
        if (!CollectionUtils.isEmpty(baseModelPropertyList)) {
            baseModelPropertyList.stream().forEach(p -> {
                if (p.getResource().getString("resource").equals(AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode())) {
                    p.setValue(p.getResource().getDouble("value"));
                }
            });
        }
    }

    @Override
    public BaseModelResponseDto docomputeprocess(BaseModelImp baseModelImp, BaseModleStatusCache baseModleStatusCache) {
        BaseModelProperty autopin = selectModelProperyByPinname(AlgorithmModelProperty.MODEL_PROPERTY_MODELAUTO.getCode(), baseModelImp.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
        if (autopin != null) {
            if (autopin.getValue() == 0) {
                //把输入的mv直接丢给输出mv
                return modleshortcircuit(baseModelImp);
            }
        }

        //数据组装
        CallBaseRequestDto callBaseRequestDto = buildRequest(baseModelImp, baseModleStatusCache.getAlgorithmContext());
        //请求input
        //将请求结果返回给apc平台
        return callPid(baseModleStatusCache,baseModelImp, callBaseRequestDto);
    }


    /**
     * 更新模型输出引脚的mv值
     *
     * @param data
     * @param baseModelImp
     */
    private void computresulteprocess(BaseModelImp baseModelImp, PidDataDto data) {
        if (!CollectionUtils.isEmpty(baseModelImp.getPropertyImpList())) {
            baseModelImp.getPropertyImpList().stream().filter(p -> {
                if (p.getPindir().equals(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_OUTPUT.getCode())) {
                    return true;
                } else {
                    return false;
                }
            }).forEach(p -> {

                if (!ObjectUtils.isEmpty(data.getMv())) {
                    if (data.getMv().getPinname().equals(p.getModlePinName())) {
                        p.setValue(data.getMv().getValue());
                    }
                }

            });
        }

    }



    @Override
    public BaseModelResponseDto run(BaseModelImp modle,BaseModleStatusCache baseModleStatusCache) {
        inprocess(modle);
        return docomputeprocess(modle, baseModleStatusCache);
    }


    @Override
    public BaseModelImp convertModel(Adapter adapter) {
        PidModleAdapter pidModleAdapter = (PidModleAdapter) adapter;
        PidModel pidModle = new PidModel();
        pidModle.setModleEnable(1);
        pidModle.setModleName(pidModleAdapter.getBasemodelparam().getModelname());
        pidModle.setModletype(AlgorithmName.PID_MODEL.getCode());
        pidModle.setRefprojectid(-1);
        pidModle.setModleId(pidModleAdapter.getBasemodelparam().getModelid());
        pidModle.setPropertyImpList(new ArrayList<>());

        BaseModelProperty kpbasemodleproperty = initpidproperty("kp", pidModleAdapter.getBasemodelparam().getModelid(), pidModleAdapter.getInputparam().getKp());//new BaseModlePropertyImp();
        pidModle.getPropertyImpList().add(kpbasemodleproperty);

        BaseModelProperty kibasemodleproperty = initpidproperty("ki", pidModleAdapter.getBasemodelparam().getModelid(), pidModleAdapter.getInputparam().getKi());
        pidModle.getPropertyImpList().add(kibasemodleproperty);


        BaseModelProperty kdbasemodleproperty = initpidproperty("kd", pidModleAdapter.getBasemodelparam().getModelid(), pidModleAdapter.getInputparam().getKd());
        pidModle.getPropertyImpList().add(kdbasemodleproperty);


        BaseModelProperty deadZonebasemodleproperty = initpidproperty("deadZone", pidModleAdapter.getBasemodelparam().getModelid(), pidModleAdapter.getInputparam().getDeadZone());//new BaseModlePropertyImp();
        pidModle.getPropertyImpList().add(deadZonebasemodleproperty);


        BaseModelProperty pvbasemodleproperty = initpidproperty(AlgorithmModelProperty.MODEL_PROPERTY_PV.getCode(), pidModleAdapter.getBasemodelparam().getModelid(), pidModleAdapter.getInputparam().getPv());//new BaseModlePropertyImp();
        pidModle.getPropertyImpList().add(pvbasemodleproperty);


        BaseModelProperty spbasemodleproperty = initpidproperty(AlgorithmModelProperty.MODEL_PROPERTY_SP.getCode(), pidModleAdapter.getBasemodelparam().getModelid(), pidModleAdapter.getInputparam().getSp());//new BaseModlePropertyImp();
        pidModle.getPropertyImpList().add(spbasemodleproperty);


        MpcModelProperty initpidmvproperty = initpidplusproperty(AlgorithmModelProperty.MODEL_PROPERTY_MV.getCode(), pidModleAdapter.getBasemodelparam().getModelid(), pidModleAdapter.getInputparam().getMv(), pidModleAdapter.getInputparam().getDmvHigh(), pidModleAdapter.getInputparam().getDmvLow());//new BaseModlePropertyImp();
        pidModle.getPropertyImpList().add(initpidmvproperty);


        BaseModelProperty initpidmvupproperty = initpidproperty(AlgorithmModelProperty.MODEL_PROPERTY_MVUP.getCode(), pidModleAdapter.getBasemodelparam().getModelid(), pidModleAdapter.getInputparam().getMvuppinvalue());
        pidModle.getPropertyImpList().add(initpidmvupproperty);

        BaseModelProperty initpidmvdownproperty = initpidproperty(AlgorithmModelProperty.MODEL_PROPERTY_MVDOWN.getCode(), pidModleAdapter.getBasemodelparam().getModelid(), pidModleAdapter.getInputparam().getMvdownpinvalue());
        pidModle.getPropertyImpList().add(initpidmvdownproperty);


        if (pidModleAdapter.getInputparam().getFf() != null) {
            BaseModelProperty ffbasemodleproperty = initpidproperty(AlgorithmModelProperty.MODEL_PROPERTY_FF.getCode(), pidModleAdapter.getBasemodelparam().getModelid(), pidModleAdapter.getInputparam().getFf());//new BaseModlePropertyImp();
            pidModle.getPropertyImpList().add(ffbasemodleproperty);

            BaseModelProperty kfbasemodleproperty = initpidproperty(AlgorithmModelProperty.MODEL_PROPERTY_PID_KF.getCode(), pidModleAdapter.getBasemodelparam().getModelid(), pidModleAdapter.getInputparam().getKf());//new BaseModlePropertyImp();
            pidModle.getPropertyImpList().add(kfbasemodleproperty);

        }


        BaseModelProperty initpidautoproperty = initpidproperty(AlgorithmModelProperty.MODEL_PROPERTY_MODELAUTO.getCode(), pidModleAdapter.getBasemodelparam().getModelid(), pidModleAdapter.getInputparam().getAuto());
        pidModle.getPropertyImpList().add(initpidautoproperty);


        for (PidOutPutproperty pidOutPutproperty : pidModleAdapter.getOutputparam()) {
            BaseModelProperty outpropertyImp = new BaseModelProperty();
            outpropertyImp.setRefmodleId(pidModleAdapter.getBasemodelparam().getModelid());
            outpropertyImp.setModleOpcTag("");
            outpropertyImp.setModlePinName(pidOutPutproperty.getOutputpinname());
            outpropertyImp.setOpcTagName("");

            JSONObject resource = new JSONObject();
            resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
            outpropertyImp.setResource(resource);
            outpropertyImp.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_OUTPUT.getCode());
            outpropertyImp.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_BASE.getCode());
            pidModle.getPropertyImpList().add(outpropertyImp);
        }

        return pidModle;
    }


    private BaseModelProperty initpidproperty(String pinname, long modleId, double properyconstant) {
        BaseModelProperty kpbasemodleproperty = new BaseModelProperty();
        kpbasemodleproperty.setModlePinName(pinname);
        kpbasemodleproperty.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_BASE.getCode());
        kpbasemodleproperty.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
        kpbasemodleproperty.setRefmodleId(modleId);
        kpbasemodleproperty.setPinEnable(1);
        JSONObject resource = new JSONObject();
        resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
        resource.put("value", properyconstant);
        kpbasemodleproperty.setResource(resource);
        kpbasemodleproperty.setModleOpcTag("");
        kpbasemodleproperty.setOpcTagName("");
        return kpbasemodleproperty;
    }


    private MpcModelProperty initpidplusproperty(String pinname, long modleId, double properyconstant, double dmvhight, double dmvlow) {
        MpcModelProperty kpbasemodleproperty = new MpcModelProperty();
        kpbasemodleproperty.setModlePinName(pinname);
        kpbasemodleproperty.setOpcTagName("");
        kpbasemodleproperty.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_MPC.getCode());
        kpbasemodleproperty.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
        kpbasemodleproperty.setRefmodleId(modleId);
        kpbasemodleproperty.setPinEnable(1);
        kpbasemodleproperty.setDmvLow(dmvlow);
        kpbasemodleproperty.setDmvHigh(dmvhight);
//        kpbasemodleproperty.setDeadZone(deadZone);


        JSONObject resource = new JSONObject();
        resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
        resource.put("value", properyconstant);
        kpbasemodleproperty.setResource(resource);
        kpbasemodleproperty.setModleOpcTag("");
        return kpbasemodleproperty;
    }


    /**
     * 模型短路
     */
    private BaseModelResponseDto modleshortcircuit(BaseModelImp baseModelImp) {
        BaseModelProperty mvinputpin = selectModelProperyByPinname(AlgorithmModelProperty.MODEL_PROPERTY_MV.getCode(), baseModelImp.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
        BaseModelProperty mvoutputpin = selectModelProperyByPinname(AlgorithmModelProperty.MODEL_PROPERTY_MV.getCode(), baseModelImp.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_OUTPUT.getCode());
        if ((mvinputpin != null) && (mvoutputpin != null)) {
            //输入直接反写到输出
            mvoutputpin.setValue(mvinputpin.getValue());
            PidResponse4PlantDto pidResponseDto = buildResponse(baseModelImp, 0, 0, 0, "模型短路", 200);
            return pidResponseDto;
        } else {
            return buildResponse(baseModelImp, 0, 0, 0, "模型短路，但无匹配的输入输出", 123456);
        }

    }


    private PidResponse4PlantDto buildResponse(BaseModelImp baseModelImp, double bkp, double bki, double bkd, String msg, int status) {

        PidResponse4PlantDto pidRespon = new PidResponse4PlantDto();
        PidData4PlantDto pidData = new PidData4PlantDto();
        pidRespon.setMessage(msg);
        pidRespon.setStatus(status);
        pidRespon.setData(pidData);

        pidData.setPartkp(bkp);
        pidData.setPartki(bki);
        pidData.setPartkd(bkd);

        List<PinDataDto> dmvDataList = new ArrayList<>();
        pidData.setMvData(dmvDataList);

        List<BaseModelProperty> modleProperties = baseModelImp.getPropertyImpList();

        if (!CollectionUtils.isEmpty(modleProperties)) {
            modleProperties.stream().filter(p -> {
                if (p.getPindir().equals(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_OUTPUT.getCode())) {
                    return true;
                } else {
                    return false;
                }

            }).forEach(p -> {
                PinDataDto dmvData = new PinDataDto();
                dmvData.setPinname(p.getModlePinName());
                dmvData.setValue(p.getValue());
                dmvDataList.add(dmvData);
            });
        }
        return pidRespon;
    }

    /**
     * 组装pid请求参数
     */
    private CallBaseRequestDto buildRequest(BaseModelImp baseModelImp, Object context) {
        JSONObject scriptinput = new JSONObject();

        JSONObject INjson = new JSONObject();
        scriptinput.put("IN1", INjson);

        JSONObject OUTjson = new JSONObject();
        scriptinput.put("OUT1", OUTjson);

        if (!CollectionUtils.isEmpty(baseModelImp.getPropertyImpList())) {
            baseModelImp.getPropertyImpList().forEach(p -> {
                if (p.getPindir().equals(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode())) {
                    JSONObject invalue = new JSONObject();
                    invalue.put("value", p.getValue());
                    INjson.put(p.getModlePinName(), invalue);

                    if (p.getModlePinName().equals(AlgorithmModelProperty.MODEL_PROPERTY_MV.getCode())) {
                        JSONObject inmvdmvHighnealue = new JSONObject();
                        inmvdmvHighnealue.put("value", p.getDmvHigh());
                        INjson.put("dmvHigh", inmvdmvHighnealue);

                        JSONObject inmvdmvLownealue = new JSONObject();
                        inmvdmvLownealue.put("value", p.getDmvLow());
                        INjson.put("dmvLow", inmvdmvLownealue);
                    }

                } else if (p.getPindir().equals(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_OUTPUT.getCode())) {
                    JSONObject outvalue = new JSONObject();
                    outvalue.put("pinName", p.getModlePinName());
                    OUTjson.put(p.getModlePinName(), outvalue);
                }
            });
        }

        return CallBaseRequestDto.builder()
                .context(context == null ? new JSONObject() : context)
                .input(scriptinput)
                .build();
    }

    /**
     * 调用pid算法接口
     */
    public PidResponse4PlantDto callPid(BaseModleStatusCache baseModleStatusCache, BaseModelImp baseModelImp, CallBaseRequestDto callBaseRequestDto) {

        String requestUrl = algorithmRouteConfig.getUrl() + algorithmRouteConfig.getPid();
        ResponseEntity<PidResponseDto> responseEntity = null;
        try {
            responseEntity = restTemplateNoCloud.postForEntity(requestUrl,
                    JSONObject.parseObject(JSONObject.toJSONString(callBaseRequestDto)), PidResponseDto.class);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return buildResponse(baseModelImp, 0, 0, 0, e.getMessage(), 123456);
        }

        if (ObjectUtils.isEmpty(responseEntity) || ObjectUtils.isEmpty(responseEntity.getBody())) {
            //接口失败
            return buildResponse(baseModelImp, 0, 0, 0, "调用FPID接口失败,无返回内容", 123456);
        }

        if (HttpStatus.OK.value() != responseEntity.getBody().getStatus()) {
            return buildResponse(baseModelImp, 0, 0, 0, responseEntity.getBody().getMessage(), 123456);
        }

        PidResponseDto pidResponseDto = responseEntity.getBody();
        //模型缓存上下文更新
        if (!ObjectUtils.isEmpty(baseModleStatusCache)) {
            baseModleStatusCache.setAlgorithmContext(pidResponseDto.getAlgorithmContext());
        }
        computresulteprocess(baseModelImp, pidResponseDto.getData());

        return buildResponse(baseModelImp, pidResponseDto.getData().getMv().getPartkp(), pidResponseDto.getData().getMv().getPartki(), pidResponseDto.getData().getMv().getPartkd(), pidResponseDto.getMessage(), pidResponseDto.getStatus());

    }
}



