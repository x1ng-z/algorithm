package com.cloud.algorithm.model.dto.apcPlant.request.fpid;

import lombok.Data;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/3/9 9:21
 */
@Data
public class PidOutPutproperty {
    private String outputpinname;

    public String getOutputpinname() {
        return outputpinname;
    }

    public void setOutputpinname(String outputpinname) {
        this.outputpinname = outputpinname;
    }
}
