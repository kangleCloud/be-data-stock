package com.vita.jackson.serializer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.jackson.serializer
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: LocalDateTime反序列化器
 * @Version: 1.0
 */
public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    /**
     * 将字符串反序列化为 LocalDateTime。
     *
     * @param jsonParser             JSON 解析器
     * @param deserializationContext 反序列化上下文
     * @return LocalDateTime 对象
     * @throws IOException IO 异常
     */
    @Override
    public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        return LocalDateTime.parse(jsonParser.getValueAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
