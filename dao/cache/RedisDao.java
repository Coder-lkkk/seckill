package com.lkkk.seckill.dao.cache;

import com.lkkk.seckill.dao.SeckillDao;
import com.lkkk.seckill.pojo.Seckill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

public class RedisDao {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillDao seckillDao;

    /*根据id从缓存里查记录*/
    public Seckill getSeckill(long seckillId){
        if(redisTemplate == null){
            System.out.println("redisTemplate实例化失败");
        }
        return (Seckill) redisTemplate.opsForValue().get(Long.toString(seckillId));
    }
    /*将秒杀商品列表放进缓存*/
    public void  setSeckillList(List<Seckill> list){
        redisTemplate.opsForValue().set("seckillList",list);
    }
    /*从缓存里得到秒杀商品列表*/
    public List<Seckill> getSeckillList(){
/*        redisTemplate.opsForList().leftPush("seckillList",new Seckill(1000L,"iphone",100));
        System.out.println(redisTemplate.opsForList().leftPop("seckillList"));
        return null;*/
        return (List<Seckill>) redisTemplate.opsForValue().get("seckillList");
    }

    /*将单个秒杀商品的信息放进缓存*/
    public void setSeckill(Seckill seckill){
        if(redisTemplate == null){
            System.out.println("redisTemplate实例化失败");
        }
        redisTemplate.opsForValue().set(seckill.getSeckillId().toString(),seckill);
    }
}
