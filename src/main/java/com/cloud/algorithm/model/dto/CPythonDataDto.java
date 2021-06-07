package com.cloud.algorithm.model.dto;

import lombok.Data;

import java.util.List;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 18:21
 */
@Data
public class CPythonDataDto  implements java.io.Serializable{
    private List<PinDataDto> mvData;
}
