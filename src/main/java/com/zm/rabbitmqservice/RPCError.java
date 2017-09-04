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


/**
 *
 * @author zmiller
 */
class RPCError {
    
    private String message;
    private Object data;
    private int code;
    
    enum Code {
        
        BAD_REQUEST(400),
        NOT_FOUND(403),
        SERVER_ERROR(500);
        
        private final int value;
        
        private Code(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    enum Reason {
        
        BAD_PARAMETERS("Bad parameters"),
        NO_SUCH_METHOD("No such method"),
        UNKNOWN_EXCEPTION("Unknown error"),
        WRONG_RPC_VERSION("jsonrpc must be 2.0"),
        COULD_NOT_PARSE_REQUEST("Could not parase request"),
        COULD_NOT_PARSE_PARAMS("Could not parse parameter"),
        INVALID_PARAM_COUNT("Invalid number of paramters"),
        INVALID_PARAM_TYPE("Invalid paramter type");
        
        private final String value;
        
        private Reason(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    public void setMessage(Reason reason) {
        this.message = reason.getValue();
    }
    
    public void setCode(Code code) {
        this.code = code.getValue();
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    public String getMessage() {
        return message;
    }
    
    public int getCode() {
        return code;
    }
    
    public Object getData() {
        return data;
    }
}
