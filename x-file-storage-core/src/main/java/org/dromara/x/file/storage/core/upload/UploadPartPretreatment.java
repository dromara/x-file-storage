package org.dromara.x.file.storage.core.upload;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dromara.x.file.storage.core.*;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.hash.HashCalculator;
import org.dromara.x.file.storage.core.hash.HashCalculatorManager;
import org.dromara.x.file.storage.core.hash.HashCalculatorSetter;

import java.io.IOException;

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
     * 如果条件为 true 则：设置文件存储服务类
     * @param flag 条件
     * @param fileStorageService 文件存储服务类
     * @return 手动分片上传-上传分片预处理器
     */
    public UploadPartPretreatment setFileStorageService(boolean flag, FileStorageService fileStorageService) {
        if (flag) setFileStorageService(fileStorageService);
        return this;
    }

    /**
     * 如果条件为 true 则：设置文件信息
     * @param flag 条件
     * @param fileInfo 文件信息
     * @return 手动分片上传-上传分片预处理器
     */
    public UploadPartPretreatment setFileInfo(boolean flag, FileInfo fileInfo) {
        if (flag) setFileInfo(fileInfo);
        return this;
    }

    /**
     * 如果条件为 true 则：设置要上传的分片文件包装类
     * @param flag 条件
     * @param partFileWrapper 要上传的分片文件包装类
     * @return 手动分片上传-上传分片预处理器
     */
    public UploadPartPretreatment setPartFileWrapper(boolean flag, FileWrapper partFileWrapper) {
        if (flag) setPartFileWrapper(partFileWrapper);
        return this;
    }

    /**
     * 如果条件为 true 则：设置分片号。每一个上传的分片都有一个分片号，一般情况下取值范围是1~10000
     * @param flag 条件
     * @param partNumber 分片号。每一个上传的分片都有一个分片号，一般情况下取值范围是1~10000
     * @return 手动分片上传-上传分片预处理器
     */
    public UploadPartPretreatment setPartNumber(boolean flag, int partNumber) {
        if (flag) setPartNumber(partNumber);
        return this;
    }

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
        // [update] 20240514: InputStreamPlus检查hashCalculatorManager
        final boolean hasListenerFlag = true;
        InputStreamPlus inputStreamPlus = getInputStreamPlus(hasListenerFlag);

        if (ObjectUtil.isEmpty(inputStreamPlus.getHashCalculatorManager())) {
            inputStreamPlus = getInputStreamPlus(hasListenerFlag, this.getHashCalculatorManager());
        }

        return inputStreamPlus;
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
     * [add] 20240514: 获取增强版本的 InputStream,支持填充hashCalculatorManager
     */
    private InputStreamPlus getInputStreamPlus(boolean hasListener, HashCalculatorManager hashCalculatorManager) throws IOException {
        if (inputStreamPlus == null || inputStreamPlus.getHashCalculatorManager() == null) {
            inputStreamPlus = new InputStreamPlus(
                    partFileWrapper.getInputStream(), hasListener ? progressListener : null, partFileWrapper.getSize(), hashCalculatorManager);
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
