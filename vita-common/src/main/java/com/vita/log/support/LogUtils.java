package com.vita.log.support;

import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.log.support
 * @Author: znk
 * @CreateTime: 2026-03-12 20:51:30
 * @Description: 日志摘要工具类。
 * <p>
 * 主要用于统一生成：
 * <ul>
 *     <li>请求参数日志</li>
 *     <li>响应结果日志</li>
 *     <li>异常摘要日志</li>
 * </ul>
 * 为避免日志过大、敏感信息泄露或序列化异常，本工具类内置了：
 * <ul>
 *     <li>敏感字段脱敏</li>
 *     <li>超长日志截断</li>
 *     <li>文件流/请求响应对象过滤</li>
 *     <li>循环引用检测</li>
 * </ul>
 * @Version: 1.0
 */
public final class LogUtils {

    /**
     * 脱敏占位符。
     * 命中敏感字段后，日志中统一显示为 ***。
     */
    private static final String MASK = "******";

    /**
     * 日志截断后的后缀标识。
     */
    private static final String TRUNCATE_SUFFIX = "...";

    /**
     * 单条日志最大长度。
     * 超过该长度时会自动截断，避免日志过长影响存储与检索。
     */
    private static final int MAX_LOG_LENGTH = 2000;

    /**
     * 默认敏感字段集合。
     * 字段名匹配时不区分大小写，内部统一转为小写后判断。
     */
    private static final Set<String> DEFAULT_MASK_FIELDS = Set.of(
            "password",
            "oldpassword",
            "newpassword",
            "confirmpassword",
            "token",
            "authorization"
    );

    /**
     * 工具类不允许实例化。
     */
    private LogUtils() {
    }

    /**
     * 生成请求参数日志。
     * <p>
     * 处理逻辑：
     * <ol>
     *     <li>过滤不可记录对象，例如文件流、请求响应对象等</li>
     *     <li>递归清洗参数内容</li>
     *     <li>对敏感字段执行脱敏</li>
     *     <li>最终序列化为 JSON 字符串并按最大长度截断</li>
     * </ol>
     *
     * @param args          Controller 或 AOP 切面中获取到的方法入参
     * @param excludeFields 额外需要脱敏的字段名数组，可为空
     * @return 处理后的请求日志字符串；若无可记录内容则返回 null
     */
    public static String toRequestLog(Object[] args, String[] excludeFields) {
        if (args == null || args.length == 0) {
            return null;
        }

        // 合并默认脱敏字段和业务自定义脱敏字段
        Set<String> maskFields = mergeMaskFields(excludeFields);
        List<Object> sanitizedArgs = new ArrayList<>();
        // 使用 IdentityHashMap 按对象引用跟踪，避免递归时出现循环引用
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        for (Object arg : args) {
            if (isFilterObject(arg)) {
                continue;
            }
            Object sanitized = sanitizeValue(arg, maskFields, visited);
            if (sanitized != null) {
                sanitizedArgs.add(sanitized);
            }
        }
        if (sanitizedArgs.isEmpty()) {
            return null;
        }
        return truncate(JSON.toJSONString(sanitizedArgs));
    }

    /**
     * 生成响应结果日志。
     * <p>
     * 对响应对象进行清洗、脱敏和截断后输出 JSON 字符串。
     *
     * @param result 接口返回值
     * @return 处理后的响应日志；若结果为空或属于过滤对象则返回 null
     */
    public static String toResponseLog(Object result) {
        if (result == null || isFilterObject(result)) {
            return null;
        }
        Object sanitized = sanitizeValue(result, DEFAULT_MASK_FIELDS, new IdentityHashMap<>());
        return sanitized == null ? null : truncate(JSON.toJSONString(sanitized));
    }

    /**
     * 生成异常摘要日志。
     * <p>
     * 输出格式通常为：异常类名: 异常消息。
     * 若异常消息为空，则仅记录异常类名。
     *
     * @param throwable 异常对象
     * @return 异常摘要字符串；为空时返回 null
     */
    public static String toErrorLog(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        String message = throwable.getMessage();
        String summary = CharSequenceUtil.isBlank(message)
                ? throwable.getClass().getSimpleName()
                : throwable.getClass().getSimpleName() + ": " + message;
        return truncate(summary);
    }

    /**
     * 判断当前对象是否属于不应写入日志的类型。
     * <p>
     * 这些对象通常包含以下风险：
     * <ul>
     *     <li>体积过大，如文件、字节数组</li>
     *     <li>无法安全序列化，如请求响应对象</li>
     *     <li>无业务日志价值，如流对象、校验结果对象</li>
     * </ul>
     *
     * @param value 待判断对象
     * @return true-需要过滤；false-允许进入日志处理
     */
    public static boolean isFilterObject(Object value) {
        return value == null
                || value instanceof File
                || value instanceof byte[]
                || value instanceof char[]
                || value instanceof MultipartFile
                || value instanceof MultipartFile[]
                || value instanceof BindingResult
                || value instanceof ServletRequest
                || value instanceof ServletResponse
                || value instanceof InputStream
                || value instanceof OutputStream;
    }

    /**
     * 合并默认脱敏字段和外部传入的自定义脱敏字段。
     *
     * @param excludeFields 额外脱敏字段
     * @return 不可变脱敏字段集合
     */
    private static Set<String> mergeMaskFields(String[] excludeFields) {
        if (excludeFields == null || excludeFields.length == 0) {
            return DEFAULT_MASK_FIELDS;
        }
        Set<String> merged = new java.util.LinkedHashSet<>(DEFAULT_MASK_FIELDS);
        for (String excludeField : excludeFields) {
            if (CharSequenceUtil.isNotBlank(excludeField)) {
                merged.add(excludeField.toLowerCase(Locale.ROOT));
            }
        }
        return Collections.unmodifiableSet(merged);
    }

