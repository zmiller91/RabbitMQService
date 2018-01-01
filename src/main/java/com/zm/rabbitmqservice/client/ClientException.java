package com.zm.rabbitmqservice.client;

public class ClientException extends Exception {
    public final String message;
    public final Throwable cause;
    public ClientException(String message, Throwable cause) {
        this.message = message;
        this.cause = cause;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }
}
