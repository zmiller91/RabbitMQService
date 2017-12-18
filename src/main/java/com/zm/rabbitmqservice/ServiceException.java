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

import java.util.List;

/**
 *
 * @author zmiller
 */
public class ServiceException extends Exception {

    public Code code;
    public Reason reason;
    public List<String> messages;

    enum Code {

        BAD_REQUEST(400),
        NOT_FOUND(404),
        SERVER_ERROR(500);

        private final int value;

        Code(int value) {
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
        COULD_NOT_PARSE_REQUEST("Could not parse request"),
        COULD_NOT_PARSE_PARAMS("Could not parse parameter"),
        INVALID_PARAM_COUNT("Invalid number of parameters"),
        INVALID_PARAM_TYPE("Invalid parameter type");

        private final String value;

        Reason(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    ServiceException(Code code, Reason reason) {
        this(code, reason, null);
    }
    
    ServiceException(Code code, Reason reason, List<String> messages) {
        this.code = code;
        this.reason = reason;
        this.messages = messages;
    }

    @Override
    public String getMessage() {
        return this.reason.getValue();
    }
}
