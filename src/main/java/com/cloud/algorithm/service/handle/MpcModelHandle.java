package com.cloud.algorithm.service.handle;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cloud.algorithm.config.AlgorithmRouteConfig;
import com.cloud.algorithm.config.MpcConfig;
import com.cloud.algorithm.constant.*;
import com.cloud.algorithm.model.BaseModelImp;
import com.cloud.algorithm.model.bean.ResponTimeSerise;
import com.cloud.algorithm.model.bean.cache.BaseModleStatusCache;
import com.cloud.algorithm.model.bean.cache.MpcModelStatusCache;
import com.cloud.algorithm.model.bean.controlmodel.MpcModel;
import com.cloud.algorithm.model.bean.modelproperty.BaseModelProperty;
import com.cloud.algorithm.model.bean.modelproperty.BoundModelPropery;
import com.cloud.algorithm.model.bean.modelproperty.MpcModelProperty;
import com.cloud.algorithm.model.dto.*;
import com.cloud.algorithm.model.dto.apcPlant.Adapter;
import com.cloud.algorithm.model.dto.apcPlant.request.dmc.*;
import com.cloud.algorithm.model.dto.apcPlant.response.dmc.DmcData4PlantDto;
import com.cloud.algorithm.model.dto.apcPlant.response.dmc.DmcDmvData4PlantDto;
import com.cloud.algorithm.model.dto.apcPlant.response.dmc.DmcPvPredictData4PlantDto;
import com.cloud.algorithm.model.dto.apcPlant.response.dmc.DmcResponse4PlantDto;
import com.cloud.algorithm.service.BoundCondition;
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

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 9:39
 */
@Service
@Slf4j
public class MpcModelHandle implements Handle, BoundCondition {
    private static final Pattern pvpattern = Pattern.compile("(^pv(\\d+)$)");
    private static final Pattern ffpattern = Pattern.compile("(^ff(\\d+)$)");
    private static final Pattern mvpattern = Pattern.compile("(^mv(\\d+)$)");


    @Autowired
    private AlgorithmRouteConfig algorithmRouteConfig;

    @Autowired
    @Qualifier("nocloud")
    private RestTemplate restTemplateNoCloud;


    @Autowired
    private MpcConfig mpcConfig;


    @Override
    public String getCode() {
        return AlgorithmName.MPC_MODEL.getCode();
    }

    @Override
    public BaseModelResponseDto run(BaseModelImp modle, BaseModleStatusCache baseModleStatusCache/*???????????????????????????*/) {
        MpcModel mpcModel = (MpcModel) modle;
        mpcModel.setTotalPv(mpcConfig.getNumPv());
        mpcModel.setTotalMv(mpcConfig.getNumMv());
        mpcModel.setTotalFf(mpcConfig.getMumFf());
        inprocess(modle);
        return docomputeprocess(modle, baseModleStatusCache);
    }


    @Override
    public boolean isBreakLimit(BoundModelPropery boundModelPropery) {
        //??????????????????????????????
        boolean breaklow = ((boundModelPropery.getDownLmt() != null) && (boundModelPropery.getValue() < boundModelPropery.getDownLmt().getValue()));
        boolean breakup = ((boundModelPropery.getUpLmt() != null) && (boundModelPropery.getValue() > boundModelPropery.getUpLmt().getValue()));
        return (breaklow || breakup);
    }


    /**
     * ???????????????????????????????????????????????????????????????????????????,???????????????????????????
     *
     * @param modleStatusCache
     * @param modelId
     * @param code             cv1 pv1 mv1
     * @return true ???????????????????????????????????????false
     */
    @Override
    public boolean isclockAlarm(MpcModelStatusCache modleStatusCache, Long modelId, String code) {
        if (ObjectUtils.isEmpty(modleStatusCache)) {
            return true;
        } else {
            if (CollectionUtils.isEmpty(modleStatusCache.getPinClock())) {
                return true;
            } else {
                Instant runClock = modleStatusCache.getPinClock().get(code);
                if (!ObjectUtils.isEmpty(runClock)) {
                    //??????????????????????????????????????????????????????
                    //????????????
                    return Instant.now().isAfter(runClock);
                } else {
                    return true;
                }
            }
        }

    }


    /**
     * ????????????
     *
     * @param code    cv1 pv1
     * @param modelId
     */
    @Override
    public void clearRunClock(MpcModelStatusCache modleStatusCache, Long modelId, String code) {
        if (ObjectUtils.isEmpty(modleStatusCache)) {
            return;
        } else {
            if (CollectionUtils.isEmpty(modleStatusCache.getPinClock())) {
                return;
            } else {
                modleStatusCache.getPinClock().remove(code);
            }
        }
    }


