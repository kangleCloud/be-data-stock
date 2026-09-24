package com.vita.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.jackson.serializer
 * @Author: znk
 * @CreateTime: 2026-03-04  20:20:46
 * @Description: LongToStringSerializer是一个自定义的JSON序列化器，用于将Long类型的数据序列化为String类型。这个序列化器主要用于解决前端在处理大数字时可能出现的精度丢失问题。通过将Long类型转换为String类型，可以确保前端能够正确地接收和处理这些数据，而不会因为JavaScript的数字精度限制而导致数据丢失或错误。
 * @Version: 1.0
 */
public class LongToStringSerializer extends JsonSerializer<Long> {

    /**
     * 将Long类型序列化为String类型，避免前端精度丢失问题
     *
     * @param aLong              需要序列化的Long类型数据
     * @param jsonGenerator      用于生成JSON输出的JsonGenerator对象
     * @param serializerProvider 提供序列化上下文信息的SerializerProvider对象
     * @throws IOException 如果在序列化过程中发生IO异常
     */
    @Override
    public void serialize(Long aLong, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(aLong.toString());
    }

}
