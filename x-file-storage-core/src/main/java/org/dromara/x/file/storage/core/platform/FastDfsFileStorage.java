package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import org.csource.common.MyException;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.UploadStream;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.FastDfsConfig;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.constant.FormatTemplate;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * There is no description.
 *
 * @author XS <wanghaiqi@beeplay123.com>
 * @version 1.0
 * @date 2023/10/19 11:35
 */
@Getter
@Setter
public class FastDfsFileStorage implements FileStorage {

    /**
     * FastDFS Config
     */
    private final FastDfsConfig config;

    /**
     * FastDFS Client
     */
    private final FileStorageClientFactory<StorageClient> clientFactory;

    /**
     * @param config        {@link FastDfsConfig}
     * @param clientFactory {@link FileStorageClientFactory}
     */
    public FastDfsFileStorage(FastDfsConfig config, FileStorageClientFactory<StorageClient> clientFactory) {
        this.config = config;
        this.clientFactory = clientFactory;
    }

    /**
     * 获取平台
     */
    @Override
    public String getPlatform() {
        return config.getPlatform();
    }

    /**
     * 设置平台
     *
     * @param platform
     */
    @Override
    public void setPlatform(String platform) {
        this.config.setPlatform(platform);
    }

    /**
     * 保存文件
     *
     * @param fileInfo file Info
     * @param pre      Pretreatment
     */
    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        if (fileInfo.getFileAcl() != null && pre.getNotSupportAclThrowException()) {
            throw FileStorageRuntimeException.acl(fileInfo, getPlatform());
        }
        try (InputStream in = pre.getInputStreamPlus()) {
            String[] fileUpload = clientFactory
                    .getClient()
                    .upload_file(
                            config.getGroupName(),
                            fileInfo.getSize(),
                            new UploadStream(in, fileInfo.getSize()),
                            fileInfo.getExt(),
                            getObjectMetadata(fileInfo, FileInfo::getMetadata));
            fileInfo.setUrl(StrUtil.format(
                    FormatTemplate.FULL_URL, config.getDomain(), StrUtil.join(StrPool.SLASH, (Object[]) fileUpload)));
            fileInfo.setBasePath(fileUpload[0]);
            fileInfo.setFilename(fileUpload[1]);

            // 缩略图（若包含）
            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) {
                String[] thumbnailUpload = clientFactory
                        .getClient()
                        .upload_file(
                                config.getGroupName(),
                                thumbnailBytes,
                                pre.getThumbnailSuffix(),
                                getObjectMetadata(fileInfo, FileInfo::getThMetadata));
                fileInfo.setUrl(StrUtil.format(
                        FormatTemplate.FULL_URL, config.getDomain(), StrUtil.join(StrPool.SLASH, (Object[])
                                thumbnailUpload)));
                fileInfo.setBasePath(thumbnailUpload[0]);
                fileInfo.setFilename(thumbnailUpload[1]);
            }
            return true;
        } catch (Exception e) {
            throw FileStorageRuntimeException.save(fileInfo, getPlatform(), e);
        }
    }

    /**
     * Get object metadata.
     *
     * @param fileInfo
     * @return {@link NameValuePair[]}
     */
    private NameValuePair[] getObjectMetadata(FileInfo fileInfo, Function<FileInfo, Map<String, String>> function) {
        Map<String, String> metadata = function.apply(fileInfo);
        if (CollUtil.isNotEmpty(metadata)) {
            NameValuePair[] nameValuePairs = new NameValuePair[metadata.size()];
            int index = 0;
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                nameValuePairs[index++] = new NameValuePair(entry.getKey(), entry.getValue());
            }
            return nameValuePairs;
        }
        return new NameValuePair[0];
    }

    /**
     * 删除文件
     *
     * @param fileInfo
     */
    @Override
    public boolean delete(FileInfo fileInfo) {
        try {
            int deleted = clientFactory.getClient().delete_file(config.getGroupName(), fileInfo.getFilename());
            return deleted == 0;
        } catch (Exception e) {
            throw FileStorageRuntimeException.delete(fileInfo, getPlatform(), e);
        }
    }

    /**
     * 文件是否存在
     *
     * @param fileInfo
     */
    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            org.csource.fastdfs.FileInfo fileInfo1 =
                    clientFactory.getClient().get_file_info(config.getGroupName(), fileInfo.getFilename());
            return fileInfo1 != null;
        } catch (IOException | MyException e) {
            throw FileStorageRuntimeException.exists(fileInfo, getPlatform(), e);
        }
    }

    /**
     * 下载文件
     *
     * @param fileInfo
     * @param consumer
     */
    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        try {
            byte[] bytes = clientFactory.getClient().download_file(config.getGroupName(), fileInfo.getFilename());
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)) {
                consumer.accept(byteArrayInputStream);
            }
        } catch (IOException | MyException e) {
            throw FileStorageRuntimeException.download(fileInfo, getPlatform(), e);
        }
    }

    /**
     * 下载缩略图文件
     *
     * @param fileInfo
     * @param consumer
     */
    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw FileStorageRuntimeException.downloadThNotFound(fileInfo, getPlatform());
        }

        try {
            byte[] bytes = clientFactory.getClient().download_file(config.getGroupName(), fileInfo.getThFilename());
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)) {
                consumer.accept(byteArrayInputStream);
            }
        } catch (IOException | MyException e) {
            throw FileStorageRuntimeException.downloadTh(fileInfo, getPlatform(), e);
        }
    }

    /**
     * 释放相关资源
     */
    @Override
    public void close() {
        clientFactory.close();
    }
}
