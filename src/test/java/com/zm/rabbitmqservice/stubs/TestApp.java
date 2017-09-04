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
package com.zm.rabbitmqservice.stubs;

import com.zm.rabbitmqservice.stubs.TestAPI;

/**
 *
 * @author zmiller
 */
public class TestApp implements TestAPI {

    public static final String STRING_NO_PARAMS_RETVAL = "Hello.";
    
    @Override
    public String stringNoParams() {
        return STRING_NO_PARAMS_RETVAL;
    }

    @Override
    public void voidNoParams() {}

    @Override
    public Void objectVoidNoParams() {
        return null;
    }

    @Override
    public void throwException() {
        throw new RuntimeException();
    }

    @Override
    public String twoStrings(String a, String b) {
        return a + b;
    }

    @Override
    public CustomReturnObject customReturnObject(String message) {
        return new CustomReturnObject(message);
    }
    
}
