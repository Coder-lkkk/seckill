package com.lkkk.seckill.exception;

public class SeckillClosedException extends RuntimeException{
    public SeckillClosedException(String message) {
        super(message);
    }

    public SeckillClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
