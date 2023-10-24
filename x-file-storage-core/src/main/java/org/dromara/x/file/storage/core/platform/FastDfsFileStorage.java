package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import org.csource.common.MyException;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.StorageClient1;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.FastDfsConfig;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.file.FileWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Consumer;

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
     *
     */
    private final FastDfsConfig config;
    
    /**
     *
     */
    private final FileStorageClientFactory<StorageClient1> clientFactory;
    
    /**
     *
     */
    private String platform;
    
    private String groupName;
    
    /**
     * @param config
     * @param clientFactory
     */
    public FastDfsFileStorage(FastDfsConfig config, FileStorageClientFactory<StorageClient1> clientFactory) {
        this.platform = config.getPlatform();
        this.groupName = config.getGroupName();
        this.config = config;
        this.clientFactory = clientFactory;
    }
    
    
    /**
     * 获取平台
     */
    @Override
    public String getPlatform() {
        return platform;
    }
    
    /**
     * 设置平台
     *
     * @param platform
     */
    @Override
    public void setPlatform(String platform) {
        this.platform = platform;
    }
    
    /**
     * 保存文件
     *
     * @param fileInfo
     * @param pre
     */
    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        if (fileInfo.getFileAcl() != null && pre.getNotSupportAclThrowException()) {
            throw FileStorageRuntimeException.acl(fileInfo, platform);
        }
        FileWrapper fileWrapper = pre.getFileWrapper();
        try (InputStream in = fileWrapper.getInputStream()) {
            byte[] bytes = IoUtil.readBytes(in);
            NameValuePair[] metadata = getObjectMetadata(fileInfo);
            String strings = clientFactory.getClient().upload_file1(bytes, fileInfo.getExt(), metadata);
            
            //            byte[] thumbnailBytes = pre.getThumbnailBytes();
            //            if (thumbnailBytes != null) { //上传缩略图
            //                String newThFileKey = getThFileKey(fileInfo);
            //                fileInfo.setThUrl(domain + newThFileKey);
            //                client.upload(getAbsolutePath(basePath + fileInfo.getPath()),fileInfo.getThFilename(),new ByteArrayInputStream(thumbnailBytes));
            //            }
        } catch (IOException | MyException e) {
            throw FileStorageRuntimeException.save(fileInfo, platform, e);
        }
        return true;
    }
    
    /**
     * Get object metadata.
     *
     * @param fileInfo
     * @return {@link NameValuePair[]}
     */
    private NameValuePair[] getObjectMetadata(FileInfo fileInfo) {
        Map<String, String> metadata = fileInfo.getMetadata();
        if (CollUtil.isNotEmpty(metadata)) {
            NameValuePair[] nameValuePairs = new NameValuePair[metadata.size()];
            int index = 0;
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                nameValuePairs[index++] = new NameValuePair(entry.getKey(), entry.getValue());
            }
            return nameValuePairs;
        }
        return null;
    }
    
    /**
     * 删除文件
     *
     * @param fileInfo
     */
    @Override
    public boolean delete(FileInfo fileInfo) {
        try {
            //            if (fileInfo.getThFilename() != null) {   //删除缩略图
            //                client.delFile(getAbsolutePath(getThFileKey(fileInfo)));
            //            }
            clientFactory.getClient().delete_file(groupName, fileInfo.getFilename());
        } catch (IOException | MyException e) {
            throw FileStorageRuntimeException.delete(fileInfo, platform, e);
        }
        return true;
    }
    
    /**
     * 文件是否存在
     *
     * @param fileInfo
     */
    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            org.csource.fastdfs.FileInfo fileInfo1 = clientFactory.getClient().get_file_info(groupName, "");
            return fileInfo1 != null;
        } catch (IOException | MyException e) {
            throw FileStorageRuntimeException.exists(fileInfo, platform, e);
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
            byte[] bytes = clientFactory.getClient().download_file(groupName, "");
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)) {
                consumer.accept(byteArrayInputStream);
            }
        } catch (IOException | MyException e) {
            throw FileStorageRuntimeException.download(fileInfo, platform, e);
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
            throw FileStorageRuntimeException.downloadThNotFound(fileInfo, platform);
        }
        
        try {
            byte[] bytes = clientFactory.getClient().download_file(groupName, "");
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)) {
                consumer.accept(byteArrayInputStream);
            }
        } catch (IOException | MyException e) {
            throw FileStorageRuntimeException.downloadTh(fileInfo, platform, e);
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