package com.zm.rabbitmqservice;

public class ClientException extends Exception {
    public final String message;
    public final Throwable cause;
    public ClientException(String message, Throwable cause) {
        this.message = message;
        this.cause = cause;
    }

}