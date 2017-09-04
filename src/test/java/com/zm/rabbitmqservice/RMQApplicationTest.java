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

import com.zm.rabbitmqservice.stubs.TestApp;
import com.zm.rabbitmqservice.stubs.TestAPI;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import static org.mockito.Mockito.mock;

/**
 *
 * @author zmiller
 */
public class RMQApplicationTest {
    
    Gson gson = new Gson();
    
    RPCResponse callHandleDelivery(RPCRequest request) {
        byte[] body = gson.toJson(request, RPCRequest.class).getBytes();
        return callHandleDelivery(body);
    }
    
    RPCResponse callHandleDelivery(byte[] body) {
        Channel channel = mock(Channel.class);
        RPCResponse response = new RPCResponse();
        RMQApplication app = new RMQApplication(channel, new TestApp(), TestAPI.class);
        app.handleDelivery(response, "test", mock(Envelope.class), mock(BasicProperties.class), body);
        return response;
    }
    
    RPCRequest createRequest(String id, String jsonrpc, String method, JsonArray params) {
        RPCRequest request = new RPCRequest();
        request.id = id;
        request.jsonrpc = jsonrpc;
        request.method = method;
        request.params = params;
        return request;
    }
    
    
    // SERVICE TESTS
    
    // malformed request
    // RPCResponse id matches request id
    // RPCResponse protocol is "2.0"
    // RPCResponse has result property on success
    //     null
    //     void
    //     object
    // RPCResponse has error property on error
    //     contains code, error, and id
    
    // PARAMETER TESTS
    
    // no parameters
    // parameter object is not array
    // multiple parameters
    // wrong number of parameters
    // wrong type of parameters
    
    // EXCEPTION TESTS
    
    // exception thrown in start causes reconnect
    // RMQApplicationException is caught and reported
    // Server error is caught and reported
    
    
    // API TESTS
    
    // no params
    // multiple params
    // application must inherit api
    // no duplicate methods allowed
    // no such method
}
