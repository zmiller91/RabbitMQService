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

/**
 *
 * @author zmiller
 */
class RPCResponse {

    final String jsonrpc = "2.0";
    String id;
    RPCError<?> error;
    private String result;

    public void setResult(Object result) {
        this.result = new Gson().toJson(result, result.getClass());
    }

    public <T> T getResult(Class<T> type) {
        return new Gson().fromJson(this.result, type);
    }
}
