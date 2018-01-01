package com.zm.rabbitmqservice.service;

public class ServiceUnavailableException extends Exception {

    public enum Status {
        IN_QUEUE,
        EXPIRED
    }

    public ServiceUnavailableException(Status status) {
        super(getMessage(status));
    }

    private static String getMessage(Status status) {
        String message = "The requested service is unavailable or unresponsive. ";
        switch(status) {
            case IN_QUEUE:
                message += "The request has not been expired and will be processed when the service comes online.";
                break;
            case EXPIRED:
                message += "The request has been expired from the queue.";
        }
        return message;
    }
}
