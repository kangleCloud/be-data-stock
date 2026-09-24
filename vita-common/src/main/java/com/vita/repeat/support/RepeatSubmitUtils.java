package com.vita.repeat.support;

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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.TemporalAccessor;
import java.util.*;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.repeat.support
 * @Author: znk
 * @CreateTime: 2026-06-17
 * @Description: 重复提交判重工具类，负责过滤不可比较对象、规范化参数并生成稳定摘要
 * @Version: 1.0
 */
public final class RepeatSubmitUtils {

    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final String FILTERED = "[Filtered]";
    private static final String CIRCULAR = "[Circular]";
    private static final Set<String> EXCLUDE_FIELDS = Set.of(
            "password",
            "oldpassword",
            "newpassword",
            "confirmpassword",
            "token",
            "authorization"
    );

    private RepeatSubmitUtils() {
    }

    /**
     * 生成方法参数摘要。
     *
     * @param args 方法参数
     * @return SHA-256 摘要字符串；当参数为空或全部被过滤时返回空串
     */
    public static String digestArgs(Object[] args) {
        Object normalized = normalizeArgs(args);
        String canonical = JSON.toJSONString(normalized);
        return sha256Hex(canonical);
    }

    /**
     * 规范化方法参数，保证同义输入生成一致的结构。
     *
     * @param args 原始参数
     * @return 规范化后的对象
     */
    public static Object normalizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return List.of();
        }
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        List<Object> normalized = new ArrayList<>();
        for (Object arg : args) {
            if (isFilterObject(arg)) {
                continue;
            }
            Object value = normalizeValue(arg, visited);
            if (value != null) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    /**
     * 构建重复提交 Redis key。
     *
     * @param httpMethod HTTP 方法
     * @param requestUri 请求路径
     * @param identity   用户标识或 IP
     * @param argsDigest 参数摘要
     * @return 完整 Redis key
     */
    public static String buildRepeatSubmitKey(String httpMethod, String requestUri, String identity, String argsDigest) {
        return "repeat_submit:" + safePart(httpMethod) + ':' + safePart(requestUri) + ':' + safePart(identity) + ':' + safePart(argsDigest);
    }

    /**
     * 判断是否属于不应进入判重摘要的对象。
     *
     * @param value 待判断对象
     * @return true 表示过滤
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

    private static Object normalizeValue(Object value, IdentityHashMap<Object, Boolean> visited) {
        if (value == null || isFilterObject(value)) {
            return null;
        }

        Class<?> valueClass = value.getClass();
        if (isSimpleValueType(valueClass)) {
            return normalizeSimpleValue(value);
        }

        if (visited.containsKey(value)) {
            return CIRCULAR;
        }
        visited.put(value, Boolean.TRUE);
        try {
            if (valueClass.isArray()) {
                int length = Array.getLength(value);
                List<Object> items = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    Object item = normalizeValue(Array.get(value, i), visited);
                    if (item != null) {
                        items.add(item);
                    }
                }
                return items;
            }
            if (value instanceof Collection<?> collection) {
                List<Object> items = new ArrayList<>(collection.size());
                for (Object item : collection) {
                    Object normalized = normalizeValue(item, visited);
                    if (normalized != null) {
                        items.add(normalized);
                    }
                }
                items.sort(Comparator.comparing(RepeatSubmitUtils::stableString));
                return items;
            }
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> normalizedMap = new TreeMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    if (EXCLUDE_FIELDS.contains(key.toLowerCase(Locale.ROOT))) {
                        normalizedMap.put(key, FILTERED);
                        continue;
                    }
                    Object normalized = normalizeValue(entry.getValue(), visited);
                    if (normalized != null) {
                        normalizedMap.put(key, normalized);
                    }
                }
                return normalizedMap;
            }

            Map<String, Object> beanMap = new TreeMap<>();
            for (Field field : getAllFields(valueClass)) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                field.setAccessible(true);
                String fieldName = field.getName();
                if (EXCLUDE_FIELDS.contains(fieldName.toLowerCase(Locale.ROOT))) {
                    beanMap.put(fieldName, FILTERED);
                    continue;
                }
                Object fieldValue = field.get(value);
                Object normalized = normalizeValue(fieldValue, visited);
                if (normalized != null) {
                    beanMap.put(fieldName, normalized);
                }
            }
            return beanMap;
        } catch (IllegalAccessException ex) {
            return stableString(value);
        } finally {
            visited.remove(value);
        }
    }

    private static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private static boolean isSimpleValueType(Class<?> valueClass) {
        return BeanUtils.isSimpleValueType(valueClass)
                || TemporalAccessor.class.isAssignableFrom(valueClass)
                || java.util.Date.class.isAssignableFrom(valueClass)
                || UUID.class.isAssignableFrom(valueClass);
    }

    private static Object normalizeSimpleValue(Object value) {
        if (value instanceof CharSequence sequence) {
            return sequence.toString();
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

    private static String sha256Hex(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                builder.append(Character.forDigit((b >>> 4) & 0xF, 16));
                builder.append(Character.forDigit(b & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256算法不可用", ex);
        }
    }

    private static String stableString(Object value) {
        return value == null ? "null" : JSON.toJSONString(value);
    }

    private static String safePart(String value) {
        return value == null || value.isBlank() ? "anonymous" : value;
    }
}
