/*
 * Copyright (C) 2017 zmiller
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.zm.rabbitmqservice;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import static com.zm.rabbitmqservice.ServiceException.Code;
import static com.zm.rabbitmqservice.ServiceException.Reason;
import static com.zm.rabbitmqservice.ServiceException.Code.BAD_REQUEST;
import static com.zm.rabbitmqservice.ServiceException.Code.NOT_FOUND;
import static com.zm.rabbitmqservice.ServiceException.Code.SERVER_ERROR;
import static com.zm.rabbitmqservice.ServiceException.Reason.COULD_NOT_PARSE_PARAMS;
import static com.zm.rabbitmqservice.ServiceException.Reason.COULD_NOT_PARSE_REQUEST;
import static com.zm.rabbitmqservice.ServiceException.Reason.INVALID_PARAM_COUNT;
import static com.zm.rabbitmqservice.ServiceException.Reason.INVALID_PARAM_TYPE;
import static com.zm.rabbitmqservice.ServiceException.Reason.NO_SUCH_METHOD;
import static com.zm.rabbitmqservice.ServiceException.Reason.UNKNOWN_EXCEPTION;
import static com.zm.rabbitmqservice.ServiceException.Reason.WRONG_RPC_VERSION;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 *
 * @author zmiller
 */
public class RMQApplication extends DefaultConsumer {
    
    private static final ConnectionFactory factory = new ConnectionFactory();
    private final Gson gson;
    private final Channel channel;
    private final Map<String, Class<?>[]> methods;
    private final Object app;
    
     private RMQApplication(Channel channel, Object app, Class api) {
        super(channel);
        
        gson = new Gson();
        this.channel = channel;
        this.app = app;
        methods = new HashMap<>();
        for(Method m : api.getMethods()) {
            methods.put(m.getName(), m.getParameterTypes());
        }
    }

    /**
     * Start the RabbitMQ application. This is the main method that gets executed when creating a RabbitMQ application.
     *
     * @param host - RabbitMQ host
     * @param queueName - RabbitMQ channel
     * @param app - Class that implements an API which will be called when messages are retrieved
     * @param api - API that defines the valid application operations
     * @param executorPoolSize - size of the RabbitMQ thread pool
     */
    public static void start(String host, String queueName, Object app, Class api, int executorPoolSize) {
        
        // Application must inherit the API
        if(!api.isAssignableFrom(app.getClass())) {
            throw new RuntimeException(app.getClass().getName() + " does not implement " + api.getName());
        }
        
        // No duplicate method names allowed
        Set<String> names = new HashSet<>();
        for(Method m : api.getMethods()) {
            if(names.contains(m.getName())) {
                throw new RuntimeException("Duplicate API method names are not allowed");
            }
            names.add(m.getName());
        }
        
        // Start consuming the queue, reconnect if there is an error
        factory.setHost(host);
        Channel channel = null;
        Connection connection = null;
        while(true) {
            try {
                if(connection == null || channel == null) {
                    System.out.println("Establishing new connection...");
                    connection = factory.newConnection(Executors.newFixedThreadPool(executorPoolSize));
                    channel = connection.createChannel();
                    channel.queueDeclare(queueName, false, false, false, null);
                    channel.basicQos(1);
                    channel.basicConsume(queueName, false, new RMQApplication(channel, app, api));
                }
            }
            
            catch(Exception ex) {
                try {if (channel != null) {channel.close();}} catch (Exception e) {e.printStackTrace();}
                try {if (connection != null) {connection.close();}} catch (Exception e) {e.printStackTrace();}
                try { Thread.sleep(15000); } catch (InterruptedException e) {}
                connection = null;
                channel = null;
            }
            
            // Let another thread take over
            try { Thread.sleep(0); } catch (InterruptedException ex) {}
        }
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) {
        RPCResponse response = new RPCResponse();
        try {

            String message = new String(body,"UTF-8");
            RPCRequest request = parseRequest(message, response);
            boolean success = request != null;

            // Must be jsonrpc 2.0 by spec
            if(success && (request.jsonrpc == null || !request.jsonrpc.equals("2.0"))) {
                setError(response, WRONG_RPC_VERSION, BAD_REQUEST);
                success = false;
            }

            // Method must exist in the api class
            if(success && methods.get(request.method) == null) {
                setError(response, NO_SUCH_METHOD, NOT_FOUND);
                success = false;
            }

            // Invoke the specified method if the request is valid
            if(success) {
                invoke(request, response);
            }

        } catch (UnsupportedEncodingException | JsonSyntaxException e) {
            setError(response, COULD_NOT_PARSE_REQUEST, BAD_REQUEST);
        }

        acknowledge(response, envelope, properties);
    }

