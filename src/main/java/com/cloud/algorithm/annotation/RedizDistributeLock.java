package com.cloud.algorithm.annotation;

import java.lang.annotation.*;

/**
 * @author zzx
 * @version 1.0
 * @date 2021/6/5 13:11
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD})
public @interface RedizDistributeLock {

}
