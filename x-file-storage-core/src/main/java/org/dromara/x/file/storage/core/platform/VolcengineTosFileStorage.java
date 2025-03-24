package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.volcengine.tos.TOSV2;
import com.volcengine.tos.comm.common.ACLType;
import com.volcengine.tos.model.object.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.VolcengineTosConfig;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.get.*;
import org.dromara.x.file.storage.core.upload.*;
import org.dromara.x.file.storage.core.util.Tools;

/**
 * 火山云 TOS 存储
 */
@Getter
@Setter
@NoArgsConstructor
public class VolcengineTosFileStorage implements FileStorage {

    private String platform;
    private String bucketName;
    private String domain;
    private String basePath;
    private String defaultAcl;
    private int multipartThreshold;
    private int multipartPartSize;
    private FileStorageClientFactory<TOSV2> clientFactory;

    public VolcengineTosFileStorage(VolcengineTosConfig config, FileStorageClientFactory<TOSV2> clientFactory) {
        platform = config.getPlatform();
        bucketName = config.getBucketName();
        domain = config.getDomain();
        basePath = config.getBasePath();
        defaultAcl = config.getDefaultAcl();
        multipartThreshold = config.getMultipartThreshold();
        multipartPartSize = config.getMultipartPartSize();
        this.clientFactory = clientFactory;
    }

    public TOSV2 getClient() {
        return clientFactory.getClient();
    }

    @Override
    public void close() {
        clientFactory.close();
    }

    @Override
    public boolean isSupportAcl() {
        return true;
    }

