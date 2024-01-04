package org.dromara.x.file.storage.core.hash;

import cn.hutool.core.util.StrUtil;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;

/**
 * 哈希计算器管理器
 */
public class HashCalculatorManager implements HashCalculatorSetter<HashCalculatorManager> {
    private final List<HashCalculator> hashCalculatorList = new CopyOnWriteArrayList<>();
    private volatile HashInfo hashInfo;

    /**
     * 添加一个哈希计算器
     * @param hashCalculator 哈希计算器
     * @return 哈希计算器管理器
     */
    public HashCalculatorManager setHashCalculator(HashCalculator hashCalculator) {
        hashCalculatorList.add(hashCalculator);
        return this;
    }

    /**
     * 增量计算哈希
     * @param bytes 字节数组
     * @return 哈希计算器管理器
     */
    public HashCalculatorManager update(byte[] bytes) {
        if (hashInfo != null) {
            throw new FileStorageRuntimeException(
                    StrUtil.format("当前 HashCalculatorManager 已调用 getHashInfo() 方法获取了哈希信息，无法再次进行增量计算"));
        }
        for (HashCalculator hashCalculator : hashCalculatorList) {
            hashCalculator.update(bytes);
        }
        return this;
    }

    /**
     * 获取哈希信息，注意：此方法一旦调用过后，将无法再次增量计算哈希
     * @return 哈希信息
     */
    public HashInfo getHashInfo() {
        if (hashInfo == null) {
            synchronized (this) {
                if (hashInfo == null) {
                    hashInfo = new HashInfo();
                    for (HashCalculator hashCalculator : hashCalculatorList) {
                        hashInfo.put(hashCalculator.getName(), hashCalculator.getValue());
                    }
                }
            }
        }
        return hashInfo;
    }
}
