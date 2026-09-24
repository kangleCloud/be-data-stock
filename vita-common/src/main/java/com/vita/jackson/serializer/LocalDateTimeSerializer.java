package com.vita.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.jackson.serializer
 * @Author: znk
 * @CreateTime: 2026-07-10  16:40:48
 * @Description: LocalDateTime序列化器
 * @Version: 1.0
 */
public class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

    /**
     * 将 LocalDateTime 序列化为日期时间字符串。
     *
     * @param localDateTime      日期时间对象
     * @param jsonGenerator      JSON 生成器
     * @param serializerProvider 序列化上下文
     * @throws IOException IO 异常
     */
    @Override
    public void serialize(LocalDateTime localDateTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }
}
