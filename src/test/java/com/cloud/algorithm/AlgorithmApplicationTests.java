package com.cloud.algorithm;

import com.cloud.algorithm.model.dto.BaseModelResponseDto;
import com.cloud.algorithm.service.KeyGenerant;
import com.cloud.algorithm.service.rediz.RedizServiceImp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class AlgorithmApplicationTests implements KeyGenerant {


    @Autowired
    private RedizServiceImp redizServiceImp;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
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


    private void redisListInsert(String key){

        BaseModelResponseDto.builder().status(201).message("haha" + System.currentTimeMillis()).build();
        BaseModelResponseDto.builder().build();

        Long expiredTT=redisTemplate.opsForList().size(key);
        System.out.println("expired time="+expiredTT);
        Object o=redisTemplate.opsForList().leftPop(key);
        System.out.println("expired pop value="+o);
        Object o_1=redisTemplate.opsForList().index(key,0);
        System.out.println("expired pop value="+o_1);



        redisTemplate.opsForList().rightPush(key,BaseModelResponseDto.builder().status(201).message("haha" + System.currentTimeMillis()).build());
        redisTemplate.opsForList().rightPush(key,BaseModelResponseDto.builder().status(202).message("haha" + System.currentTimeMillis()).build());
        redisTemplate.opsForList().rightPush(key,BaseModelResponseDto.builder().status(203).message("haha" + System.currentTimeMillis()).build());
        redisTemplate.opsForList().rightPush(key,BaseModelResponseDto.builder().status(204).message("haha" + System.currentTimeMillis()).build());
        redisTemplate.expire(key,30*1000,TimeUnit.MILLISECONDS);
        Long expireTime=redisTemplate.opsForList().getOperations().getExpire(key);
        System.out.println(String.format("expireTime=%d second\n",expireTime));

        System.out.println("size="+redisTemplate.opsForList().size(key));
        while(redisTemplate.opsForList().size(key)>2){
            BaseModelResponseDto dto=(BaseModelResponseDto)redisTemplate.opsForList().leftPop(key);
            System.out.println(dto.toString());
        }
        System.out.println("size="+redisTemplate.opsForList().size(key));

        List<Object> allContext=redisTemplate.opsForList().range(key,0,-1);
        System.out.println("all context"+allContext);
        redisTemplate.opsForList().set(key,-1,BaseModelResponseDto.builder().status(205).message("haha" + System.currentTimeMillis()).build());
        allContext=redisTemplate.opsForList().range(key,-3,-1);
        System.out.println("all context"+allContext);

    }


    @Test
    public void  Testredis(){

//        LocalDateTime a = LocalDateTime.of(2012, 6, 30, 12, 00);
//        LocalDateTime b = LocalDateTime.of(2012, 7, 1, 12, 00);
//
//        System.out.println(Duration.between(b,a).getSeconds());;


//        Object src=BaseModelResponseDto.builder().status(201).message("haha" + System.currentTimeMillis()).build();
//        Object tag=BaseModelResponseDto.builder().build();
//
//        BeanUtils.copyProperties(tag,src);
//        System.out.println(tag);


        redisListInsert("list-test");
//        redisTemplate.opsForList().leftPop();
//        redisTemplate.expire()
    }



}
