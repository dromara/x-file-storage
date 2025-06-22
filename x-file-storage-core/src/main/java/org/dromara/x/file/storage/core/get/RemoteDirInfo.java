package org.dromara.x.file.storage.core.get;

import com.azure.storage.blob.implementation.models.BlobPrefixInternal;
import com.github.sardine.DavResource;
import com.google.cloud.storage.Blob;
import com.jcraft.jsch.ChannelSftp;
import io.minio.messages.Prefix;
import java.io.File;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.net.ftp.FTPFile;
import org.dromara.x.file.storage.core.util.Tools;
import org.json.JSONObject;

/**
 * 远程目录信息
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class RemoteDirInfo {
    /**
     * 存储平台
     */
    private String platform;
    /**
     * 基础存储路径
     */
    private String basePath;
    /**
     * 存储路径
     */
    private String path;
    /**
     * 目录名称
     */
    private String name;
    /**
     * 原始数据
     */
    private Object original;

    /**
     * 获取原始数据并转换为指定类型
     */
    public <T> T getOriginal(Class<T> clazz) {
        if (original == null) return null;
        if (clazz.isInstance(original)) {
            return Tools.cast(original);
        }
        return null;
    }

    /**
     * 获取阿里云 OSS 存储平台的目录原始数据，失败返回 null，
     */
    public String getOriginalAliyunOss() {
        return getOriginal(String.class);
    }

    /**
     * 获取 Amazon S3 存储平台的目录原始数据，失败返回 null，
     */
    public String getOriginalAmazonS3() {
        return getOriginal(String.class);
    }

    /**
     * 获取 Azure Blob Storage 存储平台的目录原始数据，失败返回 null，
     */
    public BlobPrefixInternal getOriginalAzureBlobStorage() {
        return getOriginal(BlobPrefixInternal.class);
    }

    /**
     * 获取百度云 BOS 存储平台的目录原始数据，失败返回 null，
     */
    public String getOriginalBaiduBos() {
        return getOriginal(String.class);
    }

    /**
     * 获取 FTP 存储平台的目录原始数据，失败返回 null
     */
    public FTPFile getOriginalFtp() {
        return getOriginal(FTPFile.class);
    }

    /**
     * 获取 Google Cloud Storage 存储平台的目录原始数据，失败返回 null
     */
    public Blob getOriginalGoogleCloudStorage() {
        return getOriginal(Blob.class);
    }

    /**
     * 获取华为云 OBS 存储平台的目录原始数据，失败返回 null
     */
    public String getOriginalHuaweiObs() {
        return getOriginal(String.class);
    }

    /**
     * 获取本地存储平台（升级版）的目录原始数据，失败返回 null
     */
    public File getOriginalLocal() {
        return getOriginal(File.class);
    }

    /**
     * 获取 Minio 存储平台的目录原始数据，失败返回 null，
     */
    public Prefix getOriginalMinio() {
        return getOriginal(Prefix.class);
    }

    /**
     * 获取七牛云 Kodo 存储平台的目录原始数据，失败返回 null
     */
    public String getOriginalQiniuKodo() {
        return getOriginal(String.class);
    }

    /**
     * 获取 SFTP 存储平台的目录原始数据，失败返回 null
     */
    public ChannelSftp.LsEntry getOriginalSftp() {
        return getOriginal(ChannelSftp.LsEntry.class);
    }

    /**
     * 获取腾讯云 COS 存储平台的目录原始数据，失败返回 null，
     */
    public String getOriginalTencentCos() {
        return getOriginal(String.class);
    }

    /**
     * 获取又拍云 USS 存储平台的目录原始数据，失败返回 null，
     */
    public JSONObject getOriginalUpyunUss() {
        return getOriginal(JSONObject.class);
    }

    /**
     * 获取 WebDAV 存储平台的目录原始数据，失败返回 null
     */
    public DavResource getOriginalWebDav() {
        return getOriginal(DavResource.class);
    }

    /**
     * 获取 Mongo GridFS 存储平台的目录原始数据，失败返回 null，
     */
    public String getOriginalMongoGridFs() {
        return getOriginal(String.class);
    }
}
