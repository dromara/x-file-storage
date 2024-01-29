package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.NamingCase;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.StrUtil;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.*;
import com.google.cloud.storage.Storage.CopyRequest;
import com.google.cloud.storage.Storage.PredefinedAcl;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.GoogleCloudStorageConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.get.GetFilePretreatment;
import org.dromara.x.file.storage.core.get.RemoteFileInfo;

/**
 * GoogleCloud Storage 存储
 *
 * @author Kytrun Xuyanwu
 * @version 1.0
 * {@code @date} 2022/11/4 9:56
 */
@Getter
@Setter
@NoArgsConstructor
public class GoogleCloudStorageFileStorage implements FileStorage {
    private String projectId;
    private String bucketName;
    private String credentialsPath;
    private String basePath;
    private String platform;
    private String domain;
    private String defaultAcl;
    private FileStorageClientFactory<Storage> clientFactory;

    public GoogleCloudStorageFileStorage(
            GoogleCloudStorageConfig config, FileStorageClientFactory<Storage> clientFactory) {
        platform = config.getPlatform();
        bucketName = config.getBucketName();
        domain = config.getDomain();
        basePath = config.getBasePath();
        defaultAcl = config.getDefaultAcl();
        this.clientFactory = clientFactory;
    }

    public Storage getClient() {
        return clientFactory.getClient();
    }

    @Override
    public void close() {
        clientFactory.close();
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        ArrayList<Storage.BlobWriteOption> optionList = new ArrayList<>();
        BlobInfo.Builder blobInfoBuilder = BlobInfo.newBuilder(bucketName, newFileKey);
        setMetadata(blobInfoBuilder, fileInfo, optionList);
        Storage client = getClient();

        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            // 上传原文件
            client.createFrom(blobInfoBuilder.build(), in, optionList.toArray(new Storage.BlobWriteOption[] {}));
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());

