package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.NamingCase;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.StrUtil;
import com.obs.services.ObsClient;
import com.obs.services.internal.ObsConvertor;
import com.obs.services.model.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.HuaweiObsConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.get.*;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlPretreatment;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;
import org.dromara.x.file.storage.core.upload.*;
import org.dromara.x.file.storage.core.util.Tools;

/**
 * 华为云 OBS 存储
 */
@Getter
@Setter
@NoArgsConstructor
public class HuaweiObsFileStorage implements FileStorage {
    private String platform;
    private String bucketName;
    private String domain;
    private String basePath;
    private String defaultAcl;
    private int multipartThreshold;
    private int multipartPartSize;
    private FileStorageClientFactory<ObsClient> clientFactory;

    public HuaweiObsFileStorage(HuaweiObsConfig config, FileStorageClientFactory<ObsClient> clientFactory) {
        platform = config.getPlatform();
        bucketName = config.getBucketName();
        domain = config.getDomain();
        basePath = config.getBasePath();
        defaultAcl = config.getDefaultAcl();
        multipartThreshold = config.getMultipartThreshold();
        multipartPartSize = config.getMultipartPartSize();
        this.clientFactory = clientFactory;
    }

    public ObsClient getClient() {
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
        AccessControlList fileAcl = getAcl(fileInfo.getFileAcl());
        ObjectMetadata metadata = getObjectMetadata(fileInfo);
        ProgressListener listener = pre.getProgressListener();
        ObsClient client = getClient();
        boolean useMultipartUpload = fileInfo.getSize() == null || fileInfo.getSize() >= multipartThreshold;
        String uploadId = null;
        try (InputStreamPlus in = pre.getInputStreamPlus(false)) {
            if (useMultipartUpload) { // 分片上传
                InitiateMultipartUploadRequest initiateMultipartUploadRequest =
                        new InitiateMultipartUploadRequest(bucketName, newFileKey);
                initiateMultipartUploadRequest.setMetadata(metadata);
                initiateMultipartUploadRequest.setAcl(fileAcl);
                uploadId = client.initiateMultipartUpload(initiateMultipartUploadRequest)
                        .getUploadId();
                List<PartEtag> partList = new ArrayList<>();
                int i = 0;
                AtomicLong progressSize = new AtomicLong();
                if (listener != null) listener.start();
                while (true) {
                    byte[] bytes = IoUtil.readBytes(in, multipartPartSize);
                    if (bytes == null || bytes.length == 0) break;
                    UploadPartRequest part = new UploadPartRequest();
                    part.setBucketName(bucketName);
                    part.setObjectKey(newFileKey);
                    part.setUploadId(uploadId);
                    part.setInput(new ByteArrayInputStream(bytes));
                    part.setPartSize((long) bytes.length); // 设置分片大小。除了最后一个分片没有大小限制，其他的分片最小为100 KB。
                    part.setPartNumber(
                            ++i); // 设置分片号。每一个上传的分片都有一个分片号，取值范围是1~10000，如果超出此范围，ObsClient将返回InvalidArgument错误码。
                    if (listener != null) {
                        part.setProgressListener(e -> listener.progress(
                                progressSize.addAndGet(e.getNewlyTransferredBytes()), fileInfo.getSize()));
                    }
                    UploadPartResult uploadPartResult = client.uploadPart(part);
                    partList.add(new PartEtag(uploadPartResult.getEtag(), uploadPartResult.getPartNumber()));
                }
                client.completeMultipartUpload(
                        new CompleteMultipartUploadRequest(bucketName, newFileKey, uploadId, partList));
                if (listener != null) listener.finish();
            } else {
                PutObjectRequest request = new PutObjectRequest(bucketName, newFileKey, in);
                request.setMetadata(metadata);
                request.setAcl(fileAcl);
                if (listener != null) {
                    listener.start();
                    request.setProgressListener(e -> listener.progress(e.getTransferredBytes(), fileInfo.getSize()));
                }
                client.putObject(request);
                if (listener != null) listener.finish();
            }
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());

            // 上传缩略图
            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                PutObjectRequest request =
                        new PutObjectRequest(bucketName, newThFileKey, new ByteArrayInputStream(thumbnailBytes));
                request.setMetadata(getThObjectMetadata(fileInfo));
                request.setAcl(getAcl(fileInfo.getThFileAcl()));
                client.putObject(request);
            }

