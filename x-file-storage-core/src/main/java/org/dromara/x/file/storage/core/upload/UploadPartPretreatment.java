package org.dromara.x.file.storage.core.upload;

import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.x.file.storage.core.*;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.hash.HashCalculator;
import org.dromara.x.file.storage.core.hash.HashCalculatorManager;
import org.dromara.x.file.storage.core.hash.HashCalculatorSetter;

/**
 * 手动分片上传-上传分片预处理器
 */
@Getter
@Setter
@Accessors(chain = true)
public class UploadPartPretreatment
        implements ProgressListenerSetter<UploadPartPretreatment>, HashCalculatorSetter<UploadPartPretreatment> {
    /**
     * 文件存储服务类
     */
    private FileStorageService fileStorageService;
    /**
     * 文件信息
     */
    private FileInfo fileInfo;
    /**
     * 要上传的分片文件包装类
     */
    private FileWrapper partFileWrapper;
    /**
     * 分片号。每一个上传的分片都有一个分片号，一般情况下取值范围是1~10000
     */
    private int partNumber;
    /**
     * 上传进度监听
     */
    private ProgressListener progressListener;
    /**
     * 哈希计算器管理器
     */
    private HashCalculatorManager hashCalculatorManager = new HashCalculatorManager();

    /**
     * 传时用的增强版本的 InputStream ，可以带进度监听、计算哈希等功能，仅内部使用
     */
    private InputStreamPlus inputStreamPlus;

    /**
     * 添加一个哈希计算器
     * @param hashCalculator 哈希计算器
     */
    @Override
    public UploadPartPretreatment setHashCalculator(HashCalculator hashCalculator) {
        hashCalculatorManager.setHashCalculator(hashCalculator);
        return this;
    }

    /**
     * 设置哈希计算器管理器（如果条件为 true）
     * @param flag 条件
     * @param hashCalculatorManager 哈希计算器管理器
     */
    public UploadPartPretreatment setHashCalculatorManager(boolean flag, HashCalculatorManager hashCalculatorManager) {
        if (flag) setHashCalculatorManager(hashCalculatorManager);
        return this;
    }

    /**
     * 获取增强版本的 InputStream ，可以带进度监听、计算哈希等功能
     */
    public InputStreamPlus getInputStreamPlus() throws IOException {
        return getInputStreamPlus(true);
    }

    /**
     * 获取增强版本的 InputStream ，可以带进度监听、计算哈希等功能
     */
    public InputStreamPlus getInputStreamPlus(boolean hasListener) throws IOException {
        if (inputStreamPlus == null) {
            inputStreamPlus = new InputStreamPlus(
                    partFileWrapper.getInputStream(), hasListener ? progressListener : null, partFileWrapper.getSize());
        }
        return inputStreamPlus;
    }

    /**
     * 执行上传
     */
    public FilePartInfo upload() {
        return new UploadPartActuator(this).execute();
    }
}
