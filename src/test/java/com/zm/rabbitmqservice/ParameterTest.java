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
//import com.google.gson.JsonArray;
//import static com.zm.rabbitmqservice.RPCError.Code.BAD_REQUEST;
//import static com.zm.rabbitmqservice.RPCError.Reason.COULD_NOT_PARSE_PARAMS;
//import static com.zm.rabbitmqservice.RPCError.Reason.INVALID_PARAM_COUNT;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//import org.junit.Test;
//
///**
// *
// * @author zmiller
// */
//public class ParameterTest extends RMQApplicationTest {
//
//    @Test
//    public void testParams_NoParams_ShouldAcceptNullOrEmpty() {
//
//        // Test
//        RPCRequest request = createRequest("abc", "2.0", "voidNoParams", null);
//        RPCResponse response = callHandleDelivery(request);
//        assertEquals(RMQApplication.OK, response.result);
//
//        // Test
//        request = createRequest("abc", "2.0", "voidNoParams", new JsonArray());
//        response = callHandleDelivery(request);
//        assertEquals(RMQApplication.OK, response.result);
//    }
//
//    @Test
//    public void testParams_WrongNumber_ShouldReturnError() {
//
//        // Test
//        JsonArray params = new JsonArray();
//        params.add("justone");
//        RPCRequest request = createRequest("abc", "2.0", "twoStrings", params);
//        RPCResponse response = callHandleDelivery(request);
//
//        // Assert
//        assertEquals(BAD_REQUEST.getValue(), response.error.getCode());
//        assertEquals(INVALID_PARAM_COUNT.getValue(), response.error.getMessage());
//    }
//
//    @Test
//    public void testParams_WrongParamType_ShouldReturnError() {
//
//        // Test
//        JsonArray params = new JsonArray();
//        params.add("justone");
//        params.add(new JsonArray());
//
//        RPCRequest request = createRequest("abc", "2.0", "twoStrings", params);
//        RPCResponse response = callHandleDelivery(request);
//
//        // Assert
//        assertEquals(BAD_REQUEST.getValue(), response.error.getCode());
//        assertEquals(COULD_NOT_PARSE_PARAMS.getValue(), response.error.getMessage());
//        assertNotNull(response.error.getData());
//    }
//}