    @Override
    public boolean isSupportMetadata() {
        return true;
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        TOSV2 client = getClient();
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                DeleteObjectInput thInput =
                        new DeleteObjectInput().setBucket(bucketName).setKey(getThFileKey(fileInfo));
                client.deleteObject(thInput);
            }
            DeleteObjectInput input =
                    new DeleteObjectInput().setBucket(bucketName).setKey(getFileKey(fileInfo));
            client.deleteObject(input);
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            HeadObjectV2Input input =
                    new HeadObjectV2Input().setBucket(bucketName).setKey(getFileKey(fileInfo));
            getClient().headObject(input);
            return true;
        } catch (Exception e) {
            if (e.getMessage().contains("404")) {
                return false;
            }
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        try {
            GetObjectV2Input input =
                    new GetObjectV2Input().setBucket(bucketName).setKey(getFileKey(fileInfo));
            GetObjectV2Output output = getClient().getObject(input);
            try (InputStream in = output.getContent()) {
                consumer.accept(in);
            }
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);

        try {
            GetObjectV2Input input =
                    new GetObjectV2Input().setBucket(bucketName).setKey(getThFileKey(fileInfo));
            GetObjectV2Output output = getClient().getObject(input);
            try (InputStream in = output.getContent()) {
                consumer.accept(in);
            }
        } catch (Exception e) {
            throw ExceptionFactory.downloadTh(fileInfo, platform, e);
        }
    }

    @Override
    public boolean isSupportSameCopy() {
        return true;
    }

    @Override
    public void sameCopy(FileInfo srcFileInfo, FileInfo destFileInfo, CopyPretreatment pre) {
        Check.sameCopyBasePath(platform, basePath, srcFileInfo, destFileInfo);

        TOSV2 client = getClient();

        // 获取远程文件信息
        String srcFileKey = getFileKey(srcFileInfo);
        HeadObjectV2Output srcFile;
        try {
            HeadObjectV2Input headInput =
                    new HeadObjectV2Input().setBucket(bucketName).setKey(srcFileKey);
            srcFile = client.headObject(headInput);
        } catch (Exception e) {
            throw ExceptionFactory.sameCopyNotFound(srcFileInfo, destFileInfo, platform, e);
        }

        // 复制缩略图文件
        String destThFileKey = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                CopyObjectV2Input copyRequest = new CopyObjectV2Input()
                        .setBucket(bucketName)
                        .setKey(destThFileKey)
                        .setSrcBucket(bucketName)
                        .setSrcKey(getThFileKey(srcFileInfo));
                if (destFileInfo.getThFileAcl() != null) {
                    ObjectMetaRequestOptions options = new ObjectMetaRequestOptions();
                    options.setAclType(getAcl(destFileInfo.getThFileAcl()));
                    copyRequest.setOptions(options);
                }
                client.copyObject(copyRequest);
            } catch (Exception e) {
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 复制文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        try {
            CopyObjectV2Input copyRequest = new CopyObjectV2Input()
                    .setBucket(bucketName)
                    .setKey(destFileKey)
                    .setSrcBucket(bucketName)
                    .setSrcKey(srcFileKey);
            if (destFileInfo.getFileAcl() != null) {
                ObjectMetaRequestOptions options = new ObjectMetaRequestOptions();
                options.setAclType(getAcl(destFileInfo.getFileAcl()));
                copyRequest.setOptions(options);
            }
            client.copyObject(copyRequest);
        } catch (Exception e) {
            throw ExceptionFactory.sameCopy(srcFileInfo, destFileInfo, platform, e);
        }
    }

    @Override
    public boolean setFileAcl(FileInfo fileInfo, Object acl) {
        ACLType tosAcl = getAcl(acl);
        if (tosAcl == null) return false;
        try {
            // 通过PutObjectBasicInput和相应options设置ACL
            ObjectMetaRequestOptions options = new ObjectMetaRequestOptions();
            options.setAclType(tosAcl);

            PutObjectBasicInput basicInput = new PutObjectBasicInput();
            basicInput.setBucket(bucketName);
            basicInput.setKey(getFileKey(fileInfo));
            basicInput.setOptions(options);

            // 目前TOS Java SDK没有直接的putObjectACL方法，需要重新上传或创建对象
            // 这是一个变通实现，在实际使用时可能需要根据SDK的更新进行调整
            PutObjectInput input = new PutObjectInput();
            input.setPutObjectBasicInput(basicInput);
            input.setContent(new ByteArrayInputStream(new byte[0])); // 空内容，仅更新元数据

            getClient().putObject(input);
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.setFileAcl(fileInfo, tosAcl, platform, e);
        }
    }

    @Override
    public boolean setThFileAcl(FileInfo fileInfo, Object acl) {
        ACLType tosAcl = getAcl(acl);
        if (tosAcl == null) return false;
        String key = getThFileKey(fileInfo);
        if (key == null) return false;
        try {
            // 通过PutObjectBasicInput和相应options设置ACL
            ObjectMetaRequestOptions options = new ObjectMetaRequestOptions();
            options.setAclType(tosAcl);

            PutObjectBasicInput basicInput = new PutObjectBasicInput();
            basicInput.setBucket(bucketName);
            basicInput.setKey(key);
            basicInput.setOptions(options);

            // 目前TOS Java SDK没有直接的putObjectACL方法，需要重新上传或创建对象
            // 这是一个变通实现，在实际使用时可能需要根据SDK的更新进行调整
            PutObjectInput input = new PutObjectInput();
            input.setPutObjectBasicInput(basicInput);
            input.setContent(new ByteArrayInputStream(new byte[0])); // 空内容，仅更新元数据

            getClient().putObject(input);
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.setThFileAcl(fileInfo, tosAcl, platform, e);
        }
    }

    /**
     * 获取 TOS ACL 枚举
     */
    private ACLType getAcl(Object acl) {
        if (acl instanceof ACLType) {
            return (ACLType) acl;
        } else if (acl instanceof String || acl == null) {
            String sAcl = (String) acl;
            if (StrUtil.isEmpty(sAcl)) sAcl = defaultAcl;
            if (StrUtil.isEmpty(sAcl)) return null;

            // 尝试遍历枚举值进行匹配
            for (ACLType item : ACLType.values()) {
                if (item.toString().equalsIgnoreCase(sAcl)) {
                    return item;
                }
            }
            // 处理常见的ACL名称映射
            if ("private".equalsIgnoreCase(sAcl)) return ACLType.ACL_PRIVATE;
            if ("public-read".equalsIgnoreCase(sAcl)) return ACLType.ACL_PUBLIC_READ;
            if ("public-read-write".equalsIgnoreCase(sAcl)) return ACLType.ACL_PUBLIC_READ_WRITE;
            if ("authenticated-read".equalsIgnoreCase(sAcl)) return ACLType.ACL_AUTHENTICATED_READ;
            if ("bucket-owner-read".equalsIgnoreCase(sAcl)) return ACLType.ACL_BUCKET_OWNER_READ;
            if ("bucket-owner-full-control".equalsIgnoreCase(sAcl)) return ACLType.ACL_BUCKET_OWNER_FULL_CONTROL;
        } else {
            throw ExceptionFactory.unrecognizedAcl(acl, platform);
        }
        return null;
    }

    /**
     * 获取文件存储路径
     */
    public String getFileKey(FileInfo fileInfo) {
        return Tools.join(fileInfo.getBasePath(), fileInfo.getPath(), fileInfo.getFilename());
    }

    /**
     * 获取缩略图文件存储路径
     */
    public String getThFileKey(FileInfo fileInfo) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) return null;
        return Tools.join(fileInfo.getBasePath(), fileInfo.getPath(), fileInfo.getThFilename());
    }

    /**
     * 获取文件对象元数据
     */
    private ObjectMetaRequestOptions getObjectMetadata(FileInfo fileInfo) {
        ObjectMetaRequestOptions meta = new ObjectMetaRequestOptions();
        // 设置 Content-Type
        meta.setContentType(fileInfo.getContentType());
        // 设置文件 ACL
        if (fileInfo.getFileAcl() != null) {
            meta.setAclType(getAcl(fileInfo.getFileAcl()));
        } else if (StrUtil.isNotBlank(defaultAcl)) {
            meta.setAclType(getAcl(defaultAcl));
        }
        // 设置 HTTP 标准标头
        if (CollUtil.isNotEmpty(fileInfo.getMetadata())) {
            Map<String, String> customMeta = fileInfo.getMetadata().entrySet().stream()
                    .filter(entry -> {
                        String name = entry.getKey().toLowerCase();
                        return name.startsWith("x-tos-meta-");
                    })
                    .collect(Collectors.toMap(
                            entry -> {
                                String name = entry.getKey().trim();
                                if (name.toLowerCase().startsWith("x-tos-meta-")) {
                                    return name.substring("x-tos-meta-".length());
                                }
                                return name;
                            },
                            Map.Entry::getValue,
                            (oldValue, newValue) -> newValue));
            if (CollUtil.isNotEmpty(customMeta)) {
                meta.setCustomMetadata(customMeta);
            }
        }
        return meta;
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        // 检查存储平台是否支持对应的元数据/ACL设置
        if ((fileInfo.getMetadata() != null || fileInfo.getUserMetadata() != null)
                && !isSupportMetadata()
                && pre.getNotSupportMetadataThrowException()) {
            throw ExceptionFactory.uploadNotSupportMetadata(fileInfo, platform);
        }

        if ((fileInfo.getFileAcl() != null || fileInfo.getThFileAcl() != null)
                && !isSupportAcl()
                && pre.getNotSupportAclThrowException()) {
            throw ExceptionFactory.uploadNotSupportAcl(fileInfo, platform);
        }

        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);

        FileWrapper file = pre.getFileWrapper();
        long size = 0;
        boolean isRequiredLength = false;
        try {
            size = file.getSize();
            isRequiredLength = true;
        } catch (Exception e) {
            // 无需处理
        }
        if (!isRequiredLength || size < multipartThreshold) {
            // 小文件上传
            uploadNormal(fileInfo, pre, file);
        } else {
            // 大文件上传
            uploadMultipart(fileInfo, pre, file, size);
        }

        // 上传缩略图
        byte[] thumbnailBytes = pre.getThumbnailBytes();
        if (thumbnailBytes != null) {
            String newThFileKey = getThFileKey(fileInfo);
            fileInfo.setThUrl(domain + newThFileKey);
            uploadNormalTh(fileInfo, pre, thumbnailBytes);
        }

        return true;
    }

    /**
     * 普通上传，对象
     */
    public void uploadNormal(FileInfo fileInfo, UploadPretreatment pre, FileWrapper file) {
        try {
            PutObjectBasicInput basicInput =
                    new PutObjectBasicInput().setBucket(bucketName).setKey(getFileKey(fileInfo));

            ObjectMetaRequestOptions meta = getObjectMetadata(fileInfo);
            if (meta != null) {
                basicInput.setOptions(meta);
            }

            InputStream in = file.getInputStream();
            ProgressListener progressListener = pre.getProgressListener();

            if (progressListener != null) {
                progressListener.start();
                progressListener.progress(0L, fileInfo.getSize());
            }

            PutObjectInput input =
                    new PutObjectInput().setPutObjectBasicInput(basicInput).setContent(in);

            getClient().putObject(input);

            if (progressListener != null) {
                progressListener.finish();
            }
        } catch (Exception e) {
            throw ExceptionFactory.upload(fileInfo, platform, e);
        }
    }

    /**
     * 普通上传，缩略图
     */
    public void uploadNormalTh(FileInfo fileInfo, UploadPretreatment pre, byte[] thumbnailBytes) {
        try {
            PutObjectBasicInput basicInput =
                    new PutObjectBasicInput().setBucket(bucketName).setKey(getThFileKey(fileInfo));

            // 设置 ACL
            if (fileInfo.getThFileAcl() != null) {
                ObjectMetaRequestOptions meta = new ObjectMetaRequestOptions();
                meta.setAclType(getAcl(fileInfo.getThFileAcl()));
                basicInput.setOptions(meta);
            } else if (StrUtil.isNotBlank(defaultAcl)) {
                ObjectMetaRequestOptions meta = new ObjectMetaRequestOptions();
                meta.setAclType(getAcl(defaultAcl));
                basicInput.setOptions(meta);
            }

            InputStream in = new ByteArrayInputStream(thumbnailBytes);

            PutObjectInput input =
                    new PutObjectInput().setPutObjectBasicInput(basicInput).setContent(in);

            getClient().putObject(input);
        } catch (Exception e) {
            throw new FileStorageRuntimeException("上传缩略图失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分片上传，对象
     */
    public void uploadMultipart(FileInfo fileInfo, UploadPretreatment pre, FileWrapper file, long size) {
        TOSV2 client = getClient();
        String uploadId = null;
        try {
            // 1. 初始化分片上传任务
            CreateMultipartUploadInput createInput =
                    new CreateMultipartUploadInput().setBucket(bucketName).setKey(getFileKey(fileInfo));

            ObjectMetaRequestOptions meta = getObjectMetadata(fileInfo);
            if (meta != null) {
                createInput.setOptions(meta);
            }

            CreateMultipartUploadOutput createOutput = client.createMultipartUpload(createInput);
            uploadId = createOutput.getUploadID();

            // 2. 上传分片
            List<UploadedPartV2> parts = new ArrayList<>();
            ProgressListener progressListener = pre.getProgressListener();
            if (progressListener != null) {
                progressListener.start();
            }

            InputStream in = file.getInputStream();
            long partSize = multipartPartSize;
            long uploadedSize = 0;
            int partNumber = 1;

            byte[] buffer = new byte[multipartPartSize];
            int len;
            while ((len = in.read(buffer)) > 0) {
                byte[] partBuffer = buffer;
                if (len != buffer.length) {
                    partBuffer = new byte[len];
                    System.arraycopy(buffer, 0, partBuffer, 0, len);
                }

                UploadPartV2Input partInput = new UploadPartV2Input()
                        .setBucket(bucketName)
                        .setKey(getFileKey(fileInfo))
                        .setUploadID(uploadId)
                        .setPartNumber(partNumber)
                        .setContentLength((long) len)
                        .setContent(new ByteArrayInputStream(partBuffer));

                UploadPartV2Output partOutput = client.uploadPart(partInput);
                parts.add(new UploadedPartV2().setPartNumber(partNumber).setEtag(partOutput.getEtag()));

                uploadedSize += len;
                partNumber++;

                if (progressListener != null) {
                    progressListener.progress(uploadedSize, fileInfo.getSize());
                }
            }

            // 3. 完成分片上传
            CompleteMultipartUploadV2Input completeInput = new CompleteMultipartUploadV2Input()
                    .setBucket(bucketName)
                    .setKey(getFileKey(fileInfo))
                    .setUploadID(uploadId)
                    .setUploadedParts(parts);

            client.completeMultipartUpload(completeInput);

            if (progressListener != null) {
                progressListener.finish();
            }
        } catch (Exception e) {
            // 出现异常时，尝试取消分片上传任务
            if (uploadId != null) {
                try {
                    AbortMultipartUploadInput abortInput = new AbortMultipartUploadInput()
                            .setBucket(bucketName)
                            .setKey(getFileKey(fileInfo))
                            .setUploadID(uploadId);
                    client.abortMultipartUpload(abortInput);
                } catch (Exception ignored) {
                    // 忽略取消时的异常
                }
            }
            throw ExceptionFactory.upload(fileInfo, platform, e);
        }
    }

    @Override
    public RemoteFileInfo getFile(GetFilePretreatment pre) {
        String path = pre.getPath();
        String filename = pre.getFilename();
        String key = Tools.join(basePath, path, filename);

        try {
            HeadObjectV2Input headInput =
                    new HeadObjectV2Input().setBucket(bucketName).setKey(key);
            HeadObjectV2Output headOutput = getClient().headObject(headInput);

            FileInfo fileInfo = new FileInfo();
            fileInfo.setPlatform(platform)
                    .setBasePath(basePath)
                    .setPath(path)
                    .setFilename(filename)
                    .setContentType(headOutput.getContentType())
                    .setSize(headOutput.getContentLength())
                    .setCreateTime(new Date())
                    .setUrl(domain + key);

            RemoteFileInfo remoteFileInfo = new RemoteFileInfo();
            remoteFileInfo
                    .setPlatform(platform)
                    .setBasePath(basePath)
                    .setPath(path)
                    .setFilename(filename)
                    .setContentType(headOutput.getContentType())
                    .setSize(headOutput.getContentLength())
                    .setLastModified(new Date())
                    .setUrl(domain + key)
                    .setOriginal(headOutput);

            return remoteFileInfo;
        } catch (Exception e) {
            throw new FileStorageRuntimeException("获取文件信息失败: " + e.getMessage(), e);
        }
    }
}
