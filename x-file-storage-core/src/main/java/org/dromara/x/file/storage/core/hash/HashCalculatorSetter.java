package org.dromara.x.file.storage.core.hash;

import static org.dromara.x.file.storage.core.constant.Constant.Hash.MessageDigest.*;

import java.security.MessageDigest;
import org.dromara.x.file.storage.core.util.Tools;

/**
 * 哈希计算器 Setter 接口
 */
public interface HashCalculatorSetter<T extends HashCalculatorSetter<?>> {

    /**
     * 添加 MD2 哈希计算器（如果条件为 true）
     * @param flag 条件
     */
    default T setHashCalculatorMd2(boolean flag) {
        return setHashCalculator(flag, MD2);
    }

    /**
     * 添加 MD2 哈希计算器
     */
    default T setHashCalculatorMd2() {
        return setHashCalculator(MD2);
    }

    /**
     * 添加 MD5 哈希计算器（如果条件为 true）
     * @param flag 条件
     */
    default T setHashCalculatorMd5(boolean flag) {
        return setHashCalculator(flag, MD5);
    }

    /**
     * 添加 MD5 哈希计算器
     */
    default T setHashCalculatorMd5() {
        return setHashCalculator(MD5);
    }

    /**
     * 添加 SHA1 哈希计算器（如果条件为 true）
     * @param flag 条件
     */
    default T setHashCalculatorSha1(boolean flag) {
        return setHashCalculator(flag, SHA1);
    }

    /**
     * 添加 SHA1 哈希计算器
     */
    default T setHashCalculatorSha1() {
        return setHashCalculator(SHA1);
    }

    /**
     * 添加 SHA256 哈希计算器（如果条件为 true）
     * @param flag 条件
     */
    default T setHashCalculatorSha256(boolean flag) {
        return setHashCalculator(flag, SHA256);
    }

    /**
     * 添加 SHA256 哈希计算器
     */
    default T setHashCalculatorSha256() {
        return setHashCalculator(SHA256);
    }

    /**
     * 添加 SHA384 哈希计算器（如果条件为 true）
     * @param flag 条件
     */
    default T setHashCalculatorSha384(boolean flag) {
        return setHashCalculator(flag, SHA384);
    }

    /**
     * 添加 SHA384 哈希计算器
     */
    default T setHashCalculatorSha384() {
        return setHashCalculator(SHA384);
    }

    /**
     * 添加 SHA512 哈希计算器（如果条件为 true）
     * @param flag 条件
     */
    default T setHashCalculatorSha512(boolean flag) {
        return setHashCalculator(flag, SHA512);
    }

    /**
     * 添加 SHA512 哈希计算器
     */
    default T setHashCalculatorSha512() {
        return setHashCalculator(SHA512);
    }

    /**
     * 添加哈希计算器（如果条件为 true）
     * @param flag 条件
     * @param name 哈希名称，例如 MD5、SHA1、SHA256等，详情{@link org.dromara.x.file.storage.core.constant.Constant.Hash.MessageDigest}
     */
    default T setHashCalculator(boolean flag, String name) {
        return setHashCalculator(flag, new MessageDigestHashCalculator(name));
    }

    /**
     * 添加哈希计算器
     * @param name 哈希名称，例如 MD5、SHA1、SHA256等，详情{@link org.dromara.x.file.storage.core.constant.Constant.Hash.MessageDigest}
     */
    default T setHashCalculator(String name) {
        return setHashCalculator(new MessageDigestHashCalculator(name));
    }

    /**
     * 添加哈希计算器
     * @param flag 条件
     * @param messageDigest 消息摘要算法
     */
    default T setHashCalculator(boolean flag, MessageDigest messageDigest) {
        return setHashCalculator(flag, new MessageDigestHashCalculator(messageDigest));
    }

    /**
     * 添加哈希计算器
     * @param messageDigest 消息摘要算法
     */
    default T setHashCalculator(MessageDigest messageDigest) {
        return setHashCalculator(new MessageDigestHashCalculator(messageDigest));
    }

    /**
     * 添加哈希计算器（如果条件为 true）
     * @param flag 条件
     * @param hashCalculator 哈希计算器
     */
    default T setHashCalculator(boolean flag, HashCalculator hashCalculator) {
        if (flag) setHashCalculator(hashCalculator);
        return Tools.cast(this);
    }

    /**
     * 添加哈希计算器
     * @param hashCalculator 哈希计算器
     */
    T setHashCalculator(HashCalculator hashCalculator);
}
