package org.dromara.x.file.storage.test;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.HexUtil;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.hash.HashCalculator;
import org.dromara.x.file.storage.core.hash.HashCalculatorManager;
import org.dromara.x.file.storage.core.hash.HashInfo;
import org.junit.jupiter.api.Test;

/**
 * 哈希计算器相关测试类
 */
@Slf4j
public class HashCalculatorTest {

    public InputStream getInputStream() {
        return this.getClass().getClassLoader().getResourceAsStream("image.jpg");
    }

    @Test
    public void test() throws NoSuchAlgorithmException {
        HashCalculatorManager manager = new HashCalculatorManager()
                .setHashCalculatorMd2()
                .setHashCalculatorMd5()
                .setHashCalculatorSha1()
                .setHashCalculatorSha256()
                .setHashCalculatorSha384()
                .setHashCalculatorSha512()
                .setHashCalculator(new HashCalculator() {
                    private final MessageDigest messageDigest = MessageDigest.getInstance("MD5");

                    /**
                     * 获取哈希名称，例如 MD5、SHA1、SHA256等，详情{@link org.dromara.x.file.storage.core.constant.Constant.Hash}
                     */
                    @Override
                    public String getName() {
                        return messageDigest.getAlgorithm();
                    }

                    /**
                     * 获取哈希值，一般情况下获取后将不能继续增量计算哈希
                     */
                    @Override
                    public String getValue() {
                        return HexUtil.encodeHexStr(messageDigest.digest());
                    }

                    /**
                     * 增量计算哈希
                     * @param bytes 字节数组
                     */
                    @Override
                    public void update(byte[] bytes) {
                        messageDigest.update(bytes);
                    }
                });

        InputStream in = getInputStream();
        while (true) {
            byte[] bytes = IoUtil.readBytes(in, 1024);
            if (bytes == null || bytes.length == 0) break;
            manager.update(bytes);

            //            int b = in.read();
            //            if (b == -1) break;
            //            manager.update(new byte[] {(byte) b});
        }
        HashInfo hashInfo = manager.getHashInfo();

        log.info("哈希计算结果：{}", hashInfo);

        byte[] bytes = IoUtil.readBytes(getInputStream());
        String md2 = HexUtil.encodeHexStr(
                MessageDigest.getInstance(Constant.Hash.MessageDigest.MD2).digest(bytes));
        Assert.isTrue(md2.equalsIgnoreCase(hashInfo.getMd2()), "MD2 与实际不一致，实际：{}，计算结果为：{}", md2, hashInfo.getMd2());
        log.info("MD2 对比结果一致");

        String md5 = HexUtil.encodeHexStr(
                MessageDigest.getInstance(Constant.Hash.MessageDigest.MD5).digest(bytes));
        Assert.isTrue(md5.equalsIgnoreCase(hashInfo.getMd5()), "MD5 与实际不一致，实际：{}，计算结果为：{}", md5, hashInfo.getMd5());
        log.info("MD5 对比结果一致");

        String sha1 = HexUtil.encodeHexStr(
                MessageDigest.getInstance(Constant.Hash.MessageDigest.SHA1).digest(bytes));
        Assert.isTrue(
                sha1.equalsIgnoreCase(hashInfo.getSha1()), "SHA1 与实际不一致，实际：{}，计算结果为：{}", sha1, hashInfo.getSha1());
        log.info("SHA1 对比结果一致");

        String sha256 = HexUtil.encodeHexStr(
                MessageDigest.getInstance(Constant.Hash.MessageDigest.SHA256).digest(bytes));
        Assert.isTrue(
                sha256.equalsIgnoreCase(hashInfo.getSha256()),
                "SHA256 与实际不一致，实际：{}，计算结果为：{}",
                sha256,
                hashInfo.getSha256());
        log.info("SHA256 对比结果一致");

        String sha384 = HexUtil.encodeHexStr(
                MessageDigest.getInstance(Constant.Hash.MessageDigest.SHA384).digest(bytes));
        Assert.isTrue(
                sha384.equalsIgnoreCase(hashInfo.getSha384()),
                "SHA384 与实际不一致，实际：{}，计算结果为：{}",
                sha384,
                hashInfo.getSha384());
        log.info("SHA384 对比结果一致");

        String sha512 = HexUtil.encodeHexStr(
                MessageDigest.getInstance(Constant.Hash.MessageDigest.SHA512).digest(bytes));
        Assert.isTrue(
                sha512.equalsIgnoreCase(hashInfo.getSha512()),
                "SHA512 与实际不一致，实际：{}，计算结果为：{}",
                sha512,
                hashInfo.getSha512());
        log.info("SHA512 对比结果一致");
    }
}
