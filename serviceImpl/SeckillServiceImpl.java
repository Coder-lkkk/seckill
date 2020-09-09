package com.lkkk.seckill.serviceImpl;

import com.lkkk.seckill.dao.SeckillDao;
import com.lkkk.seckill.dao.SuccessKilledDao;
import com.lkkk.seckill.dao.cache.RedisDao;
import com.lkkk.seckill.dto.Exposer;
import com.lkkk.seckill.dto.SeckillExecution;
import com.lkkk.seckill.enums.SeckillStateEnum;
import com.lkkk.seckill.exception.DuplicateKillException;
import com.lkkk.seckill.exception.SeckillClosedException;
import com.lkkk.seckill.exception.SeckillException;
import com.lkkk.seckill.pojo.Seckill;
import com.lkkk.seckill.pojo.SuccessKilled;
import com.lkkk.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.List;;

@Service
public class SeckillServiceImpl implements SeckillService{

    //日志对象
    private Logger logger= LoggerFactory.getLogger(this.getClass());
    //加入一个混淆字符串(秒杀接口)的salt，为了我避免用户猜出我们的md5值，值任意给，越复杂越好
    private final String salt="shsdssljdd'l.";

    //注入Service依赖
    @Autowired //@Resource
    private SeckillDao seckillDao;

    @Autowired
    private RedisDao redisDao;

    @Autowired //@Resource
    private SuccessKilledDao successKilledDao;

    public List<Seckill> getSeckillList() {
        //先从缓存查
        List<Seckill> list = redisDao.getSeckillList();
        if(list==null){
            //缓存中没有就从数据库查
            //这里写死了只查四条，后面可以根据分页的数据来查
            list = seckillDao.queryAll(0,4);
            if(list.isEmpty()){
                System.out.println("缓存中没有商品列表");
            }else{
                //将数据库查出的的列表放入缓存
                redisDao.setSeckillList(list);
            }
        }
        return list;
    }

    public Seckill getById(long seckillId) {
        //先从缓存查
        Seckill seckill = redisDao.getSeckill(seckillId);
        if (seckill == null) {
            //缓存没有的话从数据库查
            seckill = seckillDao.queryById(seckillId);
            if (!(seckill == null)) {
                //从数据库中查出来放进缓存
                redisDao.setSeckill(seckill);
            } else {
                System.out.println("数据库中没有该商品");
            }
        }
        //缓存有的话直接返回
        return seckillDao.queryById(seckillId);
    }

    public Exposer exportSeckillUrl(long seckillId) {

        //从redis缓存查
        Seckill seckill = redisDao.getSeckill(seckillId);
        if (seckill == null) {
            // 2.访问数据库
            seckill = seckillDao.queryById(seckillId);
            if (seckill == null) {
                return new Exposer(false, seckillId);
            } else {
                // 3.访问redis
                redisDao.setSeckill(seckill);
            }
        }

        //该商品的秒杀时间
        Date startTime=seckill.getStartTime();
        Date endTime=seckill.getEndTime();
        //系统当前时间
        Date nowTime=new Date();
        //如果当前时间不在秒杀时间内
        if (startTime.getTime()>nowTime.getTime() || endTime.getTime()<nowTime.getTime())
        {
            return new Exposer(false,seckillId,nowTime.getTime(),startTime.getTime(),endTime.getTime());
        }

        //否则，秒杀开启，返回秒杀商品的id、用给接口加密的md5
        String md5=getMD5(seckillId);
        return new Exposer(true,md5,seckillId);
    }

    private String getMD5(long seckillId)
    {
        String base=seckillId+"/"+salt;
        String md5= DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }

    //秒杀是否成功，成功:减库存，增加明细；失败:抛出异常，事务回滚
    //开启事务
    @Transactional
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5)
            throws SeckillException, DuplicateKillException, SeckillClosedException {

        if (md5==null||!md5.equals(getMD5(seckillId)))
        {
            return new SeckillExecution(seckillId, SeckillStateEnum.stateOf(-3));
            /*throw new SeckillException("seckill data rewrite");//秒杀数据被重写了*/
        }
        //执行秒杀逻辑:减库存+增加购买明细
        Date nowTime=new Date();

        try{
            //减库存
            int updateCount=seckillDao.reduceNumber(seckillId,nowTime);
            if (updateCount<=0)
            {
                return new SeckillExecution(seckillId, SeckillStateEnum.stateOf(0));
                //没有更新库存记录，说明秒杀结束
                /*throw new SeckillClosedException("seckill is closed");*/
            }else {
                //否则更新了库存，秒杀成功,增加明细
                int insertCount=successKilledDao.insertSuccessKilled(seckillId,userPhone);
                //看是否该明细被重复插入，即用户是否重复秒杀
                if (insertCount<=0)
                {
                    return new SeckillExecution(seckillId, SeckillStateEnum.stateOf(-1));
                   /* throw new DuplicateKillException("seckill repeated");*/

                }else {
                    //秒杀成功,得到成功插入的明细记录,并返回成功秒杀的信息
                    SuccessKilled successKilled=successKilledDao.queryByIdWithSeckill(seckillId,userPhone);

                    //返回一个秒杀对象，携带有秒杀id、状态枚举对象、还有秒杀明细对象
                    return new SeckillExecution(seckillId, SeckillStateEnum.stateOf(1),successKilled);
                }
            }

        }catch (SeckillClosedException e1)
        {
            throw e1;
        }catch (DuplicateKillException e2)
        {
            throw e2;
        }catch (Exception e)
        {
            logger.error(e.getMessage(),e);
            //所以编译期异常转化为运行期异常
            throw new SeckillException("seckill inner error :"+e.getMessage());
        }

    }
}
