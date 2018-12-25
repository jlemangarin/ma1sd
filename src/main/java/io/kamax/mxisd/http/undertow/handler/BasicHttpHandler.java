/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxisd.http.undertow.handler;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.exception.HttpMatrixException;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.proxy.Response;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

public abstract class BasicHttpHandler implements HttpHandler {

    private transient final Logger log = LoggerFactory.getLogger(BasicHttpHandler.class);

    protected String getRemoteHostAddress(HttpServerExchange exchange) {
        return ((InetSocketAddress) exchange.getConnection().getPeerAddress()).getAddress().getHostAddress();
    }

    protected String getQueryParameter(HttpServerExchange exchange, String name) {
        try {
            String raw = exchange.getQueryParameters().getOrDefault(name, new LinkedList<>()).peekFirst();
            return URLDecoder.decode(raw, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new InternalServerError(e);
        }
    }

    protected String getPathVariable(HttpServerExchange exchange, String name) {
        return getQueryParameter(exchange, name);
    }

    protected void writeBodyAsUtf8(HttpServerExchange exchange, String body) {
        exchange.getResponseSender().send(body, StandardCharsets.UTF_8);
    }

    protected <T> T parseJsonTo(HttpServerExchange exchange, Class<T> type) {
        try {
            return GsonUtil.get().fromJson(IOUtils.toString(exchange.getInputStream(), StandardCharsets.UTF_8), type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected JsonObject parseJsonObject(HttpServerExchange exchange, String key) {
        try {
            JsonObject base = GsonUtil.parseObj(IOUtils.toString(exchange.getInputStream(), StandardCharsets.UTF_8));
            return GsonUtil.getObj(base, key);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void respond(HttpServerExchange ex, int statusCode, JsonElement bodyJson) {
        respondJson(ex, statusCode, GsonUtil.get().toJson(bodyJson));
    }

    protected void respond(HttpServerExchange ex, JsonElement bodyJson) {
        respond(ex, 200, bodyJson);
    }

    protected void respondJson(HttpServerExchange ex, int status, String body) {
        ex.setStatusCode(status);
        ex.getResponseHeaders().put(HttpString.tryFromString("Content-Type"), "application/json");
        writeBodyAsUtf8(ex, body);
    }

    protected void respondJson(HttpServerExchange ex, String body) {
        respondJson(ex, 200, body);
    }

    protected void respondJson(HttpServerExchange ex, Object body) {
        respondJson(ex, GsonUtil.get().toJson(body));
    }

    protected JsonObject buildErrorBody(HttpServerExchange exchange, String errCode, String error) {
        JsonObject obj = new JsonObject();
        obj.addProperty("errcode", errCode);
        obj.addProperty("error", error);
        obj.addProperty("success", false);
        log.info("Request {} {} - Error {}: {}", exchange.getRequestMethod(), exchange.getRequestURL(), errCode, error);
        return obj;
    }

    protected void respond(HttpServerExchange ex, int status, String errCode, String error) {
        respond(ex, status, buildErrorBody(ex, errCode, error));
    }

    protected void handleException(HttpServerExchange exchange, HttpMatrixException ex) {
        respond(exchange, ex.getStatus(), buildErrorBody(exchange, ex.getErrorCode(), ex.getError()));
    }

    protected void respond(HttpServerExchange exchange, Response upstream) {
        exchange.setStatusCode(upstream.getStatus());
        upstream.getHeaders().forEach((key, value) -> exchange.getResponseHeaders().addAll(HttpString.tryFromString(key), value));
        writeBodyAsUtf8(exchange, upstream.getBody());
    }
}
