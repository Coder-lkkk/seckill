package com.lkkk.seckill.controller;

import com.lkkk.seckill.dao.SeckillDao;
import com.lkkk.seckill.dto.Exposer;
import com.lkkk.seckill.dto.SeckillExecution;
import com.lkkk.seckill.dto.SeckillResult;
import com.lkkk.seckill.enums.SeckillStateEnum;
import com.lkkk.seckill.exception.DuplicateKillException;
import com.lkkk.seckill.exception.SeckillClosedException;
import com.lkkk.seckill.pojo.Seckill;
import com.lkkk.seckill.service.SeckillService;
import com.sun.org.apache.xpath.internal.operations.Mod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @RequestMapping("/getList")
    public String getList(Model model){
        List<Seckill> list =  seckillService.getSeckillList();
        for (Seckill seckill : list) {
            System.out.println(seckill);
        }
        model.addAttribute("list",list);
        return "list";
    }

    @RequestMapping(value = "/{seckillId}/detail",method = RequestMethod.GET)
    public String detail(@PathVariable("seckillId") Long seckillId, Model model){
        if(seckillId == null) { return "redirect:/seckill/getList"; }
        Seckill seckill = seckillService.getById(seckillId);
        if(seckill == null){ return "forward:/seckill/getList"; }
        model.addAttribute("seckill",seckill);
        return "detail";
    }

    //ajax ,json暴露秒杀接口的方法
    @RequestMapping(value = "/{seckillId}/exposer",
            method = RequestMethod.GET,
            produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<Exposer> exposer(@PathVariable("seckillId") Long seckillId)
    {
        SeckillResult<Exposer> result;
        try{
            Exposer exposer = seckillService.exportSeckillUrl(seckillId);
            result=new SeckillResult<Exposer>(true,exposer);
        }catch (Exception e)
        {
            e.printStackTrace();
            result=new SeckillResult<Exposer>(false,e.getMessage());
        }

        return result;
    }

    @RequestMapping(value = "/{seckillId}/{md5}/execution",
            method = RequestMethod.POST,
            produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<SeckillExecution> execute(@PathVariable("seckillId") Long seckillId,
                                                   @PathVariable("md5") String md5,
                                                   @CookieValue(value = "userPhone",required = false) Long phone)
    {
        System.out.println("reache here --- get in execute function");
        if (phone==null)
        {
            System.out.println("reache here --- phone == null");
            return new SeckillResult<SeckillExecution>(false,"未注册");
        }
        SeckillResult<SeckillExecution> result;

        try {
            System.out.println("reache here --- phone != null,and do the execute");
            SeckillExecution execution = seckillService.executeSeckill(seckillId, phone, md5);
            return new SeckillResult<SeckillExecution>(true, execution);
        }catch (DuplicateKillException e1)
        {
            SeckillExecution execution=new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
            return new SeckillResult<SeckillExecution>(false,execution);
        }catch (SeckillClosedException e2)
        {
            SeckillExecution execution=new SeckillExecution(seckillId, SeckillStateEnum.END);
            return new SeckillResult<SeckillExecution>(false,execution);
        }
        catch (Exception e)
        {
            SeckillExecution execution=new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
            return new SeckillResult<SeckillExecution>(false,execution);
        }

    }

    //获取系统时间
    @RequestMapping(value = "/time/now",method = RequestMethod.GET)
    @ResponseBody
    public SeckillResult<Long> time()
    {
        Date now=new Date();
        return new SeckillResult<Long>(true,now.getTime());
    }
}
