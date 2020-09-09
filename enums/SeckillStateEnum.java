package com.lkkk.seckill.enums;

public enum SeckillStateEnum {
    SUCCESS(1,"秒杀成功"),
    END(0,"秒杀结束"),
    REPEAT_KILL(-1,"重复秒杀"),
    INNER_ERROR(-2,"系统异常"),
    DATE_REWRITE(-3,"数据篡改");

    private int state;
    private String info;

    SeckillStateEnum(int state, String info) {
        this.state = state;
        this.info = info;
    }

    public int getState() {
        return state;
    }


    public String getInfo() {
        return info;
    }


    public static SeckillStateEnum stateOf(int index)
    {
        for (SeckillStateEnum seckillStateEnum : values())
        {
            if (seckillStateEnum.getState()==index)
            {
                return seckillStateEnum;
            }
        }
        return null;
    }
}
