package com.cloud.algorithm.model.dto.apcPlant.request.dmc;

import com.alibaba.fastjson.annotation.JSONField;
import com.cloud.algorithm.model.dto.apcPlant.Adapter;
import lombok.Data;


import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/3/8 12:02
 */
@Data
public class DmcModleAdapter implements Adapter {
    //    @JSONField(name = "basemodleparam")
    @NotNull
    private DmcBasemodleparam basemodelparam;

    @NotNull(message = "pv context is null")
    @JSONField(name = "pv")
    @Size(min = 1, message = "至少需要设置一个pv")
    private List<Pvparam> pv;

    @NotNull
    @JSONField(name = "mv")
    @Size(min = 1, message = "至少需要设置一个mv")
    private List<Mvparam> mv;


    @JSONField(name = "ff")
    private List<Ffparam> ff;

    @NotNull
    @JSONField(name = "model")
    @Size(min = 1, message = "至少需要设置一个模型")
    private List<DmcResponparam> model;


    @JSONField(name = "outputparam")
    private List<DmcOutproperty> outputparam;






}
