package com.zm.rabbitmqservice.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.zm.rabbitmqservice.RPCError;
import com.zm.rabbitmqservice.RPCRequest;
import com.zm.rabbitmqservice.RPCResponse;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.zm.rabbitmqservice.service.ServiceException.Code.BAD_REQUEST;
import static com.zm.rabbitmqservice.service.ServiceException.Code.NOT_FOUND;
import static com.zm.rabbitmqservice.service.ServiceException.Code.SERVER_ERROR;
import static com.zm.rabbitmqservice.service.ServiceException.Reason.*;
import static com.zm.rabbitmqservice.service.ServiceException.Reason.COULD_NOT_PARSE_PARAMS;

class AppConsumer<U> extends DefaultConsumer {

    private static final Gson gson = new Gson();
    private U app;
    private Map<String, Class<?>[]> methods;

    <T extends U> AppConsumer(Channel channel, T app) {
        super(channel);
        this.app = app;
        this.methods = new HashMap<>();
        for(Method m : app.getClass().getMethods()) {
            methods.put(m.getName(), m.getParameterTypes());
        }
        System.out.println("Creating AppConsumer");
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        RPCResponse response = new RPCResponse();
        try {

            RPCRequest request = parseRequest(body, response);
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

        } catch ( JsonSyntaxException | IOException e) {
            setError(response, COULD_NOT_PARSE_REQUEST, BAD_REQUEST);
        }

        acknowledge(response, envelope, properties);
    }

    /**
     * Acknowledge the request was received and processed
     *
     * @param response - response object
     * @param envelope
     * @param properties
     */
    private void acknowledge(RPCResponse response, Envelope envelope, AMQP.BasicProperties properties) {
        try {
            this.getChannel().basicPublish("", properties.getReplyTo(), properties, gson.toJson(response).getBytes());
            this.getChannel().basicAck(envelope.getDeliveryTag(), false);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Deserialize the raw RPC request into an RPCRequest object. Return null if the request cannot be deserialized.
     *
     * @param rawRequest - raw request json string
     * @param response - response object
     * @return deserialized request object or null on failure
     */
    private RPCRequest parseRequest(byte[] rawRequest, RPCResponse response) throws JsonSyntaxException, IOException {
        try (InputStream is = new ByteArrayInputStream(rawRequest);
             Reader reader = new InputStreamReader(is))
        {
            RPCRequest request = gson.fromJson(reader, RPCRequest.class);
            response.id = request.id;
            return request;
        }
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
            }
            catch (InvocationTargetException e) {

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
            setError(response, INVALID_PARAM_COUNT, ServiceException.Code.BAD_REQUEST);
            return null;
        }

        // Decode each parameter to it's java type
        for(int i = 0; i < params.length; i++) {
            try {
                params[i] = gson.fromJson(request.params.get(i), types[i]);
            }

            // Fail if there is a syntax exception, don't terminate in case there
            // are more invalid paramters
            catch(JsonSyntaxException e) {
                success = false;
                errors.add(INVALID_PARAM_TYPE.getValue()+ " at index " + i);
            }
            catch(Exception e) {
                e.printStackTrace();
                errors.add(ServiceException.Reason.UNKNOWN_EXCEPTION.getValue());
            }
        }

        if(!success) {
            setError(response, COULD_NOT_PARSE_PARAMS, ServiceException.Code.BAD_REQUEST, errors);
            return null;
        }

        return params;
    }

    private void setError(RPCResponse response, ServiceException.Reason reason, ServiceException.Code code) {
        setError(response, reason, code, null);
    }
    private void setError(RPCResponse response, ServiceException.Reason reason, ServiceException.Code code, List<String> messages) {
        response.error = new RPCError<>(new ServiceException(code, reason, messages));
    }
}