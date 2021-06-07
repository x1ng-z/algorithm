package com.cloud.algorithm.model.dto.apcPlant.request.fpid;

import com.cloud.algorithm.model.dto.apcPlant.Adapter;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/3/9 9:22
 */
@Data
public class PidModleAdapter implements Adapter {
    @NotNull
    private PidBaseModleParam basemodelparam;
    @NotNull
    private PidInputproperty inputparam;
    @NotNull
    private List<PidOutPutproperty> outputparam;

}
