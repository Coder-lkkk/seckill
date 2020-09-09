package com.lkkk.seckill.exception;

public class DuplicateKillException extends RuntimeException{

    public DuplicateKillException(String message) {
        super(message);
    }

    public DuplicateKillException(String message, Throwable cause) {
        super(message, cause);
    }
}
