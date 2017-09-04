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

import com.google.gson.JsonArray;
import static com.zm.rabbitmqservice.RPCError.Code.NOT_FOUND;
import static com.zm.rabbitmqservice.RPCError.Reason.NO_SUCH_METHOD;
import com.zm.rabbitmqservice.stubs.CustomReturnObject;
import com.zm.rabbitmqservice.stubs.DuplicateApi;
import com.zm.rabbitmqservice.stubs.DuplicateApiApp;
import com.zm.rabbitmqservice.stubs.TestApp;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author zmiller
 */
public class ApiTest extends RMQApplicationTest {
    
    @Test
    public void testApi_NoParamsMethod_ShouldGetCalled() {
        RPCRequest request = createRequest("abc", "2.0", "voidNoParams", null);
        RPCResponse response = callHandleDelivery(request);
        assertEquals(RMQApplication.OK, response.result);
    }
    
    @Test
    public void testApi_MultipleParamsMethod_ShouldGetCalled() {
        
        JsonArray params = new JsonArray();
        params.add("a");
        params.add("b");
        
        RPCRequest request = createRequest("abc", "2.0", "twoStrings", params);
        RPCResponse response = callHandleDelivery(request);
        assertEquals("ab", response.result);
    }
    
    @Test
    public void testApi_NoSuchMethod_ShouldReturnError() {
        
        RPCRequest request = createRequest("abc", "2.0", "fail", null);
        RPCResponse response = callHandleDelivery(request);
        assertNotNull(response.error);
        assertEquals(NO_SUCH_METHOD.getValue(), response.error.getMessage());
        assertEquals(NOT_FOUND.getValue(), response.error.getCode());
    }
    
    @Test
    public void testApi_ReturnCustomObject_ShouldSucceed() {
        
        JsonArray params = new JsonArray();
        params.add("test");
        
        RPCRequest request = createRequest("abc", "2.0", "customReturnObject", params);
        RPCResponse response = callHandleDelivery(request);
        assertTrue(response.result instanceof CustomReturnObject);
        assertEquals("test", ((CustomReturnObject)response.result).message);
    }
    
    @Test
    public void testApi_AppDoesntInheritApi_ShouldReturnError() {
        try {
            RMQApplication.start("test", "test", new TestApp(), String.class, 5);
            fail("Should not have succeeded");
        }
        catch (Exception e) {
            assertTrue(e.getMessage().contains("does not implement"));
        }
    }
    
    @Test
    public void testApi_DuplicateMethods_NotAllowed() {
        try {
            RMQApplication.start("test", "test", new DuplicateApiApp(), DuplicateApi.class, 5);
            fail("Should not have succeeded");
        }
        catch (Exception e) {
            assertTrue(e.getMessage().contains("Duplicate API method names are not allowed"));
        }
    }
}
