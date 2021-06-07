package com.cloud.algorithm.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/3 13:44
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BaseModelResponseDto  implements java.io.Serializable{
    private String message;
    private int status;
    private Object algorithmContext;
}