    /**
     * Deserialize the raw RPC request into an RPCRequest object. Return null if the request cannot be deserialized.
     *
     * @param rawRequest - raw request json string
     * @param response - response object
     * @return deserialized request object or null on failure
     */
    private RPCRequest parseRequest(String rawRequest, RPCResponse response) throws JsonSyntaxException {
        RPCRequest request = gson.fromJson(rawRequest, RPCRequest.class);
        response.id = request.id;
        return request;
    }

    /**
     * Invoke the method as specified in the RPCRequest object.
     *
     * @param request - request object
     * @param response - response object
     */
    private void invoke(RPCRequest request, RPCResponse response) {
        Class[] types = methods.get(request.method);
        Object[] params = parseParameters(request, response, types);
        if(params != null) {
            Method method = null;

            try {
                method = app.getClass().getMethod(request.method, types);
            } catch (NoSuchMethodException e) {
                response.error = new RPCError<>(new ServiceException(NOT_FOUND, NO_SUCH_METHOD));
            }

            // TODO: dont return early
            if (method == null) {
                return;
            }

            try {
                response.setResult(method.invoke(app, params));
            } catch (IllegalAccessException e) {
                response.error = new RPCError<>(new ServiceException(SERVER_ERROR, UNKNOWN_EXCEPTION));
            } catch (InvocationTargetException e) {

                // Attach the error to the response object if the error has been explicitly thrown
                for(Class<?> clazz : method.getExceptionTypes()) {
                    if(clazz.getName().equals(e.getCause().getClass().getName())) {
                        response.error = new RPCError<>(e.getCause());
                    }
                }

                // Unknown error
                if (response.error == null) {
                    e.printStackTrace();
                    response.error = new RPCError<>(new ServiceException(SERVER_ERROR, UNKNOWN_EXCEPTION));
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                response.error = new RPCError<>(new ServiceException(SERVER_ERROR, UNKNOWN_EXCEPTION));
            }
        }
    }

    /**
     * Parse the parameters defined in the RPCRequest to be used for reflection method invocation.
     *
     * @param request - request object
     * @param response - response object
     * @param types - parameter types of method being executed
     * @return - parsed parameters
     */
    private Object[] parseParameters(RPCRequest request, RPCResponse response, Class[] types) {
        
        Object[] params;
        boolean success = true;
        List<String> errors = new ArrayList<>();
        
        // Add empty params if none exist
        if(request.params == null) {
            request.params = new JsonArray();
        }
        
        // Invalid number of parameters
        params = new Object[request.params.size()];
        if(params.length != types.length) {
            setError(response, INVALID_PARAM_COUNT, Code.BAD_REQUEST);
            return null;
        }
        
        // Decode each parameter to it's java type
        for(int i = 0; i < params.length; i++) {
            try {
                params[i] = gson.fromJson(request.params.get(i).toString(), types[i]);
            }
            
            // Fail if there is a syntax exception, don't terminate in case there
            // are more invalid paramters
            catch(JsonSyntaxException e) {
                success = false;
                errors.add(INVALID_PARAM_TYPE.getValue()+ " at index " + i);
            }
            catch(Exception e) {
                e.printStackTrace();
                errors.add(Reason.UNKNOWN_EXCEPTION.getValue());
            }
        }
        
        if(!success) {
            setError(response, COULD_NOT_PARSE_PARAMS, Code.BAD_REQUEST, errors);
            return null;
        }
        
        return params;
    }

    /**
     * Acknowledge the request was received and processed
     *
     * @param response - response object
     * @param envelope
     * @param properties
     */
    private void acknowledge(RPCResponse response, Envelope envelope, BasicProperties properties) {
        AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
                .correlationId(properties.getCorrelationId())
                .build();
        
        try {
//            System.out.println(gson.toJson(response));
            byte[] reply = gson.toJson(response).getBytes("UTF-8");
            channel.basicPublish("", properties.getReplyTo(), replyProps, reply);
            channel.basicAck(envelope.getDeliveryTag(), false);
        } 
        
        catch (Exception e) {
            //TODO: dont leave this empty
        }
    }

    private void setError(RPCResponse response, Reason reason, Code code) {
        setError(response, reason, code, null);
    }
    private void setError(RPCResponse response, Reason reason, Code code, List<String> messages) {
            response.error = new RPCError<>(new ServiceException(code, reason, messages));
    }
}
