///*
// * Copyright (C) 2017 zmiller
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package com.zm.rabbitmqservice;
//
//import static com.zm.rabbitmqservice.RPCError.Code.BAD_REQUEST;
//import static com.zm.rabbitmqservice.RPCError.Reason.COULD_NOT_PARSE_REQUEST;
//import static com.zm.rabbitmqservice.RPCError.Reason.WRONG_RPC_VERSION;
//import static com.zm.rabbitmqservice.stubs.TestApp.STRING_NO_PARAMS_RETVAL;
//import java.util.HashMap;
//import java.util.Map;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertNull;
//import org.junit.Test;
//
///**
// *
// * @author zmiller
// */
//public class ServiceTest extends RMQApplicationTest {
//
//    @Test
//    public void serviceTest_ValidRequest_ShouldSucceed() {
//
//        // Test
//        RPCRequest request = createRequest("abc", "2.0", "stringNoParams", null);
//        RPCResponse response = callHandleDelivery(request);
//
//        // Assert
//        assertEquals(request.id, response.id);
//        assertEquals("2.0", response.jsonrpc);
//        assertNotNull(response.result);
//        assertEquals(STRING_NO_PARAMS_RETVAL, response.result);
//    }
//
//    @Test
//    public void serviceTest_WrongJsonRPCParam_ShouldReturnError() {
//
//        // Test
//        RPCRequest request = createRequest("abc", "3.0", "stringNoParams", null);
//        RPCResponse response = callHandleDelivery(request);
//
//        // Assert
//        assertNotNull(response.error);
//        assertEquals(BAD_REQUEST.getValue(), response.error.getCode());
//        assertEquals(WRONG_RPC_VERSION.getValue(), response.error.getMessage());
//    }
//
//    @Test
//    public void testParams_InvalidParamProperty_ShouldReturnError() {
//
//        // Test
//        Map<String, String> input = new HashMap<>();
//        input.put("jsonrpc", "2.0");
//        input.put("id", "123");
//        input.put("method", "voidNoParams");
//        input.put("params", "garbage");
//        RPCResponse response = callHandleDelivery(gson.toJson(input).getBytes());
//
//        // Validate
//        assertEquals(COULD_NOT_PARSE_REQUEST.getValue(), response.error.getMessage());
//        assertEquals(BAD_REQUEST.getValue(), response.error.getCode());
//    }
//
//    @Test
//    public void serviceTest_MalformedInput_ShouldReturnError() {
//
//        // Test
//        RPCResponse response = callHandleDelivery("garbage".getBytes());
//
//        // Validate
//        assertEquals(COULD_NOT_PARSE_REQUEST.getValue(), response.error.getMessage());
//        assertEquals(BAD_REQUEST.getValue(), response.error.getCode());
//    }
//
//    @Test
//    public void serviceTest_ResultProperty_ExistsWhenSuccessful() {
//
//        // Test void
//        RPCRequest request = createRequest("abc", "2.0", "voidNoParams", null);
//        RPCResponse response = callHandleDelivery(request);
//        assertNull(response.error);
//        assertNotNull(response.result);
//        assertEquals(RMQApplication.OK, response.result);
//
//        // Test Void
//        request = createRequest("abc", "2.0", "objectVoidNoParams", null);
//        response = callHandleDelivery(request);
//        assertNull(response.error);
//        assertNotNull(response.result);
//        assertEquals(RMQApplication.OK, response.result);
//
//        // Test Object
//        request = createRequest("abc", "2.0", "stringNoParams", null);
//        response = callHandleDelivery(request);
//        assertNull(response.error);
//        assertNotNull(response.result);
//        assertEquals(STRING_NO_PARAMS_RETVAL, response.result);
//    }
//
//    @Test
//    public void serviceTest_ErrorProperty_ExistsWhenUnSuccessful() {
//
//        // Test void
//        RPCRequest request = createRequest("abc", "2.0", "throwException", null);
//        RPCResponse response = callHandleDelivery(request);
//
//        assertNull(response.result);
//        assertNotNull(response.error);
//        assertNotNull(response.error.getCode());
//        assertNotNull(response.error.getMessage());
//    }
//}