            return true;
        } catch (Exception e) {
            try {
                if (useMultipartUpload) {
                    client.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, newFileKey, uploadId));
                } else {
                    client.deleteObject(bucketName, newFileKey);
                }
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.upload(fileInfo, platform, e);
        }
    }

    @Override
    public MultipartUploadSupportInfo isSupportMultipartUpload() {
        return MultipartUploadSupportInfo.supportAll();
    }

    @Override
    public void initiateMultipartUpload(FileInfo fileInfo, InitiateMultipartUploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        AccessControlList fileAcl = getAcl(fileInfo.getFileAcl());
        ObjectMetadata metadata = getObjectMetadata(fileInfo);
        ObsClient client = getClient();
        try {
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, newFileKey);
            request.setMetadata(metadata);
            request.setAcl(fileAcl);
            String uploadId = client.initiateMultipartUpload(request).getUploadId();
            fileInfo.setUploadId(uploadId);
        } catch (Exception e) {
            throw ExceptionFactory.initiateMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public FilePartInfo uploadPart(UploadPartPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        ObsClient client = getClient();
        FileWrapper partFileWrapper = pre.getPartFileWrapper();
        Long partSize = partFileWrapper.getSize();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            UploadPartRequest part = new UploadPartRequest();
            part.setBucketName(bucketName);
            part.setObjectKey(newFileKey);
            part.setUploadId(fileInfo.getUploadId());
            part.setInput(in);
            part.setPartSize(partSize);
            part.setPartNumber(pre.getPartNumber());
            UploadPartResult result = client.uploadPart(part);
            FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
            filePartInfo.setETag(result.getEtag());
            filePartInfo.setPartNumber(result.getPartNumber());
            filePartInfo.setPartSize(in.getProgressSize());
            filePartInfo.setCreateTime(new Date());
            return filePartInfo;
        } catch (Exception e) {
            throw ExceptionFactory.uploadPart(fileInfo, platform, e);
        }
    }

    @Override
    public void completeMultipartUpload(CompleteMultipartUploadPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        ObsClient client = getClient();
        try {
            List<PartEtag> partList = pre.getPartInfoList().stream()
                    .map(part -> new PartEtag(part.getETag(), part.getPartNumber()))
                    .collect(Collectors.toList());
            ProgressListener.quickStart(pre.getProgressListener(), fileInfo.getSize());
            client.completeMultipartUpload(
                    new CompleteMultipartUploadRequest(bucketName, newFileKey, fileInfo.getUploadId(), partList));
            ProgressListener.quickFinish(pre.getProgressListener(), fileInfo.getSize());
        } catch (Exception e) {
            throw ExceptionFactory.completeMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        ObsClient client = getClient();
        try {
            client.abortMultipartUpload(
                    new AbortMultipartUploadRequest(bucketName, newFileKey, fileInfo.getUploadId()));
        } catch (Exception e) {
            throw ExceptionFactory.abortMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public FilePartInfoList listParts(ListPartsPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        ObsClient client = getClient();
        try {
            ListPartsResult result = client.listParts(new ListPartsRequest(
                    bucketName, newFileKey, fileInfo.getUploadId(), pre.getMaxParts(), pre.getPartNumberMarker()));
            FilePartInfoList list = new FilePartInfoList();
            list.setFileInfo(fileInfo);
            list.setList(result.getMultipartList().stream()
                    .map(p -> {
                        FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
                        filePartInfo.setETag(p.getEtag());
                        filePartInfo.setPartNumber(p.getPartNumber());
                        filePartInfo.setPartSize(p.getSize());
                        filePartInfo.setLastModified(p.getLastModified());
                        return filePartInfo;
                    })
                    .collect(Collectors.toList()));
            list.setMaxParts(result.getMaxParts());
            list.setIsTruncated(result.isTruncated());
            list.setPartNumberMarker(Integer.parseInt(result.getPartNumberMarker()));
            list.setNextPartNumberMarker(Integer.parseInt(result.getNextPartNumberMarker()));
            return list;
        } catch (Exception e) {
            throw ExceptionFactory.listParts(fileInfo, platform, e);
        }
    }

    @Override
    public ListFilesSupportInfo isSupportListFiles() {
        return ListFilesSupportInfo.supportAll();
    }

    @Override
    public ListFilesResult listFiles(ListFilesPretreatment pre) {
        ObsClient client = getClient();
        try {
            ListObjectsRequest request = new ListObjectsRequest(bucketName);
            request.setMaxKeys(pre.getMaxFiles());
            request.setMarker(pre.getMarker());
            request.setDelimiter("/");
            request.setPrefix(basePath + pre.getPath() + pre.getFilenamePrefix());
            ObjectListing result = client.listObjects(request);
            ListFilesResult list = new ListFilesResult();

            list.setDirList(result.getCommonPrefixes().stream()
                    .map(item -> {
                        RemoteDirInfo dir = new RemoteDirInfo();
                        dir.setPlatform(pre.getPlatform());
                        dir.setBasePath(basePath);
                        dir.setPath(pre.getPath());
                        dir.setName(FileNameUtil.getName(item));
                        dir.setOriginal(item);
                        return dir;
                    })
                    .collect(Collectors.toList()));

            list.setFileList(result.getObjects().stream()
                    .map(item -> {
                        RemoteFileInfo info = new RemoteFileInfo();
                        info.setPlatform(pre.getPlatform());
                        info.setBasePath(basePath);
                        info.setPath(pre.getPath());
                        info.setFilename(FileNameUtil.getName(item.getObjectKey()));
                        info.setUrl(domain + getFileKey(new FileInfo(basePath, info.getPath(), info.getFilename())));
                        ObjectMetadata metadata = item.getMetadata();
                        info.setSize(metadata.getContentLength());
                        info.setExt(FileNameUtil.extName(info.getFilename()));
                        info.setETag(metadata.getEtag());
                        info.setContentDisposition(metadata.getContentDisposition());
                        info.setContentType(metadata.getContentType());
                        info.setContentMd5(metadata.getContentMd5());
                        info.setLastModified(metadata.getLastModified());
                        info.setMetadata(BeanUtil.beanToMap(metadata, false, true));
                        info.setUserMetadata(BeanUtil.beanToMap(metadata.getAllMetadata()));
                        info.setOriginal(item);
                        return info;
                    })
                    .collect(Collectors.toList()));
            list.setPlatform(pre.getPlatform());
            list.setBasePath(basePath);
            list.setPath(pre.getPath());
            list.setFilenamePrefix(pre.getFilenamePrefix());
            list.setMaxFiles(result.getMaxKeys());
            list.setIsTruncated(result.isTruncated());
            list.setMarker(result.getMarker());
            list.setNextMarker(result.getNextMarker());
            return list;
        } catch (Exception e) {
            throw ExceptionFactory.listFiles(pre, basePath, e);
        }
    }

    @Override
    public RemoteFileInfo getFile(GetFilePretreatment pre) {
        String fileKey = getFileKey(new FileInfo(basePath, pre.getPath(), pre.getFilename()));
        ObsClient client = getClient();
        try {
            ObsObject file;
            try {
                file = client.getObject(bucketName, fileKey);
            } catch (Exception e) {
                return null;
            }
            if (file == null) return null;
            ObjectMetadata metadata = file.getMetadata();
            RemoteFileInfo info = new RemoteFileInfo();
            info.setPlatform(pre.getPlatform());
            info.setBasePath(basePath);
            info.setPath(pre.getPath());
            info.setFilename(FileNameUtil.getName(file.getObjectKey()));
            info.setUrl(domain + fileKey);
            info.setSize(metadata.getContentLength());
            info.setExt(FileNameUtil.extName(info.getFilename()));
            info.setETag(metadata.getEtag());
            info.setContentDisposition(metadata.getContentDisposition());
            info.setContentType(metadata.getContentType());
            info.setContentMd5(metadata.getContentMd5());
            info.setLastModified(metadata.getLastModified());
            info.setMetadata(BeanUtil.beanToMap(metadata, false, true));
            info.getMetadata().remove("userMetadata");
            info.getMetadata().put("eTag", metadata.getEtag());
            info.setUserMetadata(new HashMap<>(metadata.getAllMetadata()));
            info.setOriginal(file);
            return info;
        } catch (Exception e) {
            throw ExceptionFactory.getFile(pre, basePath, e);
        }
    }

    /**
     * 获取文件的访问控制列表
     */
    public AccessControlList getAcl(Object acl) {
        if (acl instanceof AccessControlList) {
            return (AccessControlList) acl;
        } else if (acl instanceof String || acl == null) {
            String sAcl = (String) acl;
            if (StrUtil.isEmpty(sAcl)) sAcl = defaultAcl;
            if (sAcl == null) return null;
            return ObsConvertor.getInstance().transCannedAcl(sAcl);
        } else {
            throw ExceptionFactory.unrecognizedAcl(acl, platform);
        }
    }

    /**
     * 获取对象的元数据
     */
    public ObjectMetadata getObjectMetadata(FileInfo fileInfo) {
        ObjectMetadata metadata = new ObjectMetadata();
        if (fileInfo.getSize() != null) metadata.setContentLength(fileInfo.getSize());
        metadata.setContentType(fileInfo.getContentType());
        fileInfo.getUserMetadata().forEach(metadata::addUserMetadata);
        if (CollUtil.isNotEmpty(fileInfo.getMetadata())) {
            CopyOptions copyOptions = CopyOptions.create()
                    .ignoreCase()
                    .setFieldNameEditor(name -> NamingCase.toCamelCase(name, CharUtil.DASHED));
            BeanUtil.copyProperties(fileInfo.getMetadata(), metadata, copyOptions);
        }
        return metadata;
    }

    /**
     * 获取缩略图对象的元数据
     */
    public ObjectMetadata getThObjectMetadata(FileInfo fileInfo) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(fileInfo.getThSize());
        metadata.setContentType(fileInfo.getThContentType());
        fileInfo.getThUserMetadata().forEach(metadata::addUserMetadata);
        if (CollUtil.isNotEmpty(fileInfo.getThMetadata())) {
            CopyOptions copyOptions = CopyOptions.create()
                    .ignoreCase()
                    .setFieldNameEditor(name -> NamingCase.toCamelCase(name, CharUtil.DASHED));
            BeanUtil.copyProperties(fileInfo.getThMetadata(), metadata, copyOptions);
        }
        return metadata;
    }

    @Override
    public boolean isSupportPresignedUrl() {
        return true;
    }

    @Override
    public GeneratePresignedUrlResult generatePresignedUrl(GeneratePresignedUrlPretreatment pre) {
        try {
            HttpMethodEnum method = Tools.getEnum(HttpMethodEnum.class, pre.getMethod());
            SpecialParamEnum specialParam = Tools.getEnum(SpecialParamEnum.class, pre.getSpecialParam());
            long expires = (pre.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
            TemporarySignatureRequest request = new TemporarySignatureRequest(method, expires);
            request.setBucketName(bucketName);
            request.setObjectKey(getFileKey(new FileInfo(basePath, pre.getPath(), pre.getFilename())));
            request.setSpecialParam(specialParam);
            Map<String, String> headers = new HashMap<>(pre.getHeaders());
            headers.putAll(pre.getUserMetadata().entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().startsWith("x-obs-meta-") ? e.getKey() : "x-obs-meta-" + e.getKey(),
                            Map.Entry::getValue)));
            request.setHeaders(headers);
            HashMap<String, Object> queryParam = new HashMap<>(pre.getQueryParams());
            pre.getResponseHeaders().forEach((k, v) -> queryParam.put("response-" + k.toLowerCase(), v));
            request.setQueryParams(queryParam);
            GeneratePresignedUrlResult result = new GeneratePresignedUrlResult(platform, basePath, pre);
            TemporarySignatureResponse response = getClient().createTemporarySignature(request);
            result.setUrl(response.getSignedUrl());
            result.setHeaders(response.getActualSignedRequestHeaders());
            return result;
        } catch (Exception e) {
            throw ExceptionFactory.generatePresignedUrl(pre, e);
        }
    }

    @Override
    public boolean isSupportAcl() {
        return true;
    }

    @Override
    public boolean setFileAcl(FileInfo fileInfo, Object acl) {
        AccessControlList oAcl = getAcl(acl);
        if (oAcl == null) return false;
        try {
            getClient().setObjectAcl(bucketName, getFileKey(fileInfo), oAcl);
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.setFileAcl(fileInfo, oAcl, platform, e);
        }
    }

    @Override
    public boolean setThFileAcl(FileInfo fileInfo, Object acl) {
        AccessControlList oAcl = getAcl(acl);
        if (oAcl == null) return false;
        String key = getThFileKey(fileInfo);
        if (key == null) return false;
        try {
            getClient().setObjectAcl(bucketName, key, oAcl);
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.setThFileAcl(fileInfo, oAcl, platform, e);
        }
    }

    @Override
    public boolean isSupportMetadata() {
        return true;
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        ObsClient client = getClient();
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                client.deleteObject(bucketName, getThFileKey(fileInfo));
            }
            client.deleteObject(bucketName, getFileKey(fileInfo));
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            return getClient().doesObjectExist(bucketName, getFileKey(fileInfo));
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        ObsObject object = getClient().getObject(bucketName, getFileKey(fileInfo));
        try (InputStream in = object.getObjectContent()) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);

        ObsObject object = getClient().getObject(bucketName, getThFileKey(fileInfo));
        try (InputStream in = object.getObjectContent()) {
            consumer.accept(in);
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

        ObsClient client = getClient();

        // 获取远程文件信息
        String srcFileKey = getFileKey(srcFileInfo);
        ObjectMetadata srcFile;
        try {
            srcFile = client.getObjectMetadata(bucketName, srcFileKey);
        } catch (Exception e) {
            throw ExceptionFactory.sameCopyNotFound(srcFileInfo, destFileInfo, platform, e);
        }

        // 复制缩略图文件
        String destThFileKey = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                CopyObjectRequest request =
                        new CopyObjectRequest(bucketName, getThFileKey(srcFileInfo), bucketName, destThFileKey);
                request.setAcl(getAcl(destFileInfo.getThFileAcl()));
                client.copyObject(request);
            } catch (Exception e) {
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 复制文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        long fileSize = srcFile.getContentLength();
        boolean useMultipartCopy = fileSize >= 1024 * 1024 * 1024; // 按照华为云 OBS 官方文档小于 1GB，走小文件复制
        String uploadId = null;
        try {
            if (useMultipartCopy) { // 大文件复制，华为云 OBS 内部不会自动复制 Metadata 和 ACL，需要重新设置
                InitiateMultipartUploadRequest initiateMultipartUploadRequest =
                        new InitiateMultipartUploadRequest(bucketName, destFileKey);
                initiateMultipartUploadRequest.setMetadata(getObjectMetadata(destFileInfo));
                initiateMultipartUploadRequest.setAcl(getAcl(destFileInfo.getFileAcl()));
                uploadId = client.initiateMultipartUpload(initiateMultipartUploadRequest)
                        .getUploadId();
                ProgressListener.quickStart(pre.getProgressListener(), fileSize);
                ArrayList<PartEtag> partList = new ArrayList<>();
                long progressSize = 0;
                int i = 0;
                while (progressSize < fileSize) {
                    // 设置分片大小为 256 MB。单位为字节。
                    long partSize = Math.min(256 * 1024 * 1024, fileSize - progressSize);
                    CopyPartRequest part =
                            new CopyPartRequest(uploadId, bucketName, srcFileKey, bucketName, destFileKey, ++i);
                    part.setByteRangeStart(progressSize);
                    part.setByteRangeEnd(progressSize + partSize + 1);
                    partList.add(new PartEtag(client.copyPart(part).getEtag(), i));
                    ProgressListener.quickProgress(pre.getProgressListener(), progressSize += partSize, fileSize);
                }
                client.completeMultipartUpload(
                        new CompleteMultipartUploadRequest(bucketName, destFileKey, uploadId, partList));
                ProgressListener.quickFinish(pre.getProgressListener());
            } else { // 小文件复制，华为云 OBS 内部会自动复制 Metadata ，但是 ACL 需要重新设置
                ProgressListener.quickStart(pre.getProgressListener(), fileSize);
                CopyObjectRequest request = new CopyObjectRequest(bucketName, srcFileKey, bucketName, destFileKey);
                request.setAcl(getAcl(destFileInfo.getFileAcl()));
                client.copyObject(request);
                ProgressListener.quickFinish(pre.getProgressListener(), fileSize);
            }
        } catch (Exception e) {
            if (destThFileKey != null)
                try {
                    client.deleteObject(bucketName, destThFileKey);
                } catch (Exception ignored) {
                }
            try {
                if (useMultipartCopy) {
                    client.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, destFileKey, uploadId));
                } else {
                    client.deleteObject(bucketName, destFileKey);
                }
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameCopy(srcFileInfo, destFileInfo, platform, e);
        }
    }
}
