package com.vita.jackson.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.jackson.serializer
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: LocalDate反序列化器
 * @Version: 1.0
 */
public class LocalDateDeserializer extends JsonDeserializer<LocalDate> {

    /**
     * 将字符串反序列化为 LocalDate。
     *
     * @param jsonParser             JSON 解析器
     * @param deserializationContext 反序列化上下文
     * @return LocalDate 对象
     * @throws IOException IO 异常
     */
    @Override
    public LocalDate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return LocalDate.parse(jsonParser.getValueAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
