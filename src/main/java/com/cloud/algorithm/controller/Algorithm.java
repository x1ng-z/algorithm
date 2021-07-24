package com.cloud.algorithm.controller;

import com.alibaba.fastjson.JSON;
import com.cloud.algorithm.aop.RedisLockHandler;
import com.cloud.algorithm.constant.AlgorithmName;
import com.cloud.algorithm.model.BaseModelImp;
import com.cloud.algorithm.model.bean.controlmodel.MpcModel;
import com.cloud.algorithm.model.dto.BaseModelResponseDto;
import com.cloud.algorithm.model.dto.apcPlant.request.customize.PythonAdapter;
import com.cloud.algorithm.model.dto.apcPlant.request.dmc.DmcModleAdapter;
import com.cloud.algorithm.model.dto.apcPlant.request.fpid.PidModleAdapter;
import com.cloud.algorithm.service.AlgorithmModelSerice;
import com.cloud.algorithm.service.AlgorithmModelSericeImp;
import com.cloud.algorithm.service.Handle;
import com.cloud.algorithm.service.handle.MpcModelHandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/2 19:00
 */
@RequestMapping("/algorithm")
@RestController
@RefreshScope
@Slf4j
public class Algorithm {

    @Value("${isCache:false}")
    private Boolean iscahce;


    @Autowired
    private AlgorithmModelSericeImp algorithmModelSericeImp;

    @Autowired
    private AlgorithmModelSerice algorithmModelSerice;

    @PostMapping("/test")
    public ResponseEntity<String> test() {
        return new ResponseEntity<String>("iscahce:" + iscahce, HttpStatus.OK);
    }


    @RequestMapping(path = "/dmc/buildrun", consumes = "application/json")
    public ResponseEntity<String> dmc(@Valid @RequestBody DmcModleAdapter dmcModleAdapter) {
        log.debug("/dmc/buildrun" + JSON.toJSONString(dmcModleAdapter));

        Handle dmchandle = algorithmModelSericeImp.getMatchHandles(AlgorithmName.MPC_MODEL.getCode());

        BaseModelImp mpcModel = dmchandle.convertModel(dmcModleAdapter);

        BaseModelResponseDto baseModelResponseDto = algorithmModelSerice.run(mpcModel.getModleId(), AlgorithmName.MPC_MODEL.getCode(), mpcModel);//dmchandle.run(mpcModel);

        return new ResponseEntity<String>(JSON.toJSONString(baseModelResponseDto), HttpStatus.OK);


    }


    @RequestMapping(path = "/cpython/buildrun", consumes = "application/json")
    public ResponseEntity<String> cpython(@Valid @RequestBody PythonAdapter pythonAdapter) {
        log.debug("/cpython/buildrun" + JSON.toJSONString(pythonAdapter));

        Handle handle = algorithmModelSericeImp.getMatchHandles(AlgorithmName.CUSTOMIZE_MODEL.getCode());

        BaseModelImp model = handle.convertModel(pythonAdapter);

        BaseModelResponseDto baseModelResponseDto = algorithmModelSerice.run(model.getModleId(), AlgorithmName.CUSTOMIZE_MODEL.getCode(), model);

        return new ResponseEntity<String>(JSON.toJSONString(baseModelResponseDto), HttpStatus.OK);


    }


    @RequestMapping(path = "/fpid/buildrun", consumes = "application/json")
    public ResponseEntity<String> fpid(@Valid @RequestBody PidModleAdapter pidModleAdapter) {
        log.debug("/fpid/buildrun" + JSON.toJSONString(pidModleAdapter));

        Handle handle = algorithmModelSericeImp.getMatchHandles(AlgorithmName.PID_MODEL.getCode());

        BaseModelImp modle = handle.convertModel(pidModleAdapter);

        BaseModelResponseDto baseModelResponseDto = algorithmModelSerice.run(modle.getModleId(), AlgorithmName.PID_MODEL.getCode(), modle);//dmchandle.run(mpcModel);

        return new ResponseEntity<String>(JSON.toJSONString(baseModelResponseDto), HttpStatus.OK);

    }



    @RequestMapping(path = "/stop/{modleid}")
    public ResponseEntity<String> stop(@PathVariable("modleid") Long modelId, String code) {
        BaseModelResponseDto baseModelResponseDto = algorithmModelSerice.stop(modelId, code);
        return new ResponseEntity<String>(JSON.toJSONString(baseModelResponseDto), HttpStatus.OK);
    }

}