    @Override
    public void inprocess(BaseModelImp baseModelImp) {
        if (!CollectionUtils.isEmpty(baseModelImp.getPropertyImpList())) {
            baseModelImp.getPropertyImpList().forEach(p -> {
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

        /**??????????????????????????????????????????*/
        BaseModleStatusCache modleStatusCache = baseModleStatusCache;

        //??????????????????????????????????????????????????????
        if (!CollectionUtils.isEmpty(baseModelImp.getPropertyImpList())) {
            Object finalModleStatusCache = modleStatusCache;
            //??????????????????????????????????????????????????????
            baseModelImp.getPropertyImpList().stream().forEach(p -> {
                Boolean activeStatus = ((MpcModelStatusCache) finalModleStatusCache).getPinActiveStatus().get(p.getModlePinName());
                p.setThisTimeParticipate(activeStatus == null || activeStatus);
            });
        }

        //?????????????????????dcs?????????
        BaseModelProperty autopin = selectModelProperyByPinname(AlgorithmModelProperty.MODEL_PROPERTY_MODELAUTO.getCode(), baseModelImp.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
        //????????????dcs??????????????????
        if (autopin != null) {
            if (autopin.getValue() == 0) {
                //????????????mv??????????????????mv???????????????
                //????????????;
                ((MpcModelStatusCache) modleStatusCache).setModelAuto(false);
                //????????????????????????????????????????????????????????????
                return modleshortcircuit((MpcModel) baseModelImp);
            } else if ((autopin.getValue() != 0) && (!((MpcModelStatusCache) modleStatusCache).getModelAuto())) {
                //????????????????????????
                ((MpcModelStatusCache) modleStatusCache).setModelAuto(true);
                ((MpcModelStatusCache) modleStatusCache).setModelbuild(false);
            }

        }

        //??????????????????????????????,?????????????????????????????????
        checkmodlepinisinLimit((MpcModelStatusCache) baseModleStatusCache, baseModelImp.getModleId(), baseModelImp.getPropertyImpList(), ((MpcModel) baseModelImp).getControlAPCOutCycle());

        if (!ObjectUtils.isEmpty(modleStatusCache)) {
            MpcModelStatusCache modleStCache = (MpcModelStatusCache) modleStatusCache;

            //????????????????????????
            if (modleBuild((MpcModel) baseModelImp)) {
                //???????????????????????????
                if (!modleStCache.getModelbuild()) {
                    //??????????????????????????????????????????????????????????????????????????????????????????
                    log.debug("build python modle id={}", baseModelImp.getModleId());
                    CallBaseRequestDto mpcBuildRequest = mpcBuildRequest((MpcModel) baseModelImp, null/*modleStCache.getAlgorithmContext()*/);
                    DmcResponse4PlantDto dmcResponse4PlantDto = callMpc(modleStCache, (MpcModel) baseModelImp, mpcBuildRequest);
                    if (dmcResponse4PlantDto.getStatus() != HttpStatus.OK.value()) {
                        return dmcResponse4PlantDto;
                    }
                }

            } else {
                if ((((MpcModel) baseModelImp).getNumOfRunnablePVPins_pp() == 0) || (((MpcModel) baseModelImp).getNumOfRunnableMVpins_mm() == 0)) {
                    //??????????????????????????????pv???mv????????????
                    return modleshortcircuit((MpcModel) baseModelImp);
                }
                return mpcBuildResponse("mpc?????????????????????????????????????????????pv???mv?????????????????????????????????", 123456);
            }


        } else {
            return mpcBuildResponse("??????????????????????????????,step=build", 123456);
        }


        //????????????????????????????????????????????? ?????????
//        Map jsonObject = (Map) baseModleStatusCache.getAlgorithmContext();
//        log.debug("mdc key is present={}", Optional.ofNullable(jsonObject.get("dmc")).isPresent());
        if (!ObjectUtils.isEmpty(modleStatusCache)) {
            CallBaseRequestDto callBaseRequestDto = mpcComputeRequest((MpcModel) baseModelImp, modleStatusCache.getAlgorithmContext());
            return callMpc((MpcModelStatusCache) modleStatusCache, (MpcModel) baseModelImp, callBaseRequestDto);
        } else {
            return mpcBuildResponse("??????????????????????????????,step=compute", 123456);
        }
    }


    /**
     * ?????????????????????????????????
     * ???????????????????????????mv???
     */
    public DmcResponse4PlantDto buildresulteprocess(MpcModelStatusCache mpcModelStatusCache, MpcModel baseModelImp, DmcResponseDto data) {

        //????????????????????????
        if (data.getData().getStep().equals(AlgorithmMpcModelStep.MPC_MODEL_STEP_BUILD.getStep())) {
            // ??????????????????????????????
            if (!ObjectUtils.isEmpty(mpcModelStatusCache)) {
                mpcModelStatusCache.setAlgorithmContext(data.getAlgorithmContext());
                mpcModelStatusCache.setModelbuild(true);
            }
            return mpcBuildResponse(data.getMessage(), data.getStatus());
        }
        return mpcErrorResponse("????????????mpc???????????????");
    }


    /**
     * ?????????????????????????????????
     * ???????????????????????????mv???
     */
    public DmcResponse4PlantDto computresulteprocess(MpcModelStatusCache mpcModelStatusCache, MpcModel baseModelImp, DmcResponseDto data) {
        //????????????????????????
        if (data.getData().getStep().equals(AlgorithmMpcModelStep.MPC_MODEL_STEP_COMPUTE.getStep())) {

            // ?????????????????????
            if (!ObjectUtils.isEmpty(mpcModelStatusCache)) {
                mpcModelStatusCache.setAlgorithmContext(data.getAlgorithmContext());
            }

            JSONArray predictpvJson = data.getData().getPredict();//
            JSONArray eJson = data.getData().getE();//
            JSONArray funelupAnddownJson = data.getData().getFunelupAnddown();//
            JSONArray dmvJson = data.getData().getDmv();//
            JSONArray dffJson = data.getData().getDff();//

            int p = baseModelImp.getNumOfRunnablePVPins_pp();
            int m = baseModelImp.getNumOfRunnableMVpins_mm();
            int v = baseModelImp.getNumOfRunnableFFpins_vv();
            int N = baseModelImp.getTimeserise_N();

            double[] predictpvArray = new double[p * N];
            double[][] funelupAnddownArray = new double[2][p * N];
            double[] eArray = new double[p];
            double[] dmvArray = new double[m];

            double[] dffArray = null;
            if (v != 0) {
                dffArray = new double[v];
            }
            for (int i = 0; i < p * N; i++) {
                predictpvArray[i] = predictpvJson.getDouble(i);
                funelupAnddownArray[0][i] = funelupAnddownJson.getJSONArray(0).getDouble(i);
                funelupAnddownArray[1][i] = funelupAnddownJson.getJSONArray(1).getDouble(i);
            }

            for (int i = 0; i < p; i++) {
                eArray[i] = eJson.getDouble(i);
            }

            for (int i = 0; i < m; i++) {
                dmvArray[i] = dmvJson.getDouble(i);
            }
            for (int i = 0; i < v; ++i) {
                dffArray[i] = dffJson.getDouble(i);
            }

            updateModleComputeResult(baseModelImp, predictpvArray, funelupAnddownArray, dmvArray, eArray, dffArray);


            JSONArray mvJson = data.getData().getMv();
            int index = 0;
            List<MpcModelProperty> runablemv = getRunablePins(baseModelImp.getCategoryMVmodletag(), baseModelImp.getMaskisRunnableMVMatrix());
            for (MpcModelProperty mpcModleProperty : runablemv) {
                String outputpinname = mpcModleProperty.getModlePinName();
                double outvalue = mvJson.getDouble(index);
                index++;
                for (BaseModelProperty modleProperty : baseModelImp.getPropertyImpList()) {
                    //???????????????????????????mv
                    MpcModelProperty outputpin = (MpcModelProperty) modleProperty;
                    if (outputpin.getPindir().equals(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_OUTPUT.getCode())) {
                        if (outputpinname.equals(outputpin.getModlePinName())) {
                            outputpin.setValue(outvalue);
                        }
                    }

                }
            }

            return mpcComputeResponse(baseModelImp, data.getMessage(), data.getStatus());
        }

        return mpcErrorResponse("????????????mpc???????????????");
    }


    @Override
    public void init() {

    }

    @Override
    public void destory() {

    }


    /**
     * ?????????????????????????????????mv??????????????????mv
     */
    private DmcResponse4PlantDto modleshortcircuit(MpcModel baseModelImp) {

        List<MpcModelProperty> modlePropertyList = new ArrayList<>();
        //????????????
        if (!CollectionUtils.isEmpty(baseModelImp.getPropertyImpList())) {
            modlePropertyList = baseModelImp.getPropertyImpList().stream().map(p -> {
                return (MpcModelProperty) p;
            }).collect(Collectors.toList());
        }
        List<MpcModelProperty> inputmvproperties = getSpecialPinTypeByMpc(AlgorithmModelProperty.MODEL_PROPERTY_MV.getCode(), modlePropertyList);

        //?????????mv?????????????????????mv?????????
        for (MpcModelProperty inputmv : inputmvproperties) {
            BaseModelProperty outputmv = selectModelProperyByPinname(inputmv.getModlePinName(), baseModelImp.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_OUTPUT.getCode());
            BaseModelProperty outputdmv = selectModelProperyByPinname("d" + inputmv.getModlePinName(), baseModelImp.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_OUTPUT.getCode());
            if (outputmv != null) {
                outputmv.setValue(inputmv.getValue());
            }
            if (outputdmv != null) {
                outputdmv.setValue(0d);
            }
        }
        return mpcComputeResponse(baseModelImp, "????????????", HttpStatus.OK.value());
    }

    /**
     * ??????mpc???????????????
     */
    private DmcResponse4PlantDto mpcErrorResponse(String msg) {
        DmcResponse4PlantDto dmcResponse4PlantDto = new DmcResponse4PlantDto();
        dmcResponse4PlantDto.setMessage(msg);
        dmcResponse4PlantDto.setStatus(123456);
        return dmcResponse4PlantDto;
    }


    private DmcResponse4PlantDto mpcComputeResponse(MpcModel mpcModel, String msg, int status) {

        DmcResponse4PlantDto dmcRespon = new DmcResponse4PlantDto();
        DmcData4PlantDto dmcdata = new DmcData4PlantDto();
        dmcRespon.setMessage(msg);

        dmcRespon.setStatus(status);


        dmcRespon.setData(dmcdata);


        List<PinDataDto> mvDataList = new ArrayList<>();
        dmcdata.setMvData(mvDataList);


        for (BaseModelProperty modleProperty : mpcModel.getPropertyImpList()) {
            //???????????????????????????mv
            MpcModelProperty outputpin = (MpcModelProperty) modleProperty;
            if (outputpin.getPindir().equals(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_OUTPUT.getCode())) {
                PinDataDto pinData = new PinDataDto();
                pinData.setPinname(outputpin.getModlePinName());
                //?????????null??????????????????mv?????????????????????????????????????????????????????????mv???????????????
                if (outputpin.getValue() == null) {
                    Map<String, BaseModelProperty> inputBasePropertMapp = mpcModel.getPropertyImpList().stream().filter(
                            mv -> {
                                return mv.getPindir().equals(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
                            }).collect(Collectors.toMap(BaseModelProperty::getModlePinName, p -> p, (o, n) -> n));
                    if (inputBasePropertMapp.containsKey(outputpin.getModlePinName())) {
                        pinData.setValue(inputBasePropertMapp.get(outputpin.getModlePinName()).getValue());
                    } else {
                        pinData.setValue(0);
                    }
                } else {
                    pinData.setValue(outputpin.getValue());
                }

                mvDataList.add(pinData);
            }
        }


        List<DmcPvPredictData4PlantDto> pvpredicts = new ArrayList<>();

        List<MpcModelProperty> runnanlepvs = getRunablePins(mpcModel.getCategoryPVmodletag(), mpcModel.getMaskisRunnablePVMatrix());

        for (int index = 0; index < runnanlepvs.size(); index++) {
            DmcPvPredictData4PlantDto pvpredict = new DmcPvPredictData4PlantDto();
            pvpredicts.add(pvpredict);
            pvpredict.setPvpinname(runnanlepvs.get(index).getModlePinName());
            pvpredict.setPredictorder(mpcModel.getBackPVPrediction()[index]);
            pvpredict.setUpfunnel(mpcModel.getBackPVFunelUp()[index]);
            pvpredict.setDownfunnel(mpcModel.getBackPVFunelDown()[index]);
            pvpredict.setE(mpcModel.getBackPVPredictionError()[index]);
        }
        dmcdata.setPvpredict(pvpredicts);


        List<MpcModelProperty> runnanlemvs = getRunablePins(mpcModel.getCategoryMVmodletag(), mpcModel.getMaskisRunnableMVMatrix());
        List<DmcDmvData4PlantDto> dmvDataList = new ArrayList<>();
        for (int index = 0; index < runnanlemvs.size(); index++) {
            DmcDmvData4PlantDto dmvData = new DmcDmvData4PlantDto();
            dmvData.setInputpinname(runnanlemvs.get(index).getModlePinName());
            dmvData.setValue(mpcModel.getBackrawDmv()[index]);
            dmvDataList.add(dmvData);
        }
        //???????????????mv???dmv?????????
        mpcModel.getCategoryMVmodletag().stream().filter(mv -> {
            //????????????????????????????????????????????????
            boolean in_res = runnanlemvs.stream().anyMatch(run_mv -> {
                return run_mv.getModlePinName().equals(mv.getModlePinName());
            });
            return !in_res;
        }).forEach(mv -> {
            //???????????????????????????mv,?????????dmv???0
            DmcDmvData4PlantDto dmvData = new DmcDmvData4PlantDto();
            dmvData.setInputpinname(mv.getModlePinName());
            dmvData.setValue(0.0);
            dmvDataList.add(dmvData);
        });
        dmcdata.setDmv(dmvDataList);

        return dmcRespon;
    }

    private DmcResponse4PlantDto mpcBuildResponse(String msg, int status) {
        DmcResponse4PlantDto dmcRespon = new DmcResponse4PlantDto();
        dmcRespon.setMessage(msg);
        dmcRespon.setStatus(status);
        return dmcRespon;
    }


    /**
     * ??????????????????dmc???????????????dao
     */
    private CallBaseRequestDto mpcBuildRequest(MpcModel mpcModel, Object context) {
        JSONObject scriptinput = new JSONObject();
        /**base*/
        scriptinput.put("m", mpcModel.getNumOfRunnableMVpins_mm());
        scriptinput.put("p", mpcModel.getNumOfRunnablePVPins_pp());
        scriptinput.put("M", mpcModel.getControltime_M());
        scriptinput.put("P", mpcModel.getPredicttime_P());
        scriptinput.put("N", mpcModel.getTimeserise_N());
        scriptinput.put("fnum", mpcModel.getNumOfRunnableFFpins_vv());
        scriptinput.put("pvusemv", mpcModel.getMaskMatrixRunnablePVUseMV());
        scriptinput.put("APCOutCycle", mpcModel.getControlAPCOutCycle());
        scriptinput.put("enable", mpcModel.getModleEnable());
        scriptinput.put("funneltype", mpcModel.getFunneltype());
        scriptinput.put("runStyle",mpcModel.getRunstyle());//???????????????????????????????????????????????????

        /***mv*/
        if (mpcModel.getNumOfRunnablePVPins_pp() != 0) {
            scriptinput.put("A", mpcModel.getA_RunnabletimeseriseMatrix());
            scriptinput.put("integrationInc_mv", mpcModel.getRunnableintegrationInc_mv());
        }

        /**ff*/
        if (mpcModel.getNumOfRunnableFFpins_vv() != 0) {
            scriptinput.put("B", mpcModel.getB_RunnabletimeseriseMatrix());
            scriptinput.put("integrationInc_ff", mpcModel.getRunnableintegrationInc_ff());
        }

        scriptinput.put("Q", mpcModel.getQ());
        scriptinput.put("R", mpcModel.getR());
        scriptinput.put("alphe", mpcModel.getAlpheTrajectoryCoefficients());
        scriptinput.put("alphemethod", mpcModel.getAlpheTrajectoryCoefmethod());
        scriptinput.put("step", AlgorithmMpcModelStep.MPC_MODEL_STEP_BUILD.getStep());

        return CallBaseRequestDto.builder()
                .context(context == null ? new JSONObject() : context)
                .input(scriptinput)
                .build();
    }

    private CallBaseRequestDto mpcComputeRequest(MpcModel mpcModel, Object context) {
        JSONObject scriptinput = new JSONObject();

        /**
         * y0(pv)
         * limitU(mv)
         * limitY()
         * FF
         * Wi(sp)
         *
         * */
        /**sp*/
        Double[] sp = new Double[mpcModel.getNumOfRunnablePVPins_pp()];
        int indexEnableSP = 0;
        for (int indexsp = 0; indexsp < mpcModel.getCategorySPmodletag().size(); ++indexsp) {
            if (mpcModel.getMaskisRunnablePVMatrix()[indexsp] == 0) {
                continue;
            }
            sp[indexEnableSP] = mpcModel.getCategorySPmodletag().get(indexsp).getValue();
            indexEnableSP++;
        }
        scriptinput.put("wi", sp);

        /**pv*/
        Double[] pv = new Double[mpcModel.getNumOfRunnablePVPins_pp()];
        int indexEnablePV = 0;
        for (int indexpv = 0; indexpv < mpcModel.getCategoryPVmodletag().size(); ++indexpv) {
            if (mpcModel.getMaskisRunnablePVMatrix()[indexpv] == 0) {
                continue;
            }
            /***
             * ??????????????????????????????????????????????????????????????????????????????
             * */
            pv[indexEnablePV] = mpcModel.getCategoryPVmodletag().get(indexpv).getValue();
            ++indexEnablePV;
        }
        scriptinput.put("y0", pv);

        /**limitU???????????? mv?????????*/
        Double[][] limitU = new Double[mpcModel.getNumOfRunnableMVpins_mm()][2];
        Double[][] limitDU = new Double[mpcModel.getNumOfRunnableMVpins_mm()][2];
        /**U?????????????????????*/
        Double[] U = new Double[mpcModel.getNumOfRunnableMVpins_mm()];
        /**U?????????????????????**/
        Double[] UFB = new Double[mpcModel.getNumOfRunnableMVpins_mm()];

        int indexEnableMV = 0;
        for (int indexmv = 0; indexmv < mpcModel.getCategoryMVmodletag().size(); ++indexmv) {
            if (mpcModel.getMaskisRunnableMVMatrix()[indexmv] == 0) {
                continue;
            }
            Double[] mvminmax = new Double[2];
            BoundModelPropery mvdown = mpcModel.getCategoryMVmodletag().get(indexmv).getDownLmt();
            BoundModelPropery mvup = mpcModel.getCategoryMVmodletag().get(indexmv).getUpLmt();

            mvminmax[0] = mvdown.getValue();

            mvminmax[1] = mvup.getValue();

            //???????????????
            limitU[indexEnableMV] = mvminmax;

            Double[] dmvminmax = new Double[2];
            dmvminmax[0] = mpcModel.getCategoryMVmodletag().get(indexmv).getDmvLow();
            dmvminmax[1] = mpcModel.getCategoryMVmodletag().get(indexmv).getDmvHigh();
            limitDU[indexEnableMV] = dmvminmax;

            //???????????????
            U[indexEnableMV] = mpcModel.getCategoryMVmodletag().get(indexmv).getValue();
            UFB[indexEnableMV] = mpcModel.getCategoryMVmodletag().get(indexmv).getFeedBack().getValue();
            indexEnableMV++;
        }
        scriptinput.put("limitU", limitU);
        scriptinput.put("limitDU", limitDU);
        scriptinput.put("U", U);
        scriptinput.put("UFB", UFB);

        //FF
        int indexEnableFF = 0;
        if (mpcModel.getNumOfRunnableFFpins_vv() != 0) {
            Double[] ff = new Double[mpcModel.getNumOfRunnableFFpins_vv()];
            Double[] fflmt = new Double[mpcModel.getNumOfRunnableFFpins_vv()];
            for (int indexff = 0; indexff < mpcModel.getCategoryFFmodletag().size(); ++indexff) {
                if (mpcModel.getMaskisRunnableFFMatrix()[indexff] == 0) {
                    continue;
                }

                ff[indexEnableFF] = mpcModel.getCategoryFFmodletag().get(indexff).getValue();
                BoundModelPropery ffuppin = mpcModel.getCategoryFFmodletag().get(indexff).getUpLmt();
                BoundModelPropery ffdownpin = mpcModel.getCategoryFFmodletag().get(indexff).getDownLmt();
                if ((ffuppin != null) && (ffdownpin != null)) {
                    /**ff??????????????????????????????*/
                    Double ffHigh = 0d;
                    Double ffLow = 0d;

                    ffHigh = ffuppin.getValue();
                    ffLow = ffdownpin.getValue();

                    if ((ffLow <= mpcModel.getCategoryFFmodletag().get(indexff).getValue()) && (ffHigh >= mpcModel.getCategoryFFmodletag().get(indexff).getValue())) {
                        fflmt[indexEnableFF] = 1d;
                    } else {
                        fflmt[indexEnableFF] = 0d;
                    }
                }

                indexEnableFF++;
            }
            scriptinput.put("FF", ff);
        }

        scriptinput.put("enable", 1);
        /**
         *??????????????????????????????
         * */
        scriptinput.put("deadZones", mpcModel.getDeadZones());
        scriptinput.put("funelInitValues", mpcModel.getFunelinitvalues());
        scriptinput.put("step", AlgorithmMpcModelStep.MPC_MODEL_STEP_COMPUTE.getStep());


        return CallBaseRequestDto.builder()
                .context(context == null ? new JSONObject() : context)
                .input(scriptinput)
                .build();
    }


    /**
     * ??????dmc??????????????????????????????
     */
    private DmcResponse4PlantDto callMpc(MpcModelStatusCache mpcModelStatusCache, MpcModel mpcModel, CallBaseRequestDto callBaseRequestDto) {

        String requestUrl = algorithmRouteConfig.getUrl() + algorithmRouteConfig.getDmc();
        ResponseEntity<DmcResponseDto> responseEntity = null;
        try {
            responseEntity = restTemplateNoCloud.postForEntity(requestUrl,
                    JSONObject.parseObject(JSONObject.toJSONString(callBaseRequestDto)), DmcResponseDto.class);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return mpcErrorResponse(e.getMessage());
        }

        if (ObjectUtils.isEmpty(responseEntity) || ObjectUtils.isEmpty(responseEntity.getBody())) {
            //????????????
            return mpcErrorResponse("??????DMC????????????");
        }
        DmcResponse4PlantDto dmcResponse4PlantDto = null;
        try {
            DmcResponseDto dmcResponse = responseEntity.getBody();
            if (dmcResponse.getStatus() == (HttpStatus.OK.value())) {
                if (dmcResponse.getData().getStep().equals(AlgorithmMpcModelStep.MPC_MODEL_STEP_BUILD.getStep())) {
                    dmcResponse4PlantDto = buildresulteprocess(mpcModelStatusCache, mpcModel, dmcResponse);
                } else if (dmcResponse.getData().getStep().equals(AlgorithmMpcModelStep.MPC_MODEL_STEP_COMPUTE.getStep())) {
                    dmcResponse4PlantDto = computresulteprocess(mpcModelStatusCache, mpcModel, dmcResponse);
                }
            } else {
                //??????????????????????????????????????????????????????
                dmcResponse4PlantDto = mpcErrorResponse(dmcResponse.getMessage());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return mpcErrorResponse(e.getMessage());
        }
        return dmcResponse4PlantDto;
    }


    /**
     * ????????????????????????0??????????????????
     *
     * @param categorypins ???????????????
     * @param maskmatrix   ????????????
     */
    private List<MpcModelProperty> getRunablePins(List<MpcModelProperty> categorypins, int[] maskmatrix) {
        List<MpcModelProperty> result = new LinkedList<>();
        for (int indexpin = 0; indexpin < categorypins.size(); ++indexpin) {
            if (1 == maskmatrix[indexpin]) {
                result.add(categorypins.get(indexpin));
            }
        }
        return result;
    }


    /**
     * ??????????????????????????????
     *
     * @param funelupAnddown   ?????????2XpN
     *                         ???0????????????????????????[0~N-1]?????????pv??????[N~2N-1]????????????pv???????????????
     *                         ???1????????????????????????
     * @param backPVPrediction
     */
    private boolean updateModleComputeResult(MpcModel mpcModel, double[] backPVPrediction, double[][] funelupAnddown, double[] backDmvWrite, double[] backPVPredictionError, double[] dff) {
        /**
         * ?????????????????????
         * */
        try {
            for (int i = 0; i < mpcModel.getNumOfRunnablePVPins_pp(); i++) {
                mpcModel.getBackPVPrediction()[i] = Arrays.copyOfRange(backPVPrediction, 0 + mpcModel.getTimeserise_N() * i, mpcModel.getTimeserise_N() + mpcModel.getTimeserise_N() * i);//pv???????????????
                mpcModel.getBackPVFunelUp()[i] = Arrays.copyOfRange(funelupAnddown[0], 0 + mpcModel.getTimeserise_N() * i, mpcModel.getTimeserise_N() + mpcModel.getTimeserise_N() * i);//PV???????????????
                mpcModel.getBackPVFunelDown()[i] = Arrays.copyOfRange(funelupAnddown[1], 0 + mpcModel.getTimeserise_N() * i, mpcModel.getTimeserise_N() + mpcModel.getTimeserise_N() * i);//PV???????????????
            }

            /**????????????*/
            mpcModel.setBackPVPredictionError(backPVPredictionError);
            mpcModel.setBackrawDmv(backDmvWrite);
            mpcModel.setBackrawDff(dff);
            for (int indexpv = 0; indexpv < mpcModel.getNumOfRunnablePVPins_pp(); indexpv++) {

                /**dMV?????????*/
                for (int indexmv = 0; indexmv < mpcModel.getNumOfRunnableMVpins_mm(); indexmv++) {
                    if (mpcModel.getMaskMatrixRunnablePVUseMV()[indexpv][indexmv] == 1) {
                        mpcModel.getBackDmvWrite()[indexpv][indexmv] = backDmvWrite[indexmv];
                    }
                }

                /**????????????*/
                for (int indexff = 0; indexff < mpcModel.getNumOfRunnableFFpins_vv(); indexff++) {
                    if (mpcModel.getMaskMatrixRunnablePVUseFF()[indexpv][indexff] == 1) {
                        mpcModel.getBackDff()[indexpv][indexff] = dff[indexff];
                    }
                }

            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }


    /**
     * 0?????????pv???ff?????????????????????????????????????????????
     * 1-1?????????????????????????????????????????????????????????????????????????????????
     * 2??????
     * 2-1??????????????????,???????????????????????????????????????????????????????????????
     * ???????????????????????????
     *
     * @param modelId
     * @param contrltime       ????????????
     * @param pins
     * @param modleStatusCache ????????????????????????
     */
    private boolean checkmodlepinisinLimit(MpcModelStatusCache modleStatusCache, Long modelId, List<BaseModelProperty> pins, Integer contrltime) {
        /**??????????????????????????????pv??????ff????????????????????????pv???ff??????????????????????????????mv???????????????????????????mv????????????????????????????????????????????????????????????*/
        boolean ishavebreakOrRestorepin = false;//??????????????????????????????????????????
        for (BaseModelProperty pin : pins) {
            MpcModelProperty mpcModleProperty = (MpcModelProperty) pin;
            boolean ispvpin = (mpcModleProperty.getPintype() != null) && (mpcModleProperty.getPintype().equals(AlgorithmModelProperty.MODEL_PROPERTY_PV.getCode()));
            boolean isffpin = (mpcModleProperty.getPintype() != null) && (mpcModleProperty.getPintype().equals(AlgorithmModelProperty.MODEL_PROPERTY_FF.getCode()));
            if (ispvpin || isffpin) {
                /**??????????????????*/
                /**
                 * ??????????????????????????????
                 * */
                if (isBreakLimit(mpcModleProperty)) {
                    /**????????????*/
                    if (mpcModleProperty.isThisTimeParticipate()) {
                        /*??????????????????????????????????????????????????????*/
                        mpcModleProperty.setThisTimeParticipate(false);
                        ishavebreakOrRestorepin = true;//?????????
                        log.debug(mpcModleProperty.getModlePinName() + " is broke limit");

                        if (!ObjectUtils.isEmpty(modleStatusCache)) {
                            //????????????????????????
                            modleStatusCache.getPinActiveStatus().put(mpcModleProperty.getModlePinName(), false);
                            //????????????clock
                            if (!CollectionUtils.isEmpty(modleStatusCache.getPinClock())) {
                                modleStatusCache.getPinClock().remove(mpcModleProperty.getModlePinName());
                            }
                        }

                    }
                } else {
                    /**????????????*/
                    //???????????????????????????
                    /*?????????????????????????????????????????????????????????,?????????????????????????????????*/
                    if (!mpcModleProperty.isThisTimeParticipate()) {
                        //?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                        /*????????????*/
                        if (!ObjectUtils.isEmpty(modleStatusCache)) {
                            /*???????????????????????????*/
                            if (null != modleStatusCache.getPinClock() && modleStatusCache.getPinClock().containsKey(mpcModleProperty.getModlePinName())) {
                                /*?????????????????????*/
                                if (isclockAlarm(modleStatusCache, modelId, mpcModleProperty.getModlePinName())) {
                                    //?????????
                                    ishavebreakOrRestorepin = true;
                                    //????????????
                                    if (!CollectionUtils.isEmpty(modleStatusCache.getPinClock())) {
                                        modleStatusCache.getPinClock().remove(mpcModleProperty.getModlePinName());
                                    }
                                    //????????????????????????
                                    mpcModleProperty.setThisTimeParticipate(true);
                                    //????????????
                                    modleStatusCache.getPinActiveStatus().put(mpcModleProperty.getModlePinName(), true);
                                    log.debug("id={},remove a  clock", modelId);
                                }//???????????????????????????????????????
                            } else {
                                /*???????????????????????????????????????*/
                                int checktime = 10;//sec
                                /*??????????????????????????????null/0????????????????????????????????????????????????10s*/
                                if (contrltime != null && contrltime > 0) {
                                    checktime = 3 * contrltime;
                                }
                                //????????????
                                if (null == modleStatusCache.getPinClock()) {
                                    modleStatusCache.setPinClock(new HashMap<>());
                                }
                                modleStatusCache.getPinClock().put(mpcModleProperty.getModlePinName(), Instant.now().plusSeconds(checktime));
                                //??????????????????????????????
                                log.debug("id={},set a new clock", modelId);
                            }
                        } else {
                            /*???????????????????????????????????????????????????????????????*/
                            ishavebreakOrRestorepin = true;
                            //????????????????????????
                            mpcModleProperty.setThisTimeParticipate(true);
                            modleStatusCache.getPinActiveStatus().put(mpcModleProperty.getModlePinName(), true);
                        }

                    }

                }

            }

        }

        if (ishavebreakOrRestorepin) {
            log.debug("some pin break limit");
            if (!ObjectUtils.isEmpty(modleStatusCache)) {
                modleStatusCache.setModelbuild(false);
            }
        }
        return ishavebreakOrRestorepin;
    }


    /**
     * ??????????????????
     */
    private Boolean modleBuild(MpcModel mpcModel) {
        log.debug("modle id=" + mpcModel.getModleId() + " is building");

        try {
            mpcModel.setStringmodlePinsMap(new ConcurrentHashMap<>());


            //??????????????????PV??????
            mpcModel.setCategoryPVmodletag(new ArrayList<>());
            //??????????????????SP??????
            mpcModel.setCategorySPmodletag(new ArrayList<>());
            //??????????????????MV??????
            mpcModel.setCategoryMVmodletag(new ArrayList<>());
            //??????????????????FF??????
            mpcModel.setCategoryFFmodletag(new ArrayList<>());
            /**?????????????????????*/
            if (!classAndCombineRegiterPinsToMap(mpcModel)) {
                return false;
            }


            /**init pvusemv and pvuseff matrix
             * ?????????????????????pv???mv/ff????????????
             * */
            mpcModel.setMaskBaseMapPvUseMvMatrix(new int[mpcModel.getCategoryPVmodletag().size()][mpcModel.getCategoryMVmodletag().size()]);
            mpcModel.setMaskBaseMapPvUseFfMatrix(new int[mpcModel.getCategoryPVmodletag().size()][mpcModel.getCategoryFFmodletag().size()]);

            /**pv???mv???????????????*/
            mpcModel.setMaskBaseMapPvEffectMvMatrix(new float[mpcModel.getCategoryPVmodletag().size()][mpcModel.getCategoryMVmodletag().size()]);

            mpcModel.setMaskisRunnableMVMatrix(new int[mpcModel.getCategoryMVmodletag().size()]);
            mpcModel.setMaskisRunnableFFMatrix(new int[mpcModel.getCategoryFFmodletag().size()]);
            mpcModel.setMaskisRunnablePVMatrix(new int[mpcModel.getCategoryPVmodletag().size()]);

            initRunnableMatrixAndBaseMapMatrix(mpcModel);

            //??????????????????pv??????
            mpcModel.setNumOfRunnablePVPins_pp(0);
            //??????????????????mv??????
            mpcModel.setNumOfRunnableMVpins_mm(0);
            //??????????????????ff??????
            mpcModel.setNumOfRunnableFFpins_vv(0);
            initStatisticRunnablePinNum(mpcModel);
            //????????????????????????????????????????????????build???
            if (mpcModel.getNumOfRunnablePVPins_pp() == 0 || mpcModel.getNumOfRunnableMVpins_mm() == 0) {
                log.debug("p=" + 0 + ",m=" + 0);
                return false;
            }
            /***
             * ??????????????????????????????
             * init A matrix
             * */
            mpcModel.setA_RunnabletimeseriseMatrix(new double[mpcModel.getNumOfRunnablePVPins_pp()][mpcModel.getNumOfRunnableMVpins_mm()][mpcModel.getTimeserise_N()]);
            //mv???????????????
            mpcModel.setRunnableintegrationInc_mv(new double[mpcModel.getNumOfRunnablePVPins_pp()][mpcModel.getNumOfRunnableMVpins_mm()]);


            //????????????pv???mv?????????????????????
            mpcModel.setMaskMatrixRunnablePvEffectMv(new float[mpcModel.getNumOfRunnablePVPins_pp()][mpcModel.getNumOfRunnableMVpins_mm()]);


            /***
             *1???fill respon into A matrix
             *2???and init matrixEnablePVUseMV
             * */

            /**predict zone params*/
            mpcModel.setQ(new Double[mpcModel.getNumOfRunnablePVPins_pp()]);//use for pv
            /**trajectry coefs*/
            mpcModel.setAlpheTrajectoryCoefficients(new Double[mpcModel.getNumOfRunnablePVPins_pp()]);//use for pv
            mpcModel.setAlpheTrajectoryCoefmethod(new String[mpcModel.getNumOfRunnablePVPins_pp()]);
            /**??????????????????????????????*/
            mpcModel.setDeadZones(new Double[mpcModel.getNumOfRunnablePVPins_pp()]);//use for pv
            mpcModel.setFunelinitvalues(new Double[mpcModel.getNumOfRunnablePVPins_pp()]);//use for pv
            /**funnel type*/
            mpcModel.setFunneltype(new double[mpcModel.getNumOfRunnablePVPins_pp()][2]);//use for pv


            mpcModel.setMaskMatrixRunnablePVUseMV(new int[mpcModel.getNumOfRunnablePVPins_pp()][mpcModel.getNumOfRunnableMVpins_mm()]);//recording enablepv use which mvs

            initPVparams(mpcModel);

            /**??????????????????*/
            //accumulativeNumOfRunnablePVMVMaping(mpcModel);


            /**init R control zone params*/
            mpcModel.setR(new Double[mpcModel.getNumOfRunnableMVpins_mm()]);//use for mv
            initRMatrix(mpcModel);

            /***
             * ????????????????????????
             * init B matrix
             * */
            mpcModel.setB_RunnabletimeseriseMatrix(new double[mpcModel.getNumOfRunnablePVPins_pp()][mpcModel.getNumOfRunnableFFpins_vv()][mpcModel.getTimeserise_N()]);
            mpcModel.setRunnableintegrationInc_ff(new double[mpcModel.getNumOfRunnablePVPins_pp()][mpcModel.getNumOfRunnableFFpins_vv()]);//ff???????????????

            /**
             *fill respon into B matrix
             *??????????????????????????????
             * */
            mpcModel.setMaskMatrixRunnablePVUseFF(new int[mpcModel.getNumOfRunnablePVPins_pp()][mpcModel.getNumOfRunnableFFpins_vv()]);

            initFFparams(mpcModel);


            /**???????????????????????? pv**/
            mpcModel.setBaseoutpoints_p(mpcModel.getCategoryPVmodletag().size());

            mpcModel.setBasefeedforwardpoints_v(mpcModel.getCategoryFFmodletag().size());

            mpcModel.setBaseinputpoints_m(mpcModel.getCategoryMVmodletag().size());

            /***************acp?????????????????????********************/
            mpcModel.setBackPVPrediction(new double[mpcModel.getNumOfRunnablePVPins_pp()][mpcModel.getTimeserise_N()]);//pv???????????????

            mpcModel.setBackPVFunelUp(new double[mpcModel.getNumOfRunnablePVPins_pp()][mpcModel.getTimeserise_N()]);//PV??????????????????

            mpcModel.setBackPVFunelDown(new double[mpcModel.getNumOfRunnablePVPins_pp()][mpcModel.getTimeserise_N()]);//???????????????

            mpcModel.setBackDmvWrite(new double[mpcModel.getNumOfRunnablePVPins_pp()][mpcModel.getNumOfRunnableMVpins_mm()]);//MV?????????
            mpcModel.setBackrawDmv(new double[mpcModel.getNumOfRunnableMVpins_mm()]);
            mpcModel.setBackrawDff(new double[mpcModel.getNumOfRunnableFFpins_vv()]);

            mpcModel.setBackPVPredictionError(new double[mpcModel.getNumOfRunnablePVPins_pp()]);//????????????

            mpcModel.setBackDff(new double[mpcModel.getNumOfRunnablePVPins_pp()][mpcModel.getNumOfRunnableFFpins_vv()]);//???????????????

//            javabuildcomplet.set(true);
            return true;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }


    /**
     * 1???????????????????????????map??????key=pvn/mvn/spn???(n=1,2,3..8) value=pin
     * 2??????????????????????????????????????????????????????????????????list???ex:categoryPVmodletag?????????pv?????????
     *
     * @return false ?????????????????????
     */
    private boolean classAndCombineRegiterPinsToMap(MpcModel mpcModel) {
        /**????????????????????????key=pvn/mvn/spn???(n=1,2,3..8) value=pin ????????????*/
        if (mpcModel.getPropertyImpList().size() == 0) {
            return false;
        }
        for (BaseModelProperty baseModlePropertyImp : mpcModel.getPropertyImpList()) {
            MpcModelProperty modlePin = (MpcModelProperty) baseModlePropertyImp;
            if (modlePin.getPindir().equals(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode())) {
                mpcModel.getStringmodlePinsMap().put(modlePin.getModlePinName(), modlePin);
            }

        }

        /**????????????*/
        for (int i = 1; i <= mpcModel.getTotalPv(); i++) {
            MpcModelProperty pvPin = mpcModel.getStringmodlePinsMap().get(AlgorithmModelProperty.MODEL_PROPERTY_PV.getCode() + i);
            if (pvPin != null) {
                mpcModel.getCategoryPVmodletag().add(pvPin);
                MpcModelProperty dcsEnablepin = mpcModel.getStringmodlePinsMap().get(AlgorithmModelProperty.MODEL_PROPERTY_PVENABLE.getCode() + i);
                if (dcsEnablepin != null) {
                    pvPin.setDcsEnabePin(dcsEnablepin);//dcs??????????????????????????????,????????????
                }

                MpcModelProperty pvdown = mpcModel.getStringmodlePinsMap().get(AlgorithmModelProperty.MODEL_PROPERTY_PVDOWN.getCode() + i);
                MpcModelProperty pvup = mpcModel.getStringmodlePinsMap().get(AlgorithmModelProperty.MODEL_PROPERTY_PVUP.getCode() + i);
                /**??????????????????????????????*/
                if ((null == pvdown) || (null == pvup)) {
                    log.debug("modleid=" + mpcModel.getModleId() + ",??????pv????????????????????????");
                }
                pvPin.setDownLmt(pvdown);//???????????????????????????
                pvPin.setUpLmt(pvup);//???????????????????????????

                MpcModelProperty spPin = mpcModel.getStringmodlePinsMap().get(AlgorithmModelProperty.MODEL_PROPERTY_SP.getCode() + i);//?????????sp??????
                if (spPin != null) {
                    mpcModel.getCategorySPmodletag().add(spPin);
                } else {
                    return false;
                }
            }
        }

        /**
         *ff
         * */
        for (int i = 1; i <= mpcModel.getTotalFf(); ++i) {
            MpcModelProperty ffPin = mpcModel.getStringmodlePinsMap().get(AlgorithmModelProperty.MODEL_PROPERTY_FF.getCode() + i);
            if (ffPin != null) {
                MpcModelProperty dcsEnablepin = mpcModel.getStringmodlePinsMap().get(AlgorithmModelProperty.MODEL_PROPERTY_FFENABLE.getCode() + i);
                if (dcsEnablepin != null) {
                    //dcs??????????????????????????????
                    ffPin.setDcsEnabePin(dcsEnablepin);
                }
                MpcModelProperty ffdown = mpcModel.getStringmodlePinsMap().get(AlgorithmModelProperty.MODEL_PROPERTY_FFDOWN.getCode() + i);
                MpcModelProperty ffup = mpcModel.getStringmodlePinsMap().get(AlgorithmModelProperty.MODEL_PROPERTY_FFUP.getCode() + i);
                ffPin.setDownLmt(ffdown);//???????????????????????????
                ffPin.setUpLmt(ffup);//???????????????????????????
                mpcModel.getCategoryFFmodletag().add(ffPin);
                if ((null == ffdown) || (null == ffup)) {
                    log.debug("modleid=" + mpcModel.getModleId() + ",??????ff????????????????????????");
                }
            }

        }

        /**
         * mv mvfb,mvdown mvup
         * */
        for (int i = 1; i < mpcModel.getTotalMv(); i++) {
            MpcModelProperty mvPin = mpcModel.getStringmodlePinsMap().get(AlgorithmModelProperty.MODEL_PROPERTY_MV.getCode() + i);

            if (mvPin != null) {

                MpcModelProperty dcsEnablepin = mpcModel.getStringmodlePinsMap().get(AlgorithmModelProperty.MODEL_PROPERTY_MVENABLE.getCode() + i);
                if (dcsEnablepin != null) {
                    mvPin.setDcsEnabePin(dcsEnablepin);//dcs??????????????????????????????
                }

                MpcModelProperty mvfbPin = mpcModel.getStringmodlePinsMap().get(AlgorithmModelProperty.MODEL_PROPERTY_MVFB.getCode() + i);
                MpcModelProperty mvupPin = mpcModel.getStringmodlePinsMap().get(AlgorithmModelProperty.MODEL_PROPERTY_MVUP.getCode() + i);
                MpcModelProperty mvdownPin = mpcModel.getStringmodlePinsMap().get(AlgorithmModelProperty.MODEL_PROPERTY_MVDOWN.getCode() + i);
                if (mvfbPin != null && mvupPin != null && mvdownPin != null) {
                    mvPin.setUpLmt(mvupPin);
                    mvPin.setDownLmt(mvdownPin);
                    mvPin.setFeedBack(mvfbPin);
                    mpcModel.getCategoryMVmodletag().add(mvPin);
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * ???????????????????????????pv???mv???ff????????????????????????????????????????????????1???????????????????????????
     * ?????????????????????????????????????????????pv??????????????????mv???pv???????????????ff
     * maskRunnablePVMatrix  maskRunnableMVMatrix  maskRunnableFFMatrix maskBaseMapPvEffectMvMatrix
     */
    private void initRunnableMatrixAndBaseMapMatrix(MpcModel mpcModel) {
        for (int indexpv = 0; indexpv < mpcModel.getCategoryPVmodletag().size(); ++indexpv) {


            /**1\marker total pvusemv
             * 2\marker participate mv
             * */
            for (int indexmv = 0; indexmv < mpcModel.getCategoryMVmodletag().size(); ++indexmv) {
                ResponTimeSerise ismapping = isPVMappingMV(mpcModel, mpcModel.getCategoryPVmodletag().get(indexpv).getModlePinName(), mpcModel.getCategoryMVmodletag().get(indexmv).getModlePinName());
                mpcModel.getMaskBaseMapPvUseMvMatrix()[indexpv][indexmv] = (null != ismapping ? 1 : 0);
                mpcModel.getMaskBaseMapPvEffectMvMatrix()[indexpv][indexmv] = (null != ismapping ? ismapping.getEffectRatio() : 0f);


                /**pv???????????????????????????????????????*/
                if ((null != ismapping) && isThisTimeRunnablePin(mpcModel.getCategoryPVmodletag().get(indexpv))) {
                    mpcModel.getMaskisRunnablePVMatrix()[indexpv] = 1;
                }

                /**1????????????????????????2???pv???????????? 3mv????????????*/
                if ((null != ismapping) && isThisTimeRunnablePin(mpcModel.getCategoryPVmodletag().get(indexpv)) && isThisTimeRunnablePin(mpcModel.getCategoryMVmodletag().get(indexmv))) {
                    mpcModel.getMaskisRunnableMVMatrix()[indexmv] = 1;
                }

            }

            /**1\marker total pvuseff
             * 2\marker participate ff
             * */
            for (int indexff = 0; indexff < mpcModel.getCategoryFFmodletag().size(); ++indexff) {
                ResponTimeSerise ismapping = isPVMappingFF(mpcModel, mpcModel.getCategoryPVmodletag().get(indexpv).getModlePinName(), mpcModel.getCategoryFFmodletag().get(indexff).getModlePinName());
                mpcModel.getMaskBaseMapPvUseFfMatrix()[indexpv][indexff] = (null != ismapping ? 1 : 0);
                if ((null != ismapping) && isThisTimeRunnablePin(mpcModel.getCategoryPVmodletag().get(indexpv)) && isThisTimeRunnablePin(mpcModel.getCategoryFFmodletag().get(indexff))) {
                    mpcModel.getMaskisRunnableFFMatrix()[indexff] = 1;
                }
            }
        }
    }

    /**
     * ???????????????????????????????????????????????????
     */
    private boolean isThisTimeRunnablePin(MpcModelProperty pin) {
        /**?????????????????????????????????*/
        return 1 == pin.getPinEnable() && (pin.isThisTimeParticipate());
    }


    /**
     * pv???mv?????????????????????
     **/
    private ResponTimeSerise isPVMappingMV(MpcModel mpcModel, String pvpin, String mvpin) {

        for (ResponTimeSerise responTimeSerise : mpcModel.getResponTimeSeriseList()) {
            if (
                    responTimeSerise.getInputPins().equals(mvpin)
                            &&
                            responTimeSerise.getOutputPins().equals(pvpin)
            ) {
                return responTimeSerise;
            }
        }
        return null;
    }

    /**
     * pv???ff?????????????????????
     */
    private ResponTimeSerise isPVMappingFF(MpcModel mpcModel, String pvpin, String ffpin) {

        for (ResponTimeSerise responTimeSerise : mpcModel.getResponTimeSeriseList()) {
            if (
                    responTimeSerise.getInputPins().equals(ffpin)
                            &&
                            responTimeSerise.getOutputPins().equals(pvpin)
            ) {
                return responTimeSerise;
            }

        }
        return null;
    }


    /**
     * 1??????????????????runnbale???pv
     * 2??????runnbale???pv??? runnbale mv?????????
     * 3????????????runnbale???pv???runnbale ff?????????
     */
    private void initStatisticRunnablePinNum(MpcModel mpcModel) {

        /**??????runnbale???pv???mv?????????*/
        for (int pvi : mpcModel.getMaskisRunnablePVMatrix()) {
            if (1 == pvi) {
                mpcModel.setNumOfRunnablePVPins_pp(mpcModel.getNumOfRunnablePVPins_pp() + 1);
            }
        }

        /**??????runnbale???pv???mv?????????*/
        for (int mvi : mpcModel.getMaskisRunnableMVMatrix()) {
            if (1 == mvi) {
                mpcModel.setNumOfRunnableMVpins_mm(mpcModel.getNumOfRunnableMVpins_mm() + 1);
            }
        }
        /**??????runnbale???pv???ff?????????*/
        for (int ffi : mpcModel.getMaskisRunnableFFMatrix()) {
            if (1 == ffi) {
                mpcModel.setNumOfRunnableFFpins_vv(mpcModel.getNumOfRunnableFFpins_vv() + 1);
            }
        }

    }


    /**
     * ?????????pv???????????????
     * Q???????????????
     * alpheTrajectoryCoefficients??????????????????
     * deadZones??????
     * funelinitvalues???????????????
     * funneltype????????????
     **/

    private void initPVparams(MpcModel mpcModel) {

        List<MpcModelProperty> runablePVPins = getRunablePins(mpcModel.getCategoryPVmodletag(), mpcModel.getMaskisRunnablePVMatrix());
        List<MpcModelProperty> runableMVPins = getRunablePins(mpcModel.getCategoryMVmodletag(), mpcModel.getMaskisRunnableMVMatrix());

        int looppv = 0;
        for (MpcModelProperty runpvpin : runablePVPins) {
            mpcModel.getQ()[looppv] = runpvpin.getQ();
            mpcModel.getAlpheTrajectoryCoefficients()[looppv] = runpvpin.getReferTrajectoryCoef();
            mpcModel.getAlpheTrajectoryCoefmethod()[looppv] = runpvpin.getTracoefmethod();
            mpcModel.getDeadZones()[looppv] = runpvpin.getDeadZone();
            mpcModel.getFunelinitvalues()[looppv] = runpvpin.getFunelinitValue();
            double[] fnl = new double[2];
            if (runpvpin.getFunneltype() != null) {
                switch (runpvpin.getFunneltype()) {
                    case MPC_FUNNEL_FULL:
                        fnl[0] = 0d;
                        fnl[1] = 0d;
                        mpcModel.getFunneltype()[looppv] = fnl;
                        break;
                    case MPC_FUNNEL_UP:
                        fnl[0] = 0;
                        //????????????
                        fnl[1] = 1;
                        mpcModel.getFunneltype()[looppv] = fnl;
                        break;
                    case MPC_FUNNEL_DOWN:
                        //????????????
                        fnl[0] = 1;
                        fnl[1] = 0;
                        mpcModel.getFunneltype()[looppv] = fnl;
                        break;
                    default:
                        //?????????
                        fnl[0] = 0;
                        fnl[1] = 0;
                        mpcModel.getFunneltype()[looppv] = fnl;
                }
            } else {
                //???????????????????????????
                fnl[0] = 0;
                fnl[1] = 0;
                mpcModel.getFunneltype()[looppv] = fnl;
            }


            int loopmv = 0;
            for (MpcModelProperty runmvpin : runableMVPins) {

                /**??????????????????*/
                ResponTimeSerise responTimeSerisePVMV = isPVMappingMV(mpcModel, runpvpin.getModlePinName(), runmvpin.getModlePinName());
                if (responTimeSerisePVMV != null) {
                    mpcModel.getA_RunnabletimeseriseMatrix()[looppv][loopmv] = responTimeSerisePVMV.responOneTimeSeries(mpcModel.getTimeserise_N(), mpcModel.getControlAPCOutCycle());
                    mpcModel.getMaskMatrixRunnablePVUseMV()[looppv][loopmv] = 1;
                    mpcModel.getMaskMatrixRunnablePvEffectMv()[looppv][loopmv] = responTimeSerisePVMV.getEffectRatio();
                    mpcModel.getRunnableintegrationInc_mv()[looppv][loopmv] = responTimeSerisePVMV.getStepRespJson().getDouble("Ki");
                }

                ++loopmv;
            }

            ++looppv;
        }

    }


    /**
     * ??????????????????pv???mv?????????????????????
     */
    private void accumulativeNumOfRunnablePVMVMaping(MpcModel mpcModel) {
        for (int p = 0; p < mpcModel.getNumOfRunnablePVPins_pp(); ++p) {
            for (int m = 0; m < mpcModel.getNumOfRunnableMVpins_mm(); ++m) {
                if (1 == mpcModel.getMaskMatrixRunnablePVUseMV()[p][m]) {
//                    simulatControlModle.addNumOfIOMappingRelation();
                }
            }

        }
    }


    private void initRMatrix(MpcModel mpcModel) {
        int indevEnableMV = 0;
        for (MpcModelProperty runmv : getRunablePins(mpcModel.getCategoryMVmodletag(), mpcModel.getMaskisRunnableMVMatrix())) {
            mpcModel.getR()[indevEnableMV] = runmv.getR();
            ++indevEnableMV;
        }
    }

    private void initFFparams(MpcModel mpcModel) {

        List<MpcModelProperty> runablePVPins = getRunablePins(mpcModel.getCategoryPVmodletag(), mpcModel.getMaskisRunnablePVMatrix());//??????????????????pv??????
        List<MpcModelProperty> runableFFPins = getRunablePins(mpcModel.getCategoryFFmodletag(), mpcModel.getMaskisRunnableFFMatrix());//??????????????????ff??????

        int looppv = 0;
        for (MpcModelProperty runpv : runablePVPins) {

            int loopff = 0;
            for (MpcModelProperty runff : runableFFPins) {

                ResponTimeSerise responTimeSerisePVFF = isPVMappingFF(mpcModel, runpv.getModlePinName(), runff.getModlePinName());

                if (responTimeSerisePVFF != null) {
                    mpcModel.getB_RunnabletimeseriseMatrix()[looppv][loopff] = responTimeSerisePVFF.responOneTimeSeries(mpcModel.getTimeserise_N(), mpcModel.getControlAPCOutCycle());
                    mpcModel.getMaskMatrixRunnablePVUseFF()[looppv][loopff] = 1;
                    mpcModel.getRunnableintegrationInc_ff()[looppv][loopff] = responTimeSerisePVFF.getStepRespJson().getDouble("Ki");
                }
                ++loopff;
            }
            ++looppv;
        }
    }


    /***
     * ?????????mpc
     * */
    @Override
    public BaseModelImp convertModel(Adapter adapter) {
        DmcModleAdapter dmcModleAdapter = (DmcModleAdapter) adapter;
        DmcBasemodleparam dmcBasemodleparam = dmcModleAdapter.getBasemodelparam();
        MpcModel mpc = new MpcModel();
        mpc.setModleEnable(1);
        mpc.setModleName(dmcBasemodleparam.getModelname());
        mpc.setModletype(AlgorithmName.MPC_MODEL.getCode());
        mpc.setRefprojectid(-1);
        mpc.setPredicttime_P(Integer.parseInt(dmcBasemodleparam.getPredicttime_P()));
        mpc.setTimeserise_N(Integer.parseInt(dmcBasemodleparam.getTimeserise_N()));
        mpc.setControltime_M(Integer.parseInt(dmcBasemodleparam.getControltime_M()));
        mpc.setRunstyle(dmcModleAdapter.getBasemodelparam().getRunstyle());
        mpc.setModleId(dmcBasemodleparam.getModelid());
        mpc.setControlAPCOutCycle(Double.valueOf(dmcBasemodleparam.getControlapcoutcycle()).intValue());

        mpc.setPropertyImpList(new ArrayList<>());
        mpc.setResponTimeSeriseList(new ArrayList<>());
        /*auto*/
        MpcModelProperty auto = new MpcModelProperty();
        auto.setModlePinName(AlgorithmModelProperty.MODEL_PROPERTY_MODELAUTO.getCode());
        auto.setOpcTagName(AlgorithmModelProperty.MODEL_PROPERTY_MODELAUTO.getCode() + dmcBasemodleparam.getModelid());
        auto.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_MPC.getCode());
        auto.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
        auto.setRefmodleId(dmcBasemodleparam.getModelid());
        auto.setPinEnable(1);
        JSONObject auto_resource = new JSONObject();
        auto_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
        auto_resource.put("value", dmcBasemodleparam.getAuto());
        auto.setResource(auto_resource);
        mpc.getPropertyImpList().add(auto);


        /*pv*/
        for (Pvparam pvparam : dmcModleAdapter.getPv()) {

            MpcModelProperty pvpinmpcModleProperty = new MpcModelProperty();
            pvpinmpcModleProperty.setRefmodleId(dmcBasemodleparam.getModelid());
            pvpinmpcModleProperty.setModlePinName(pvparam.getPvpinname());
            pvpinmpcModleProperty.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
            pvpinmpcModleProperty.setPintype(AlgorithmModelProperty.MODEL_PROPERTY_PV.getCode());
            pvpinmpcModleProperty.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_MPC.getCode());
            pvpinmpcModleProperty.setOpcTagName(pvparam.getPvpinname() + dmcBasemodleparam.getModelid());//spmodleOpcTag
            pvpinmpcModleProperty.setModleOpcTag(pvparam.getPvpinname() + dmcBasemodleparam.getModelid());
            JSONObject pv_resource = new JSONObject();
            pv_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
            pv_resource.put("value", pvparam.getPvpinvalue());
            pvpinmpcModleProperty.setResource(pv_resource);
            pvpinmpcModleProperty.setOpcTagName("");

            pvpinmpcModleProperty.setDeadZone(pvparam.getDeadzone());
            pvpinmpcModleProperty.setFunelinitValue(pvparam.getFunelinitvalue());

            pvpinmpcModleProperty.setFunneltype(AlgorithmMpcFunnelType.getCodeMap().get(pvparam.getFunneltype()));
            pvpinmpcModleProperty.setQ(pvparam.getQ());
            pvpinmpcModleProperty.setReferTrajectoryCoef(pvparam.getRefertrajectorycoef());
            pvpinmpcModleProperty.setTracoefmethod(pvparam.getTracoefmethod());
            mpc.getPropertyImpList().add(pvpinmpcModleProperty);

            int pinorder = 0;
            Matcher pvmatch = pvpattern.matcher(pvparam.getPvpinname());
            if (pvmatch.find()) {
                pinorder = Integer.parseInt(pvmatch.group(2));
            } else {
                throw new RuntimeException("can't match pin order");
            }
            if (pvparam.getPvuppinvalue() != null) {
                MpcModelProperty pvuppinmpcModleProperty = new MpcModelProperty();
                pvuppinmpcModleProperty.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
                pvuppinmpcModleProperty.setRefmodleId(dmcBasemodleparam.getModelid());
                pvuppinmpcModleProperty.setModlePinName(AlgorithmModelProperty.MODEL_PROPERTY_PVUP.getCode() + pinorder);
                pvuppinmpcModleProperty.setPintype(AlgorithmModelProperty.MODEL_PROPERTY_PVUP.getCode());
                pvuppinmpcModleProperty.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_MPC.getCode());
                pvuppinmpcModleProperty.setOpcTagName("");//spmodleOpcTag
                pvuppinmpcModleProperty.setModleOpcTag("");

                JSONObject pvup_resource = new JSONObject();
                pvup_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
                pvup_resource.put("value", pvparam.getPvuppinvalue());
                pvuppinmpcModleProperty.setOpcTagName("");
                pvuppinmpcModleProperty.setResource(pvup_resource);
                mpc.getPropertyImpList().add(pvuppinmpcModleProperty);

                pvpinmpcModleProperty.setUpLmt(pvuppinmpcModleProperty);
            }


            if (pvparam.getPvdownpinvalue() != null) {
                MpcModelProperty pvdownpinmpcModleProperty = new MpcModelProperty();
                pvdownpinmpcModleProperty.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
                pvdownpinmpcModleProperty.setRefmodleId(dmcBasemodleparam.getModelid());
                pvdownpinmpcModleProperty.setModlePinName(AlgorithmModelProperty.MODEL_PROPERTY_PVDOWN.getCode() + pinorder);
                pvdownpinmpcModleProperty.setPintype(AlgorithmModelProperty.MODEL_PROPERTY_PVDOWN.getCode());
                pvdownpinmpcModleProperty.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_MPC.getCode());
                pvdownpinmpcModleProperty.setOpcTagName("");//spmodleOpcTag
                pvdownpinmpcModleProperty.setModleOpcTag("");


                JSONObject pvdown_resource = new JSONObject();
                pvdown_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
                pvdown_resource.put("value", pvparam.getPvdownpinvalue());
                pvdownpinmpcModleProperty.setResource(pvdown_resource);
                pvdownpinmpcModleProperty.setOpcTagName("");
                mpc.getPropertyImpList().add(pvdownpinmpcModleProperty);

                pvpinmpcModleProperty.setDownLmt(pvdownpinmpcModleProperty);
            }


            MpcModelProperty sppinmpcModleProperty = new MpcModelProperty();
            sppinmpcModleProperty.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
            sppinmpcModleProperty.setRefmodleId(dmcBasemodleparam.getModelid());
            sppinmpcModleProperty.setModlePinName(AlgorithmModelProperty.MODEL_PROPERTY_SP.getCode() + pinorder);
            sppinmpcModleProperty.setPintype(AlgorithmModelProperty.MODEL_PROPERTY_SP.getCode());
            sppinmpcModleProperty.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_MPC.getCode());
            sppinmpcModleProperty.setOpcTagName("");//spmodleOpcTag
            sppinmpcModleProperty.setModleOpcTag("");


            JSONObject sp_resource = new JSONObject();
            sp_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
            sp_resource.put("value", pvparam.getSppinvalue());
            sppinmpcModleProperty.setResource(sp_resource);
            mpc.getPropertyImpList().add(sppinmpcModleProperty);

        }

//mv
        for (Mvparam mvparam : dmcModleAdapter.getMv()) {
            MpcModelProperty mvpinmpcModleProperty = new MpcModelProperty();
            mvpinmpcModleProperty.setRefmodleId(dmcBasemodleparam.getModelid());
            mvpinmpcModleProperty.setModlePinName(mvparam.getMvpinname());
            mvpinmpcModleProperty.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
            mvpinmpcModleProperty.setPintype(AlgorithmModelProperty.MODEL_PROPERTY_MV.getCode());
            mvpinmpcModleProperty.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_MPC.getCode());
            mvpinmpcModleProperty.setOpcTagName("");
            mvpinmpcModleProperty.setModleOpcTag("");
            JSONObject mv_resource = new JSONObject();
            mv_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
            mv_resource.put("value", mvparam.getMvpinvalue());
            mvpinmpcModleProperty.setResource(mv_resource);
            mvpinmpcModleProperty.setR(mvparam.getR());
            mvpinmpcModleProperty.setDmvHigh(mvparam.getDmvhigh());
            mvpinmpcModleProperty.setDmvLow(mvparam.getDmvlow());
            mpc.getPropertyImpList().add(mvpinmpcModleProperty);


            int pinorder = 0;
            Matcher pvmatch = mvpattern.matcher(mvparam.getMvpinname());
            if (pvmatch.find()) {
                pinorder = Integer.parseInt(pvmatch.group(2));
            } else {
                throw new RuntimeException("can't match pin order");
            }


            MpcModelProperty mvuppinmpcModleProperty = new MpcModelProperty();
            mvuppinmpcModleProperty.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
            mvuppinmpcModleProperty.setRefmodleId(dmcBasemodleparam.getModelid());
            mvuppinmpcModleProperty.setModlePinName(AlgorithmModelProperty.MODEL_PROPERTY_MVUP.getCode() + pinorder);
            mvuppinmpcModleProperty.setPintype(AlgorithmModelProperty.MODEL_PROPERTY_MVUP.getCode());
            mvuppinmpcModleProperty.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_MPC.getCode());

            mvuppinmpcModleProperty.setOpcTagName("");//spmodleOpcTag
            mvuppinmpcModleProperty.setModleOpcTag("");


            JSONObject mvup_resource = new JSONObject();
            mvup_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
            mvup_resource.put("value", mvparam.getMvuppinvalue());
            mvuppinmpcModleProperty.setResource(mvup_resource);
            mpc.getPropertyImpList().add(mvuppinmpcModleProperty);


            MpcModelProperty mvdownpinmpcModleProperty = new MpcModelProperty();
            mvdownpinmpcModleProperty.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
            mvdownpinmpcModleProperty.setRefmodleId(dmcBasemodleparam.getModelid());
            mvdownpinmpcModleProperty.setModlePinName(AlgorithmModelProperty.MODEL_PROPERTY_MVDOWN.getCode() + pinorder);
            mvdownpinmpcModleProperty.setPintype(AlgorithmModelProperty.MODEL_PROPERTY_MVDOWN.getCode());
            mvdownpinmpcModleProperty.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_MPC.getCode());
            mvdownpinmpcModleProperty.setOpcTagName("");//spmodleOpcTag
            mvdownpinmpcModleProperty.setModleOpcTag("");
            JSONObject mvdown_resource = new JSONObject();
            mvdown_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
            mvdown_resource.put("value", mvparam.getMvdownpinvalue());
            mvdownpinmpcModleProperty.setResource(mvdown_resource);
            mpc.getPropertyImpList().add(mvdownpinmpcModleProperty);


            MpcModelProperty mvfbpinmpcModleProperty = new MpcModelProperty();
            mvfbpinmpcModleProperty.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
            mvfbpinmpcModleProperty.setRefmodleId(dmcBasemodleparam.getModelid());
            mvfbpinmpcModleProperty.setModlePinName(AlgorithmModelProperty.MODEL_PROPERTY_MVFB.getCode() + pinorder);
            mvfbpinmpcModleProperty.setPintype(AlgorithmModelProperty.MODEL_PROPERTY_MVFB.getCode());
            mvfbpinmpcModleProperty.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_MPC.getCode());
            mvfbpinmpcModleProperty.setOpcTagName("");//mvfbmodleOpcTag
            mvfbpinmpcModleProperty.setModleOpcTag("");
            JSONObject mvfb_resource = new JSONObject();
            mvfb_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
            mvfb_resource.put("value", mvparam.getMvfbpinvalue());
            mvfbpinmpcModleProperty.setResource(mvfb_resource);
            mpc.getPropertyImpList().add(mvfbpinmpcModleProperty);


        }
//ff
        for (Ffparam ffparam : dmcModleAdapter.getFf()) {
            MpcModelProperty ffpinmpcModleProperty = new MpcModelProperty();
            ffpinmpcModleProperty.setRefmodleId(dmcBasemodleparam.getModelid());
            ffpinmpcModleProperty.setModlePinName(ffparam.getFfpinname());
            ffpinmpcModleProperty.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
            ffpinmpcModleProperty.setPintype(AlgorithmModelProperty.MODEL_PROPERTY_FF.getCode());
            ffpinmpcModleProperty.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_MPC.getCode());
            ffpinmpcModleProperty.setOpcTagName("");
            ffpinmpcModleProperty.setModleOpcTag("");
            JSONObject ffresource = new JSONObject();
            ffresource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
            ffresource.put("value", ffparam.getFfpinvalue());
            ffpinmpcModleProperty.setResource(ffresource);
            ffpinmpcModleProperty.setOpcTagName("");
            mpc.getPropertyImpList().add(ffpinmpcModleProperty);

            int pinorder = 0;
            Matcher pvmatch = ffpattern.matcher(ffparam.getFfpinname());
            if (pvmatch.find()) {
                pinorder = Integer.parseInt(pvmatch.group(2));
            } else {
                throw new RuntimeException("can't match pin order");
            }


            if (ffparam.getFfuppinvalue() != null) {
                MpcModelProperty ffuppinmpcModleProperty = new MpcModelProperty();
                ffuppinmpcModleProperty.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
                ffuppinmpcModleProperty.setRefmodleId(dmcBasemodleparam.getModelid());
                ffuppinmpcModleProperty.setModlePinName(AlgorithmModelProperty.MODEL_PROPERTY_FFUP.getCode() + pinorder);
                ffuppinmpcModleProperty.setPintype(AlgorithmModelProperty.MODEL_PROPERTY_FFUP.getCode());
                ffuppinmpcModleProperty.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_MPC.getCode());
                ffuppinmpcModleProperty.setOpcTagName("");//spmodleOpcTag
                ffuppinmpcModleProperty.setModleOpcTag("");

                JSONObject ffupresource = new JSONObject();
                ffupresource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
                ffupresource.put("value", ffparam.getFfuppinvalue());
                ffuppinmpcModleProperty.setResource(ffupresource);
                mpc.getPropertyImpList().add(ffuppinmpcModleProperty);

                ffpinmpcModleProperty.setUpLmt(ffuppinmpcModleProperty);

            }

            if (ffparam.getFfdownpinvalue() != null) {
                MpcModelProperty ffdownpinmpcModleProperty = new MpcModelProperty();
                ffdownpinmpcModleProperty.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
                ffdownpinmpcModleProperty.setRefmodleId(dmcBasemodleparam.getModelid());
                ffdownpinmpcModleProperty.setModlePinName(AlgorithmModelProperty.MODEL_PROPERTY_FFDOWN.getCode() + pinorder);
                ffdownpinmpcModleProperty.setPintype(AlgorithmModelProperty.MODEL_PROPERTY_FFDOWN.getCode());
                ffdownpinmpcModleProperty.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_MPC.getCode());
                ffdownpinmpcModleProperty.setOpcTagName("");//spmodleOpcTag
                ffdownpinmpcModleProperty.setModleOpcTag("");

                JSONObject ffdownresource = new JSONObject();
                ffdownresource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
                ffdownresource.put("value", ffparam.getFfdownpinvalue());
                ffdownpinmpcModleProperty.setResource(ffdownresource);
                mpc.getPropertyImpList().add(ffdownpinmpcModleProperty);

                ffpinmpcModleProperty.setDownLmt(ffdownpinmpcModleProperty);
            }


        }
        //respon

        for (DmcResponparam dmcResponparam : dmcModleAdapter.getModel()) {
            long modleid = dmcBasemodleparam.getModelid();
            int responid = -1;
            String inputpinName = dmcResponparam.getInputpinname();
            String outputpinName = dmcResponparam.getOutputpinname();
            double K = dmcResponparam.getK();
            double T = dmcResponparam.getT();
            double Tau = dmcResponparam.getTau();
            float effectRatio = 1.0f;
            double Ki = dmcResponparam.getKi();
            ResponTimeSerise respontimeserise;
            JSONObject jsonres;

            respontimeserise = new ResponTimeSerise();

            respontimeserise.setInputPins(inputpinName);
            respontimeserise.setOutputPins(outputpinName);
            respontimeserise.setRefrencemodleId(modleid);
//            respontimeserise.setModletagId(responid.equals("") ? -1 : Integer.valueOf(responid));
            jsonres = new JSONObject();
            jsonres.put("k", K);
            jsonres.put("t", T);
            jsonres.put("tao", Tau);
            jsonres.put("Ki", Ki);
            respontimeserise.setStepRespJson(jsonres);
            respontimeserise.setEffectRatio(effectRatio);
            mpc.getResponTimeSeriseList().add(respontimeserise);

        }

        /*output pin*/

        for (DmcOutproperty dmcOutparam : dmcModleAdapter.getOutputparam()) {
            MpcModelProperty propertyImp = new MpcModelProperty();
            propertyImp.setRefmodleId(dmcBasemodleparam.getModelid());
            propertyImp.setModleOpcTag("");
            propertyImp.setModlePinName(dmcOutparam.getOutputpinname());
            propertyImp.setOpcTagName("");

            //?????????
            JSONObject resource = new JSONObject();
            resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_MEMORY.getCode());
            propertyImp.setResource(resource);
            propertyImp.setPindir(AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_OUTPUT.getCode());
            propertyImp.setModlepropertyclazz(AlgorithmModelPropertyClazz.MODEL_PROPERTY_CLAZZ_MPC.getCode());
            mpc.getPropertyImpList().add(propertyImp);
        }


        return mpc;
    }