    /**
     * 递归清洗对象内容。
     * <p>
     * 支持处理的对象类型包括：
     * <ul>
     *     <li>基础类型/简单值类型</li>
     *     <li>数组</li>
     *     <li>集合</li>
     *     <li>Map</li>
     *     <li>Java Bean</li>
     * </ul>
     * 同时会：
     * <ul>
     *     <li>过滤不可记录对象</li>
     *     <li>脱敏敏感字段</li>
     *     <li>处理循环引用</li>
     * </ul>
     *
     * @param value      待清洗对象
     * @param maskFields 脱敏字段集合
     * @param visited    已访问对象集合，用于防止循环引用
     * @return 清洗后的对象，可直接用于 JSON 序列化
     */
    private static Object sanitizeValue(Object value,
                                        Set<String> maskFields,
                                        IdentityHashMap<Object, Boolean> visited) {
        if (value == null || isFilterObject(value)) {
            return null;
        }

        Class<?> valueClass = value.getClass();
        // 简单值类型直接进入简单处理逻辑，例如字符串截断、枚举转 name 等
        if (isSimpleValueType(valueClass)) {
            return sanitizeSimpleValue(value);
        }

        // 检测循环引用，避免 StackOverflowError
        if (visited.containsKey(value)) {
            return "[Circular]";
        }
        visited.put(value, Boolean.TRUE);

        try {
            // 数组类型处理
            if (valueClass.isArray()) {
                int length = Array.getLength(value);
                List<Object> items = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    Object sanitized = sanitizeValue(Array.get(value, i), maskFields, visited);
                    if (sanitized != null) {
                        items.add(sanitized);
                    }
                }
                return items;
            }

            // 集合类型处理
            if (value instanceof Collection<?> collection) {
                return collection.stream()
                        .map(item -> sanitizeValue(item, maskFields, visited))
                        .filter(item -> item != null)
                        .collect(Collectors.toList());
            }

            // Map 类型处理，若 key 命中敏感字段则直接脱敏
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> sanitizedMap = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    if (maskFields.contains(key.toLowerCase(Locale.ROOT))) {
                        sanitizedMap.put(key, MASK);
                        continue;
                    }
                    Object sanitized = sanitizeValue(entry.getValue(), maskFields, visited);
                    if (sanitized != null) {
                        sanitizedMap.put(key, sanitized);
                    }
                }
                return sanitizedMap;
            }

            // 普通 Java Bean 通过反射遍历全部字段进行清洗
            Map<String, Object> beanMap = new LinkedHashMap<>();
            for (Field field : getAllFields(valueClass)) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                field.setAccessible(true);
                String fieldName = field.getName();
                if (maskFields.contains(fieldName.toLowerCase(Locale.ROOT))) {
                    beanMap.put(fieldName, MASK);
                    continue;
                }
                Object fieldValue = field.get(value);
                Object sanitized = sanitizeValue(fieldValue, maskFields, visited);
                if (sanitized != null) {
                    beanMap.put(fieldName, sanitized);
                }
            }
            return beanMap;
        } catch (IllegalAccessException ex) {
            // 反射取值失败时兜底返回对象字符串形式，避免日志处理中断主流程
            return truncate(String.valueOf(value));
        } finally {
            // 当前对象处理完成后移除访问标记，避免影响其他分支对象处理
            visited.remove(value);
        }
    }

    /**
     * 获取当前类及其父类中的全部字段。
     *
     * @param type 目标类型
     * @return 字段列表，包含继承层级中的非 Object 字段
     */
    private static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            Collections.addAll(fields, current.getDeclaredFields());
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * 清洗简单值类型。
     * <p>
     * 主要处理：
     * <ul>
     *     <li>字符串：按最大长度截断</li>
     *     <li>字符：转为字符串</li>
     *     <li>枚举：记录枚举名</li>
     *     <li>UUID：转为标准字符串</li>
     * </ul>
     *
     * @param value 简单值对象
     * @return 清洗后的值
     */
    private static Object sanitizeSimpleValue(Object value) {
        if (value instanceof CharSequence sequence) {
            return truncate(sequence.toString());
        }
        if (value instanceof Character character) {
            return character.toString();
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof UUID uuid) {
            return uuid.toString();
        }
        return value;
    }

    /**
     * 判断是否属于简单值类型。
     * <p>
     * 除了 Spring 的简单值类型判断外，还补充支持：
     * <ul>
     *     <li>Java 时间类型</li>
     *     <li>Date</li>
     *     <li>UUID</li>
     * </ul>
     *
     * @param valueClass 值类型
     * @return true-简单值；false-复杂对象
     */
    private static boolean isSimpleValueType(Class<?> valueClass) {
        return BeanUtils.isSimpleValueType(valueClass)
                || TemporalAccessor.class.isAssignableFrom(valueClass)
                || Date.class.isAssignableFrom(valueClass)
                || UUID.class.isAssignableFrom(valueClass);
    }

    /**
     * 对日志内容进行长度截断。
     *
     * @param value 原始日志内容
     * @return 截断后的日志内容；若未超长则原样返回
     */
    private static String truncate(String value) {
        if (CharSequenceUtil.isBlank(value) || value.length() <= MAX_LOG_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_LOG_LENGTH - TRUNCATE_SUFFIX.length()) + TRUNCATE_SUFFIX;
    }

    /**
     * 生成日志块字符串，格式为 [内容]。
     *
     * @param msg 日志内容
     * @return 格式化后的日志块字符串；若内容为 null 则返回 []
     */
    public static String getBlock(Object msg) {
        if (msg == null) {
            msg = "";
        }
        return "[" + msg.toString() + "]";
    }
}
