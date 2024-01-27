package org.dromara.x.file.storage.core.util;

import cn.hutool.core.map.FuncKeyMap;
import cn.hutool.core.map.MapBuilder;
import cn.hutool.core.text.NamingCase;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 短横Key风格且不区分大小写的Map<br>
 * 对KEY转换为短横，以下方式都获得的值相同，put进入的值也会被覆盖<br>
 * get("ContentType")<br>
 * get("Content_Type")<br>
 * get("HelloWorld_test")<br>
 * get("Content-Type")<br>
 * get("contentType")<br>
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class KebabCaseInsensitiveMap<K, V> extends FuncKeyMap<K, V> {
    private static final long serialVersionUID = 4043263744224569870L;

    /**
     * 构造
     */
    public KebabCaseInsensitiveMap() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * 构造
     *
     * @param initialCapacity 初始大小
     */
    public KebabCaseInsensitiveMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * 构造
     *
     * @param m Map
     */
    public KebabCaseInsensitiveMap(Map<? extends K, ? extends V> m) {
        this(DEFAULT_LOAD_FACTOR, m);
    }

    /**
     * 构造
     *
     * @param loadFactor 加载因子
     * @param m          初始Map，数据会被默认拷贝到一个新的HashMap中
     */
    public KebabCaseInsensitiveMap(float loadFactor, Map<? extends K, ? extends V> m) {
        this(m.size(), loadFactor);
        this.putAll(m);
    }

    /**
     * 构造
     *
     * @param initialCapacity 初始大小
     * @param loadFactor      加载因子
     */
    public KebabCaseInsensitiveMap(int initialCapacity, float loadFactor) {
        this(MapBuilder.create(new HashMap<>(initialCapacity, loadFactor)));
    }

    /**
     * 构造<br>
     * 注意此构造将传入的Map作为被包装的Map，针对任何修改，传入的Map都会被同样修改。
     *
     * @param emptyMapBuilder Map构造器，必须构造空的Map
     */
    KebabCaseInsensitiveMap(MapBuilder<K, V> emptyMapBuilder) {
        super(emptyMapBuilder.build(), (Function<Object, K> & Serializable) (key) -> {
            if (key instanceof CharSequence) {
                key = NamingCase.toKebabCase(NamingCase.toCamelCase(key.toString()))
                        .toLowerCase();
            }
            return Tools.cast(key);
        });
    }
}