    /**
     * ??????mpc????????????
     */

    private void updatemodlevalue(DmcModleAdapter dmcModleAdapter, MpcModel mpc) {

        DmcBasemodleparam dmcBasemodleparam = dmcModleAdapter.getBasemodelparam();


        BaseModelProperty auto = selectModelProperyByPinname(AlgorithmModelProperty.MODEL_PROPERTY_MODELAUTO.getCode(), mpc.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
        JSONObject auto_resource = new JSONObject();
        auto_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
        auto_resource.put("value", dmcBasemodleparam.getAuto());
        auto.setResource(auto_resource);

        /*pv*/
        for (Pvparam pvparam : dmcModleAdapter.getPv()) {

            MpcModelProperty pvpinmpcModleProperty = (MpcModelProperty) selectModelProperyByPinname(pvparam.getPvpinname(), mpc.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());

            JSONObject pv_resource = new JSONObject();
            pv_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
            pv_resource.put("value", pvparam.getPvpinvalue());
            pvpinmpcModleProperty.setResource(pv_resource);
            pvpinmpcModleProperty.setOpcTagName("");

            pvpinmpcModleProperty.setDeadZone(pvparam.getDeadzone());
            pvpinmpcModleProperty.setFunelinitValue(pvparam.getFunelinitvalue());

            pvpinmpcModleProperty.setFunneltype(AlgorithmMpcFunnelType.getCodeMap().get(pvparam.getFunneltype()));
            pvpinmpcModleProperty.setQ(pvparam.getQ());
            pvpinmpcModleProperty.setReferTrajectoryCoef(pvparam.getRefertrajectorycoef());
            pvpinmpcModleProperty.setTracoefmethod(pvparam.getTracoefmethod());

            int pinorder = 0;
            Matcher pvmatch = pvpattern.matcher(pvparam.getPvpinname());
            if (pvmatch.find()) {
                pinorder = Integer.parseInt(pvmatch.group(2));
            } else {
                throw new RuntimeException("can't match pin order");
            }


            MpcModelProperty pvuppinmpcModleProperty = (MpcModelProperty) selectModelProperyByPinname(AlgorithmModelProperty.MODEL_PROPERTY_PVUP.getCode() + pinorder, mpc.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
            if (pvuppinmpcModleProperty != null) {
                JSONObject pvup_resource = new JSONObject();
                pvup_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
                pvup_resource.put("value", pvparam.getPvuppinvalue());
                pvuppinmpcModleProperty.setOpcTagName("");
                pvuppinmpcModleProperty.setResource(pvup_resource);
            }

            MpcModelProperty pvdownpinmpcModleProperty = (MpcModelProperty) selectModelProperyByPinname(AlgorithmModelProperty.MODEL_PROPERTY_PVDOWN.getCode() + pinorder, mpc.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
            if (pvdownpinmpcModleProperty != null) {
                JSONObject pvdown_resource = new JSONObject();
                pvdown_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
                pvdown_resource.put("value", pvparam.getPvdownpinvalue());
                pvdownpinmpcModleProperty.setResource(pvdown_resource);
                pvdownpinmpcModleProperty.setOpcTagName("");
            }


            MpcModelProperty sppinmpcModleProperty = (MpcModelProperty) selectModelProperyByPinname(AlgorithmModelProperty.MODEL_PROPERTY_SP.getCode() + pinorder, mpc.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
            JSONObject sp_resource = new JSONObject();
            sp_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
            sp_resource.put("value", pvparam.getSppinvalue());
            sppinmpcModleProperty.setResource(sp_resource);

        }

//mv
        for (Mvparam mvparam : dmcModleAdapter.getMv()) {
            MpcModelProperty mvpinmpcModleProperty = (MpcModelProperty) selectModelProperyByPinname(mvparam.getMvpinname(), mpc.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
            JSONObject mv_resource = new JSONObject();
            mv_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
            mv_resource.put("value", mvparam.getMvpinvalue());
            mvpinmpcModleProperty.setResource(mv_resource);
            mvpinmpcModleProperty.setR(mvparam.getR());
            mvpinmpcModleProperty.setDmvHigh(mvparam.getDmvhigh());
            mvpinmpcModleProperty.setDmvLow(mvparam.getDmvlow());


            int pinorder = 0;
            Matcher pvmatch = mvpattern.matcher(mvparam.getMvpinname());
            if (pvmatch.find()) {
                pinorder = Integer.parseInt(pvmatch.group(2));
            } else {
                throw new RuntimeException("can't match pin order");
            }


            MpcModelProperty mvuppinmpcModleProperty = (MpcModelProperty) selectModelProperyByPinname(AlgorithmModelProperty.MODEL_PROPERTY_MVUP.getCode() + pinorder, mpc.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
            JSONObject mvup_resource = new JSONObject();
            mvup_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
            mvup_resource.put("value", mvparam.getMvuppinvalue());
            mvuppinmpcModleProperty.setResource(mvup_resource);


            MpcModelProperty mvdownpinmpcModleProperty = (MpcModelProperty) selectModelProperyByPinname(AlgorithmModelProperty.MODEL_PROPERTY_MVDOWN.getCode() + pinorder, mpc.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
            JSONObject mvdown_resource = new JSONObject();
            mvdown_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
            mvdown_resource.put("value", mvparam.getMvdownpinvalue());
            mvdownpinmpcModleProperty.setResource(mvdown_resource);


            MpcModelProperty mvfbpinmpcModleProperty = (MpcModelProperty) selectModelProperyByPinname(AlgorithmModelProperty.MODEL_PROPERTY_MVFB.getCode() + pinorder, mpc.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
            JSONObject mvfb_resource = new JSONObject();
            mvfb_resource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
            mvfb_resource.put("value", mvparam.getMvfbpinvalue());
            mvfbpinmpcModleProperty.setResource(mvfb_resource);
        }
//ff
        for (Ffparam ffparam : dmcModleAdapter.getFf()) {

            MpcModelProperty ffpinmpcModleProperty = (MpcModelProperty) selectModelProperyByPinname(ffparam.getFfpinname(), mpc.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());

            JSONObject ffresource = new JSONObject();
            ffresource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
            ffresource.put("value", ffparam.getFfpinvalue());
            ffpinmpcModleProperty.setResource(ffresource);
            ffpinmpcModleProperty.setOpcTagName("");

            int pinorder = 0;
            Matcher pvmatch = ffpattern.matcher(ffparam.getFfpinname());
            if (pvmatch.find()) {
                pinorder = Integer.parseInt(pvmatch.group(2));
            } else {
                throw new RuntimeException("can't match pin order");
            }


            MpcModelProperty ffuppinmpcModleProperty = (MpcModelProperty) selectModelProperyByPinname(AlgorithmModelProperty.MODEL_PROPERTY_FFUP.getCode() + pinorder, mpc.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
            if (ffuppinmpcModleProperty != null) {
                JSONObject ffupresource = new JSONObject();
                ffupresource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
                ffupresource.put("value", ffparam.getFfuppinvalue());
                ffuppinmpcModleProperty.setResource(ffupresource);
            }


            MpcModelProperty ffdownpinmpcModleProperty = (MpcModelProperty) selectModelProperyByPinname(AlgorithmModelProperty.MODEL_PROPERTY_FFDOWN.getCode() + pinorder, mpc.getPropertyImpList(), AlgorithmModelPropertyDir.MODEL_PROPERTYDIR_INPUT.getCode());
            if (ffdownpinmpcModleProperty != null) {
                JSONObject ffdownresource = new JSONObject();
                ffdownresource.put("resource", AlgorithmValueFrom.ALGORITHM_VALUE_FROM_CONSTANT.getCode());
                ffdownresource.put("value", ffparam.getFfdownpinvalue());
                ffdownpinmpcModleProperty.setResource(ffdownresource);
            }

        }

    }


}
