package com.xiaofeiyang.expense.framework.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaofeiyang.expense.framework.ApiResponse;
import feign.FeignException;
import feign.Response;
import feign.codec.Decoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * Feign Decoder that unwraps {@link ApiResponse#getData()} from
 * the standard {@code {"code":200,"message":"success","data":...}} envelope
 * before delegating to {@link SpringDecoder} for the actual type binding.
 *
 * <p>Without this, a Feign client returning {@code List<CategoryDTO>}
 * would fail to deserialize because the HTTP body is an object, not an array.
 *
 * <p>Gracefully passes through responses that are NOT wrapped in ApiResponse
 * (e.g. raw arrays or other formats).
 */
public class ApiResponseDecoder implements Decoder {

    private final SpringDecoder springDecoder;
    private final ObjectMapper objectMapper;

    public ApiResponseDecoder(ObjectProvider<HttpMessageConverters> converters,
                              ObjectMapper objectMapper) {
        this.springDecoder = new SpringDecoder(converters);
        this.objectMapper = objectMapper;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException, FeignException {
        // If the Feign method returns ApiResponse<T>, let SpringDecoder handle it directly
        if (type instanceof ParameterizedType pt && pt.getRawType() == ApiResponse.class) {
            return springDecoder.decode(response, type);
        }

        // Read body bytes (can only read once from Feign Response)
        byte[] bodyBytes = response.body() != null
                ? response.body().asInputStream().readAllBytes()
                : new byte[0];

        // Try to detect and unwrap ApiResponse envelope
        try {
            JsonNode root = objectMapper.readTree(bodyBytes);
            // Only unwrap if it looks like ApiResponse: JSON object with "code" AND "data" fields
            if (root.isObject() && root.has("code") && root.has("data")) {
                int code = root.get("code").asInt();
                if (code != 200) {
                    String msg = root.has("message") ? root.get("message").asText() : "Feign call error";
                    throw FeignException.errorStatus(
                            "ApiResponse error: code=" + code + " msg=" + msg,
                            response.toBuilder().body(bodyBytes).headers(response.headers()).build());
                }
                JsonNode dataNode = root.get("data");
                if (dataNode == null || dataNode.isNull()) {
                    return null;
                }
                // Unwrap: replace body with just the "data" payload
                bodyBytes = dataNode.toString().getBytes(StandardCharsets.UTF_8);
            }
            // else: not an ApiResponse wrapper, pass original body through unchanged
        } catch (FeignException e) {
            throw e;
        } catch (Exception e) {
            // Not parseable JSON — pass original body through unchanged
        }

        Response finalResponse = response.toBuilder()
                .body(bodyBytes)
                .build();
        return springDecoder.decode(finalResponse, type);
    }
}