            // 上传缩略图
            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) {
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                ArrayList<Storage.BlobWriteOption> thOptionList = new ArrayList<>();
                BlobInfo.Builder thBlobInfoBuilder = BlobInfo.newBuilder(bucketName, newThFileKey);
                setThMetadata(thBlobInfoBuilder, fileInfo, thOptionList);
                client.createFrom(
                        thBlobInfoBuilder.build(),
                        new ByteArrayInputStream(thumbnailBytes),
                        thOptionList.toArray(new Storage.BlobWriteOption[] {}));
            }
            return true;
        } catch (Exception e) {
            try {
                checkAndDelete(newFileKey);
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.upload(fileInfo, platform, e);
        }
    }

    @Override
    public RemoteFileInfo getFile(GetFilePretreatment pre) {
        Storage client = getClient();
        try {
            Blob file;
            try {
                file = client.get(bucketName, basePath + pre.getPath() + pre.getFilename());
            } catch (Exception e) {
                return null;
            }
            RemoteFileInfo info = new RemoteFileInfo();
            info.setPlatform(pre.getPlatform());
            info.setBasePath(basePath);
            info.setPath(pre.getPath());
            info.setFilename(FileNameUtil.getName(file.getName()));
            info.setSize(file.getSize());
            info.setExt(FileNameUtil.extName(info.getFilename()));
            info.setETag(file.getEtag());
            info.setContentDisposition(file.getContentDisposition());
            info.setContentType(file.getContentType());
            info.setContentMd5(file.getMd5());
            info.setLastModified(DateUtil.date(file.getUpdateTimeOffsetDateTime()));
            HashMap<String, Object> metadata = new HashMap<>();
            if (file.getContentType() != null) metadata.put(Constant.Metadata.CONTENT_TYPE, file.getContentType());
            if (file.getContentEncoding() != null)
                metadata.put(Constant.Metadata.CONTENT_ENCODING, file.getContentEncoding());
            if (file.getContentDisposition() != null)
                metadata.put(Constant.Metadata.CONTENT_DISPOSITION, file.getContentDisposition());
            if (file.getContentLanguage() != null)
                metadata.put(Constant.Metadata.CONTENT_LANGUAGE, file.getContentLanguage());
            if (file.getStorageClass() != null) metadata.put("Storage-Class", file.getStorageClass());
            if (file.getSize() != null) metadata.put(Constant.Metadata.CONTENT_LENGTH, file.getSize());
            if (file.getMd5() != null) metadata.put(Constant.Metadata.CONTENT_MD5, file.getMd5());
            if (file.getEtag() != null) metadata.put("E-Tag", file.getEtag());
            if (file.getUpdateTimeOffsetDateTime() != null)
                metadata.put(
                        Constant.Metadata.LAST_MODIFIED,
                        DateUtil.formatHttpDate(DateUtil.date(file.getUpdateTimeOffsetDateTime())));
            info.setMetadata(metadata);
            if (file.getMetadata() != null) info.setUserMetadata(new HashMap<>(file.getMetadata()));
            info.setOriginal(file);
            return info;
        } catch (Exception e) {
            throw ExceptionFactory.getFile(pre, basePath, e);
        }
    }

    /**
     * 获取文件的访问控制列表，这里又分为 PredefinedAcl 和 List<ACL>
     */
    public AclWrapper getAcl(Object acl) {
        if (acl instanceof PredefinedAcl) {
            return new AclWrapper((PredefinedAcl) acl);
        } else if (acl instanceof String || acl == null) {
            String sAcl = (String) acl;
            if (StrUtil.isEmpty(sAcl)) sAcl = defaultAcl;
            if (StrUtil.isEmpty(sAcl)) return null;
            sAcl = sAcl.replace("-", "_");
            for (PredefinedAcl item : PredefinedAcl.values()) {
                if (item.toString().equalsIgnoreCase(sAcl)) {
                    return new AclWrapper(item);
                }
            }
            return null;
        } else if (acl instanceof Acl) {
            return new AclWrapper(Collections.singletonList((Acl) acl));
        } else if (acl instanceof Collection) {
            List<Acl> aclList = ((Collection<?>) acl)
                    .stream()
                            .map(item -> {
                                if (item instanceof Acl) {
                                    return (Acl) item;
                                } else {
                                    throw new FileStorageRuntimeException("不支持的ACL：" + item);
                                }
                            })
                            .collect(Collectors.toList());
            return new AclWrapper(aclList);
        } else {
            throw ExceptionFactory.unrecognizedAcl(acl, platform);
        }
    }

    /**
     * 设置对象的元数据
     */
    public void setMetadata(
            BlobInfo.Builder blobInfoBuilder, FileInfo fileInfo, ArrayList<Storage.BlobWriteOption> optionList) {
        blobInfoBuilder.setContentType(fileInfo.getContentType()).setMetadata(fileInfo.getUserMetadata());
        if (CollUtil.isNotEmpty(fileInfo.getMetadata())) {
            CopyOptions copyOptions = CopyOptions.create()
                    .ignoreCase()
                    .setFieldNameEditor(name -> NamingCase.toCamelCase(name, CharUtil.DASHED));
            BeanUtil.copyProperties(fileInfo.getMetadata(), blobInfoBuilder, copyOptions);
        }
        AclWrapper fileAcl = getAcl(fileInfo.getFileAcl());
        if (fileAcl != null) {
            if (fileAcl.getAclList() != null) {
                blobInfoBuilder.setAcl(fileAcl.getAclList());
            } else if (fileAcl.getPredefinedAcl() != null) {
                optionList.add(Storage.BlobWriteOption.predefinedAcl(fileAcl.getPredefinedAcl()));
            }
        }
    }

    /**
     * 设置缩略图对象的元数据
     */
    public void setThMetadata(
            BlobInfo.Builder blobInfoBuilder, FileInfo fileInfo, ArrayList<Storage.BlobWriteOption> optionList) {
        blobInfoBuilder.setContentType(fileInfo.getThContentType()).setMetadata(fileInfo.getThUserMetadata());
        if (CollUtil.isNotEmpty(fileInfo.getThMetadata())) {
            CopyOptions copyOptions = CopyOptions.create()
                    .ignoreCase()
                    .setFieldNameEditor(name -> NamingCase.toCamelCase(name, CharUtil.DASHED));
            BeanUtil.copyProperties(fileInfo.getThMetadata(), blobInfoBuilder, copyOptions);
        }
        AclWrapper fileAcl = getAcl(fileInfo.getThFileAcl());
        if (fileAcl != null) {
            if (fileAcl.getAclList() != null) {
                blobInfoBuilder.setAcl(fileAcl.getAclList());
            } else if (fileAcl.getPredefinedAcl() != null) {
                optionList.add(Storage.BlobWriteOption.predefinedAcl(fileAcl.getPredefinedAcl()));
            }
        }
    }

    @Override
    public boolean isSupportAcl() {
        return true;
    }

    @Override
    public boolean setFileAcl(FileInfo fileInfo, Object acl) {
        AclWrapper oAcl = getAcl(acl);
        if (oAcl == null) return false;
        try {
            BlobInfo.Builder builder = BlobInfo.newBuilder(bucketName, getFileKey(fileInfo));
            if (oAcl.getAclList() != null) {
                builder.setAcl(oAcl.getAclList());
                getClient().update(builder.build());
                return true;
            } else if (oAcl.getPredefinedAcl() != null) {
                getClient().update(builder.build(), Storage.BlobTargetOption.predefinedAcl(oAcl.getPredefinedAcl()));
                return true;
            }
            return false;
        } catch (Exception e) {
            throw ExceptionFactory.setFileAcl(fileInfo, oAcl, platform, e);
        }
    }

    @Override
    public boolean setThFileAcl(FileInfo fileInfo, Object acl) {
        AclWrapper oAcl = getAcl(acl);
        if (oAcl == null) return false;
        try {
            BlobInfo.Builder builder = BlobInfo.newBuilder(bucketName, getThFileKey(fileInfo));
            if (oAcl.getAclList() != null) {
                builder.setAcl(oAcl.getAclList());
                getClient().update(builder.build());
                return true;
            } else if (oAcl.getPredefinedAcl() != null) {
                getClient().update(builder.build(), Storage.BlobTargetOption.predefinedAcl(oAcl.getPredefinedAcl()));
                return true;
            }
            return false;
        } catch (Exception e) {
            throw ExceptionFactory.setThFileAcl(fileInfo, oAcl, platform, e);
        }
    }

    @Override
    public boolean isSupportPresignedUrl() {
        return true;
    }

    @Override
    public String generatePresignedUrl(FileInfo fileInfo, Date expiration) {
        try {
            BlobInfo blobInfo =
                    BlobInfo.newBuilder(bucketName, getFileKey(fileInfo)).build();
            long duration = expiration.getTime() - System.currentTimeMillis();
            return getClient()
                    .signUrl(blobInfo, duration, TimeUnit.MILLISECONDS)
                    .toString();
        } catch (Exception e) {
            throw ExceptionFactory.generatePresignedUrl(fileInfo, platform, e);
        }
    }

    @Override
    public String generateThPresignedUrl(FileInfo fileInfo, Date expiration) {
        try {
            BlobInfo blobInfo =
                    BlobInfo.newBuilder(bucketName, getThFileKey(fileInfo)).build();
            long duration = expiration.getTime() - System.currentTimeMillis();
            return getClient()
                    .signUrl(blobInfo, duration, TimeUnit.MILLISECONDS)
                    .toString();
        } catch (Exception e) {
            throw ExceptionFactory.generateThPresignedUrl(fileInfo, platform, e);
        }
    }

    @Override
    public boolean isSupportMetadata() {
        return true;
    }

    /**
     * 检查并删除对象
     * <a href="https://github.com/googleapis/java-storage/blob/main/samples/snippets/src/main/java/com/example/storage/object/DeleteObject.java">Source Example</a>
     *
     * @param fileKey 对象 key
     */
    protected void checkAndDelete(String fileKey) {
        Storage client = getClient();
        Blob blob = client.get(bucketName, fileKey);
        if (blob != null) {
            Storage.BlobSourceOption precondition = Storage.BlobSourceOption.generationMatch(blob.getGeneration());
            client.delete(bucketName, fileKey, precondition);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                checkAndDelete(getThFileKey(fileInfo));
            }
            checkAndDelete(getFileKey(fileInfo));
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            Storage client = getClient();
            BlobId blobId = BlobId.of(bucketName, getFileKey(fileInfo));
            return client.get(blobId) != null;
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Storage client = getClient();
        BlobId blobId = BlobId.of(bucketName, getFileKey(fileInfo));

        try (ReadChannel readChannel = client.reader(blobId);
                InputStream in = Channels.newInputStream(readChannel)) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }
        Storage client = getClient();
        BlobId thBlobId = BlobId.of(bucketName, getThFileKey(fileInfo));
        try (ReadChannel readChannel = client.reader(thBlobId);
                InputStream in = Channels.newInputStream(readChannel)) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.downloadTh(fileInfo, platform, e);
        }
    }

    @Data
    public static class AclWrapper {
        private List<Acl> aclList;
        private PredefinedAcl predefinedAcl;

        public AclWrapper(List<Acl> aclList) {
            this.aclList = aclList;
        }

        public AclWrapper(PredefinedAcl predefinedAcl) {
            this.predefinedAcl = predefinedAcl;
        }
    }

    @Override
    public boolean isSupportSameCopy() {
        return true;
    }

    @Override
    public void sameCopy(FileInfo srcFileInfo, FileInfo destFileInfo, CopyPretreatment pre) {
        Check.sameCopyBasePath(platform, basePath, srcFileInfo, destFileInfo);

        Storage client = getClient();

        // 获取远程文件信息
        String srcFileKey = getFileKey(srcFileInfo);
        Blob srcFile;
        try {
            srcFile = client.get(bucketName, srcFileKey);
            if (srcFile == null) {
                throw ExceptionFactory.sameCopyNotFound(srcFileInfo, destFileInfo, platform, null);
            }
        } catch (Exception e) {
            throw ExceptionFactory.sameCopyNotFound(srcFileInfo, destFileInfo, platform, e);
        }

        // 复制缩略图文件
        String destThFileKey = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                client.copy(CopyRequest.newBuilder()
                                .setSource(BlobId.of(bucketName, getThFileKey(srcFileInfo)))
                                .setTarget(BlobId.of(bucketName, destThFileKey))
                                .build())
                        .getResult();
            } catch (Exception e) {
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 复制文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        try {
            ProgressListener.quickStart(pre.getProgressListener(), srcFile.getSize());
            client.copy(CopyRequest.newBuilder()
                            .setSource(BlobId.of(bucketName, srcFileKey))
                            .setTarget(BlobId.of(bucketName, destFileKey))
                            .build())
                    .getResult();
            ProgressListener.quickFinish(pre.getProgressListener(), srcFile.getSize());
        } catch (Exception e) {
            if (destThFileKey != null)
                try {
                    checkAndDelete(destThFileKey);
                } catch (Exception ignored) {
                }
            try {
                checkAndDelete(destFileKey);
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameCopy(srcFileInfo, destFileInfo, platform, e);
        }
    }
}
