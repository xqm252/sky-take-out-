package com.sky.agent.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Java 8 兼容工具方法——替代 Java 9/10/11 新增的 API
 */
public final class Collections8 {

    private Collections8() {}

    // ===== Map/List 工厂方法 (Java 9+) =====

    /** 空 Map */
    public static <K, V> Map<K, V> mapOf() {
        return Collections.emptyMap();
    }

    /** 单元素 Map */
    public static <K, V> Map<K, V> mapOf(K k1, V v1) {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        return map;
    }

    /** 双元素 Map */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2) {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    /** 三元素 Map */
    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    /** 空 List */
    public static <T> List<T> listOf() {
        return Collections.emptyList();
    }

    /** 变长 List */
    @SafeVarargs
    public static <T> List<T> listOf(T... items) {
        return Collections.unmodifiableList(Arrays.asList(items));
    }

    // ===== 字符串方法 =====

    /** 判断字符串是否空白（Java 11 String.isBlank 兼容版） */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /** 重复字符串 count 次（Java 11 String.repeat 兼容版） */
    public static String repeat(String str, int count) {
        if (count <= 0) return "";
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    // ===== IO 方法 =====

    /** 从输入流读取所有字节（Java 9 InputStream.readAllBytes 兼容版） */
    public static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = in.read(chunk)) != -1) {
            buf.write(chunk, 0, n);
        }
        return buf.toByteArray();
    }
}
