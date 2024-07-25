package org.dromara.x.file.storage.core.util;

import cn.hutool.core.map.CaseInsensitiveMap;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class Tools {
    /**
     * 获取父路径
     */
    public static String getParent(String path) {
        if (path.endsWith("/") || path.endsWith("\\")) {
            path = path.substring(0, path.length() - 1);
        }
        int endIndex = Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"));
        return endIndex > -1 ? path.substring(0, endIndex) : null;
    }

    /**
     * 合并路径
     */
    public static String join(String... paths) {
        StringBuilder sb = new StringBuilder();
        for (String path : paths) {
            String left = sb.toString();
            boolean leftHas = left.endsWith("/") || left.endsWith("\\");
            boolean rightHas = path.startsWith("/") || path.startsWith("\\");

            if (leftHas && rightHas) {
                sb.append(path.substring(1));
            } else if (!left.isEmpty() && !leftHas && !rightHas) {
                sb.append("/").append(path);
            } else {
                sb.append(path);
            }
        }
        return sb.toString();
    }

    /**
     * 类型转换
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object object) {
        return (T) object;
    }

    /**
     * 获取流的大小（长度）
     */
    public static long getSize(InputStream in) throws IOException {
        long size = 0;
        while (in.read() != -1) size++;
        return size;
    }

    /**
     * 按照参数从前往后进行判断，返回第一个不为 null 的参数
     */
    @SafeVarargs
    public static <T> T getNotNull(T... args) {
        for (T t : args) if (t != null) return t;
        throw new NullPointerException();
    }

    /**
     * 将 Map 的 key 和 value 转换成 String 类型
     */
    public static <K, V> Map<String, String> toStringMap(Map<? extends K, ? extends V> map) {
        return stream(map, null, e -> StrUtil.toStringOrNull(e.getKey()), e -> StrUtil.toStringOrNull(e.getValue()));
    }

    /**
     * 获取枚举类型（忽略大小写）
     */
    public static <E extends Enum<E>> E getEnum(Class<E> enumClass, Object value) {
        if (value == null) return null;
        if (enumClass.isInstance(value)) {
            return Tools.cast(value);
        }
        return new CaseInsensitiveMap<>(EnumUtil.getEnumMap(enumClass)).get(value.toString());
    }

    /**
     * 通过 stream 来处理 map 并转成指定类型的新 map
     * @param map 源 map，为 null 则返回 null
     * @param callback 用来进行 stream 处理的回调函数，为 null 则跳过这一步
     * @param mapper 用来保存为 map 的映射函数，为 null 则用原来的键值
     * @return 返回一个新的 map
     * @param <K> 源 map 的键类型
     * @param <V> 源 map 的值类型
     * @param <NK>  新 map 的键类型
     * @param <NV> 新 map 的值类型
     */
    public static <K, V, NK, NV> Map<NK, NV> stream(
            Map<K, V> map,
            Function<Stream<Map.Entry<K, V>>, Stream<Map.Entry<K, V>>> callback,
            BiConsumer<Map<NK, NV>, Map.Entry<K, V>> mapper) {
        if (map == null) return null;

        if (mapper == null) {
            mapper = (nknvMap, kvEntry) -> nknvMap.put(Tools.cast(kvEntry.getKey()), Tools.cast(kvEntry.getValue()));
        }
        if (callback == null) {
            return map.entrySet().stream().collect(HashMap::new, mapper, Map::putAll);
        } else {
            return callback.apply(map.entrySet().stream()).collect(HashMap::new, mapper, Map::putAll);
        }
    }

    /**
     * 通过 stream 来处理 map 并转成指定类型的新 map
     * @param map 源 map，为 null 则返回 null
     * @param callback 用来进行 stream 处理的回调函数，为 null 则跳过这一步
     * @param keyMapper 用于生成新 map 的键的映射函数，为 null 则用原来的键
     * @param valueMapper  于生成新 map 的值的映射函数，为 null 则用原来的值
     * @return 返回一个新的 map
     * @param <K> 源 map 的键类型
     * @param <V> 源 map 的值类型
     * @param <NK>  新 map 的键类型
     * @param <NV> 新 map 的值类型
     */
    public static <K, V, NK, NV> Map<NK, NV> stream(
            Map<K, V> map,
            Function<Stream<Map.Entry<K, V>>, Stream<Map.Entry<K, V>>> callback,
            Function<Map.Entry<K, V>, NK> keyMapper,
            Function<Map.Entry<K, V>, NV> valueMapper) {

        if (keyMapper == null) keyMapper = e -> Tools.cast(e.getKey());
        if (valueMapper == null) valueMapper = e -> Tools.cast(e.getValue());
        Function<Map.Entry<K, V>, NK> finalKeyMapper = keyMapper;
        Function<Map.Entry<K, V>, NV> finalValueMapper = valueMapper;
        return stream(map, callback, (m, v) -> m.put(finalKeyMapper.apply(v), finalValueMapper.apply(v)));
    }

    /**
     * 通过 stream 来处理 map 并转成指定类型的新 map
     * @param map 源 map，为 null 则返回 null
     * @param callback 用来进行 stream 处理的回调函数，为 null 则跳过这一步
     * @param keyMapper 用于生成新 map 的键的映射函数，为 null 则用原来的键
     * @return 返回一个新的 map
     * @param <K> 源 map 的键类型
     * @param <V> 源 map 的值类型
     * @param <NK>  新 map 的键类型
     * @param <NV> 新 map 的值类型
     */
    public static <K, V, NK, NV> Map<NK, NV> stream(
            Map<K, V> map,
            Function<Stream<Map.Entry<K, V>>, Stream<Map.Entry<K, V>>> callback,
            Function<Map.Entry<K, V>, NK> keyMapper) {
        return stream(map, callback, keyMapper, null);
    }

    /**
     * 通过 stream 来处理 map 并转成指定类型的新 map
     * @param map 源 map，为 null 则返回 null
     * @param callback 用来进行 stream 处理的回调函数，为 null 则跳过这一步
     * @return 返回一个新的 map
     * @param <K> 源 map 的键类型
     * @param <V> 源 map 的值类型
     */
    public static <K, V> Map<K, V> stream(
            Map<K, V> map, Function<Stream<Map.Entry<K, V>>, Stream<Map.Entry<K, V>>> callback) {
        return stream(map, callback, (m, v) -> m.put(v.getKey(), v.getValue()));
    }
}
