package com.cloud.algorithm;

import com.cloud.algorithm.model.dto.BaseModelResponseDto;
import com.cloud.algorithm.service.KeyGenerant;
import com.cloud.algorithm.service.rediz.RedizServiceImp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

@SpringBootTest
class AlgorithmApplicationTests implements KeyGenerant {


    @Autowired
    private RedizServiceImp redizServiceImp;

    @Autowired
    private com.cloud.algorithm.service.redizDistributeLock.DistributeLockImp DistributeLockImp;

    @Test
    void contextLoads() {

//        redizServiceImp.insert("test", BaseModelResponseDto.builder().status(200).message("haha").build());
        boolean lock = false;
        try {
            lock = DistributeLockImp.tryLock(generantAlgorithmLockRedisKey(1234L), 20000, 60000, TimeUnit.MILLISECONDS);
            if (lock) {
                redizServiceImp.insert(generantAlgorithmStatusRedisKey(1234L), BaseModelResponseDto.builder().status(200).message("haha" + System.currentTimeMillis()).build());
                TimeUnit.MILLISECONDS.sleep(60000);
//                redizServiceImp.delete("test");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (lock) {
                DistributeLockImp.Unlock(generantAlgorithmLockRedisKey(1234L));
            }

        }


    }

}
