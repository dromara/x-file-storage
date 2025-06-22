package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.NamingCase;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.StrUtil;
import com.volcengine.tos.TOSV2;
import com.volcengine.tos.TosServerException;
import com.volcengine.tos.comm.common.ACLType;
import com.volcengine.tos.comm.event.DataTransferType;
import com.volcengine.tos.model.object.*;
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
import org.dromara.x.file.storage.core.FileStorageProperties.VolcengineTosConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.get.*;
import org.dromara.x.file.storage.core.move.MovePretreatment;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlPretreatment;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;
import org.dromara.x.file.storage.core.upload.*;

/**
 * 火山引擎 TOS 存储
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
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        ObjectMetaRequestOptions metadata = getObjectMetadata(fileInfo);
        ProgressListener listener = pre.getProgressListener();
        TOSV2 client = getClient();
        boolean useMultipartUpload = fileInfo.getSize() == null || fileInfo.getSize() >= multipartThreshold;
        String uploadId = null;
        try (InputStreamPlus in = pre.getInputStreamPlus(false)) {
            if (useMultipartUpload) { // 分片上传
                uploadId = client.createMultipartUpload(new CreateMultipartUploadInput()
                                .setBucket(bucketName)
                                .setKey(newFileKey)
                                .setOptions(metadata))
                        .getUploadID();

                List<UploadedPartV2> partList = new ArrayList<>();
                int i = 0;
                AtomicLong progressSize = new AtomicLong();
                if (listener != null) listener.start();
                while (true) {
                    byte[] bytes = IoUtil.readBytes(in, multipartPartSize);
                    if (bytes == null || bytes.length == 0) break;
                    UploadPartV2Input part = new UploadPartV2Input()
                            .setBucket(bucketName)
                            .setKey(newFileKey)
                            .setUploadID(uploadId)
                            .setPartNumber(++i)
                            .setContentLength(bytes.length)
                            .setContent(new ByteArrayInputStream(bytes));
                    if (listener != null) {
                        part.setDataTransferListener(e -> {
                            if (e.getType() == DataTransferType.DATA_TRANSFER_RW) {
                                listener.progress(progressSize.addAndGet(e.getRwOnceBytes()), fileInfo.getSize());
                            }
                        });
                    }
                    partList.add(new UploadedPartV2()
                            .setPartNumber(i)
                            .setEtag(client.uploadPart(part).getEtag()));
                }
                client.completeMultipartUpload(new CompleteMultipartUploadV2Input()
                        .setBucket(bucketName)
                        .setKey(newFileKey)
                        .setUploadID(uploadId)
                        .setUploadedParts(partList));
                if (listener != null) listener.finish();
            } else {
                PutObjectInput input = new PutObjectInput()
                        .setBucket(bucketName)
                        .setKey(newFileKey)
                        .setContent(in)
                        .setOptions(metadata);
                if (listener != null) {
                    input.setDataTransferListener(e -> {
                        if (e.getType() == DataTransferType.DATA_TRANSFER_STARTED) {
                            listener.start();
                        } else if (e.getType() == DataTransferType.DATA_TRANSFER_RW) {
                            listener.progress(e.getConsumedBytes(), fileInfo.getSize());
                        } else if (e.getType() == DataTransferType.DATA_TRANSFER_FAILED) {
                            listener.finish();
                        }
                    });
                }
                client.putObject(input);
            }
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                PutObjectInput input = new PutObjectInput()
                        .setBucket(bucketName)
                        .setKey(newThFileKey)
                        .setContent(new ByteArrayInputStream(thumbnailBytes))
                        .setOptions(getThObjectMetadata(fileInfo));
                client.putObject(input);
            }

            return true;
        } catch (Exception e) {
            try {
                if (useMultipartUpload) {
                    client.abortMultipartUpload(new AbortMultipartUploadInput()
                            .setBucket(bucketName)
                            .setKey(newFileKey)
                            .setUploadID(uploadId));
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
        ObjectMetaRequestOptions metadata = getObjectMetadata(fileInfo);
        TOSV2 client = getClient();
        try {
            String uploadId = client.createMultipartUpload(new CreateMultipartUploadInput()
                            .setBucket(bucketName)
                            .setKey(newFileKey)
                            .setOptions(metadata))
                    .getUploadID();
            fileInfo.setUploadId(uploadId);
        } catch (Exception e) {
            throw ExceptionFactory.initiateMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public FilePartInfo uploadPart(UploadPartPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        TOSV2 client = getClient();
        FileWrapper partFileWrapper = pre.getPartFileWrapper();
        Long partSize = partFileWrapper.getSize();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            UploadPartV2Input part = new UploadPartV2Input()
                    .setBucket(bucketName)
                    .setKey(newFileKey)
                    .setUploadID(fileInfo.getUploadId())
                    .setContent(in)
                    .setContentLength(partSize)
                    .setPartNumber(pre.getPartNumber());

            String partETag = client.uploadPart(part).getEtag();
            FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
            filePartInfo.setETag(partETag);
            filePartInfo.setPartNumber(pre.getPartNumber());
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
        TOSV2 client = getClient();
        try {
            List<UploadedPartV2> partList = pre.getPartInfoList().stream()
                    .map(part -> new UploadedPartV2()
                            .setPartNumber(part.getPartNumber())
                            .setEtag(part.getETag()))
                    .collect(Collectors.toList());
            ProgressListener.quickStart(pre.getProgressListener(), fileInfo.getSize());
            client.completeMultipartUpload(new CompleteMultipartUploadV2Input()
                    .setBucket(bucketName)
                    .setKey(newFileKey)
                    .setUploadID(fileInfo.getUploadId())
                    .setUploadedParts(partList));
            ProgressListener.quickFinish(pre.getProgressListener(), fileInfo.getSize());
        } catch (Exception e) {
            throw ExceptionFactory.completeMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        TOSV2 client = getClient();
        try {
            client.abortMultipartUpload(new AbortMultipartUploadInput()
                    .setBucket(bucketName)
                    .setKey(newFileKey)
                    .setUploadID(fileInfo.getUploadId()));
        } catch (Exception e) {
            throw ExceptionFactory.abortMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public FilePartInfoList listParts(ListPartsPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        TOSV2 client = getClient();
        try {
            ListPartsInput request = ListPartsInput.builder()
                    .bucket(bucketName)
                    .key(newFileKey)
                    .uploadID(fileInfo.getUploadId())
                    .maxParts(pre.getMaxParts())
                    .partNumberMarker(pre.getPartNumberMarker())
                    .build();
            ListPartsOutput result = client.listParts(request);
            FilePartInfoList list = new FilePartInfoList();
            list.setFileInfo(fileInfo);
            if (CollUtil.isEmpty(result.getUploadedParts())) {
                list.setList(new ArrayList<>());
            } else {
                list.setList(result.getUploadedParts().stream()
                        .map(p -> {
                            FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
                            filePartInfo.setETag(p.getEtag());
                            filePartInfo.setPartNumber(p.getPartNumber());
                            filePartInfo.setPartSize(p.getSize());
                            filePartInfo.setLastModified(p.getLastModified());
                            return filePartInfo;
                        })
                        .collect(Collectors.toList()));
            }
            list.setMaxParts(result.getMaxParts());
            list.setIsTruncated(result.isTruncated());
            list.setPartNumberMarker(result.getPartNumberMarker());
            list.setNextPartNumberMarker(result.getNextPartNumberMarker());
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
        TOSV2 client = getClient();
        try {
            ListObjectsV2Input request = ListObjectsV2Input.builder()
                    .bucket(bucketName)
                    .maxKeys(pre.getMaxFiles())
                    .marker(pre.getMarker())
                    .delimiter("/")
                    .prefix(basePath + pre.getPath() + pre.getFilenamePrefix())
                    .build();

            ListObjectsV2Output result = client.listObjects(request);
            ListFilesResult list = new ListFilesResult();
            if (CollUtil.isEmpty(result.getCommonPrefixes())) {
                list.setDirList(new ArrayList<>());
            } else {
                list.setDirList(result.getCommonPrefixes().stream()
                        .map(item -> {
                            RemoteDirInfo dir = new RemoteDirInfo();
                            dir.setPlatform(pre.getPlatform());
                            dir.setBasePath(basePath);
                            dir.setPath(pre.getPath());
                            dir.setName(FileNameUtil.getName(item.getPrefix()));
                            dir.setOriginal(item);
                            return dir;
                        })
                        .collect(Collectors.toList()));
            }
            if (CollUtil.isEmpty(result.getContents())) {
                list.setFileList(new ArrayList<>());
            } else {
                list.setFileList(result.getContents().stream()
                        .map(item -> {
                            RemoteFileInfo info = new RemoteFileInfo();
                            info.setPlatform(pre.getPlatform());
                            info.setBasePath(basePath);
                            info.setPath(pre.getPath());
                            info.setFilename(FileNameUtil.getName(item.getKey()));
                            info.setUrl(
                                    domain + getFileKey(new FileInfo(basePath, info.getPath(), info.getFilename())));
                            info.setSize(item.getSize());
                            info.setExt(FileNameUtil.extName(info.getFilename()));
                            info.setETag(item.getEtag());
                            info.setLastModified(item.getLastModified());
                            info.setOriginal(item);
                            return info;
                        })
                        .collect(Collectors.toList()));
            }
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
        TOSV2 client = getClient();
        try {
            GetObjectV2Output file = null;
            try {
                file = client.getObject(
                        new GetObjectV2Input().setBucket(bucketName).setKey(fileKey));
            } catch (Exception e) {
                return null;
            } finally {
                IoUtil.close(file);
            }
            if (file == null) return null;
            RemoteFileInfo info = new RemoteFileInfo();
            info.setPlatform(pre.getPlatform());
            info.setBasePath(basePath);
            info.setPath(pre.getPath());
            info.setFilename(FileNameUtil.getName(fileKey));
            info.setUrl(domain + fileKey);
            info.setSize(file.getContentLength());
            info.setExt(FileNameUtil.extName(info.getFilename()));
            info.setETag(file.getEtag());
            info.setContentDisposition(file.getContentDisposition());
            info.setContentType(file.getContentType());
            info.setContentMd5(file.getContentMD5());
            info.setLastModified(DateUtil.parse(file.getLastModified()));
            info.setMetadata(file.getRequestInfo().getHeader().entrySet().stream()
                    .filter(e -> !e.getKey().startsWith("x-tos-meta-"))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            if (file.getCustomMetadata() != null) info.setUserMetadata(new HashMap<>(file.getCustomMetadata()));
            info.setOriginal(file);
            return info;
        } catch (Exception e) {
            throw ExceptionFactory.getFile(pre, basePath, e);
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
            for (ACLType item : ACLType.values()) {
                if (item.toString().equalsIgnoreCase(sAcl)) {
                    return item;
                }
            }
            return null;
        } else {
            throw ExceptionFactory.unrecognizedAcl(acl, platform);
        }
    }

    /**
     * 获取文件对象元数据
     */
    private ObjectMetaRequestOptions getObjectMetadata(FileInfo fileInfo) {
        ObjectMetaRequestOptions metadata = new ObjectMetaRequestOptions();
        metadata.setContentType(fileInfo.getContentType());
        metadata.setAclType(getAcl(fileInfo.getFileAcl()));
        metadata.setCustomMetadata(fileInfo.getUserMetadata());
        if (CollUtil.isNotEmpty(fileInfo.getMetadata())) {
            CopyOptions copyOptions = CopyOptions.create()
                    .ignoreCase()
                    .setFieldNameEditor(name -> NamingCase.toCamelCase(name, CharUtil.DASHED));
            BeanUtil.copyProperties(fileInfo.getMetadata(), metadata, copyOptions);
        }
        return metadata;
    }

    /**
     * 获取缩略图文件对象元数据
     */
    private ObjectMetaRequestOptions getThObjectMetadata(FileInfo fileInfo) {
        ObjectMetaRequestOptions metadata = new ObjectMetaRequestOptions();
        metadata.setContentType(fileInfo.getThContentType());
        metadata.setAclType(getAcl(fileInfo.getThFileAcl()));
        metadata.setCustomMetadata(fileInfo.getThUserMetadata());
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
            String fileKey = getFileKey(new FileInfo(basePath, pre.getPath(), pre.getFilename()));
            Map<String, String> headers = new HashMap<>(pre.getHeaders());
            headers.putAll(pre.getUserMetadata().entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().startsWith("x-tos-meta-") ? e.getKey() : "x-tos-meta-" + e.getKey(),
                            Map.Entry::getValue)));
            HashMap<String, String> queryParams = new HashMap<>(pre.getQueryParams());
            pre.getResponseHeaders()
                    .forEach((k, v) -> queryParams.put(NamingCase.toCamelCase("response-" + k.toLowerCase(), '-'), v));

            PreSignedURLInput request = PreSignedURLInput.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .expires((pre.getExpiration().getTime() - System.currentTimeMillis()) / 1000)
                    .httpMethod(String.valueOf(pre.getMethod()).toUpperCase())
                    .header(headers)
                    .query(queryParams)
                    .build();
            PreSignedURLOutput output = getClient().preSignedURL(request);
            GeneratePresignedUrlResult result = new GeneratePresignedUrlResult(platform, basePath, pre);
            result.setUrl(output.getSignedUrl());
            result.setHeaders(output.getSignedHeader());
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
        ACLType oAcl = getAcl(acl);
        if (oAcl == null) return false;
        try {
            PutObjectACLInput input = new PutObjectACLInput()
                    .setBucket(bucketName)
                    .setKey(getFileKey(fileInfo))
                    .setAcl(oAcl);
            getClient().putObjectAcl(input);
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.setFileAcl(fileInfo, oAcl, platform, e);
        }
    }

    @Override
    public boolean setThFileAcl(FileInfo fileInfo, Object acl) {
        ACLType oAcl = getAcl(acl);
        if (oAcl == null) return false;
        try {
            PutObjectACLInput input = new PutObjectACLInput()
                    .setBucket(bucketName)
                    .setKey(getThFileKey(fileInfo))
                    .setAcl(oAcl);
            getClient().putObjectAcl(input);
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
            HeadObjectV2Output output = getClient()
                    .headObject(new HeadObjectV2Input().setBucket(bucketName).setKey(getFileKey(fileInfo)));
            return output != null;
        } catch (TosServerException e) {
            if (e.getStatusCode() == 404) return false;
            throw ExceptionFactory.exists(fileInfo, platform, e);
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    public boolean exists(String fileKey) {
        try {
            HeadObjectV2Output output = getClient()
                    .headObject(new HeadObjectV2Input().setBucket(bucketName).setKey(fileKey));
            return output != null;
        } catch (TosServerException e) {
            if (e.getStatusCode() == 404) return false;
            throw e;
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        try {
            try (GetObjectV2Output output = getClient()
                            .getObject(
                                    new GetObjectV2Input().setBucket(bucketName).setKey(getFileKey(fileInfo)));
                    InputStream in = output.getContent()) {
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
            try (GetObjectV2Output output = getClient()
                            .getObject(
                                    new GetObjectV2Input().setBucket(bucketName).setKey(getThFileKey(fileInfo)));
                    InputStream in = output.getContent()) {
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
        GetObjectV2Output srcFile = null;
        try {
            srcFile = client.getObject(
                    new GetObjectV2Input().setBucket(bucketName).setKey(srcFileKey));
        } catch (Exception e) {
            throw ExceptionFactory.sameCopyNotFound(srcFileInfo, destFileInfo, platform, e);
        } finally {
            IoUtil.close(srcFile);
        }

        // 复制缩略图文件
        String destThFileKey = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                CopyObjectV2Input input = new CopyObjectV2Input()
                        .setSrcBucket(bucketName)
                        .setSrcKey(getThFileKey(srcFileInfo))
                        .setBucket(bucketName)
                        .setKey(destThFileKey)
                        //
                        // .setMetadataDirective(MetadataDirectiveType.METADATA_DIRECTIVE_REPLACE)
                        .setOptions(getThObjectMetadata(destFileInfo));
                client.copyObject(input);
            } catch (Exception e) {
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 复制文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        long fileSize = srcFile.getContentLength();
        boolean useMultipartCopy = fileSize >= 1024 * 1024 * 1024; // 按照火山引擎 TOS 官方文档小于 5GB，但为了统一，这里还是 1GB，走小文件复制
        String uploadId = null;
        try {
            if (useMultipartCopy) { // 大文件复制，火山引擎 TOS 内部不会自动复制 Metadata 和 ACL，需要重新设置
                ObjectMetaRequestOptions metadata = getObjectMetadata(destFileInfo);
                uploadId = client.createMultipartUpload(new CreateMultipartUploadInput()
                                .setBucket(bucketName)
                                .setKey(destFileKey)
                                .setOptions(metadata))
                        .getUploadID();
                ProgressListener.quickStart(pre.getProgressListener(), fileSize);
                ArrayList<UploadedPartV2> partList = new ArrayList<>();
                long progressSize = 0;
                for (int i = 1; progressSize < fileSize; i++) {
                    // 设置分片大小为 256 MB。单位为字节。
                    long partSize = Math.min(256 * 1024 * 1024, fileSize - progressSize);
                    UploadPartCopyV2Input part = new UploadPartCopyV2Input();
                    part.setBucket(bucketName);
                    part.setKey(destFileKey);
                    part.setSourceBucket(bucketName);
                    part.setSourceKey(srcFileKey);
                    part.setUploadID(uploadId);
                    part.setCopySourceRange(progressSize, progressSize + partSize - 1);
                    part.setPartNumber(i);
                    UploadPartCopyV2Output partCopyResponse = client.uploadPartCopy(part);
                    partList.add(new UploadedPartV2()
                            .setPartNumber(i)
                            .setEtag(partCopyResponse.getEtag())
                            .setSize(partSize));
                    ProgressListener.quickProgress(pre.getProgressListener(), progressSize += partSize, fileSize);
                }
                client.completeMultipartUpload(new CompleteMultipartUploadV2Input()
                        .setBucket(bucketName)
                        .setKey(destFileKey)
                        .setUploadID(uploadId)
                        .setUploadedParts(partList));
                ProgressListener.quickFinish(pre.getProgressListener());
            } else { // 小文件复制，火山引擎 TOS 内部会自动复制 Metadata ，但是 ACL 需要重新设置，因为 ACL 包含在 Metadata 中，所以这里全部重新设置
                ProgressListener.quickStart(pre.getProgressListener(), fileSize);
                CopyObjectV2Input input = new CopyObjectV2Input()
                        .setSrcBucket(bucketName)
                        .setSrcKey(srcFileKey)
                        .setBucket(bucketName)
                        .setKey(destFileKey)
                        //
                        // .setMetadataDirective(MetadataDirectiveType.METADATA_DIRECTIVE_REPLACE)
                        .setOptions(getObjectMetadata(destFileInfo));
                client.copyObject(input);
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
                    client.abortMultipartUpload(new AbortMultipartUploadInput()
                            .setBucket(bucketName)
                            .setKey(destFileKey)
                            .setUploadID(uploadId));
                } else {
                    client.deleteObject(bucketName, destFileKey);
                }
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameCopy(srcFileInfo, destFileInfo, platform, e);
        }
    }

    @Override
    public boolean isSupportSameMove() {
        return true;
    }

    @Override
    public void sameMove(FileInfo srcFileInfo, FileInfo destFileInfo, MovePretreatment pre) {

        TOSV2 client = getClient();
        // 获取远程文件信息
        String srcFileKey = getFileKey(srcFileInfo);
        GetObjectV2Output srcFile = null;
        try {
            srcFile = client.getObject(
                    new GetObjectV2Input().setBucket(bucketName).setKey(srcFileKey));
        } catch (Exception e) {
            throw ExceptionFactory.sameMoveNotFound(srcFileInfo, destFileInfo, platform, e);
        } finally {
            IoUtil.close(srcFile);
        }

        // 移动缩略图文件
        String srcThFileKey = null;
        String destThFileKey = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            srcThFileKey = getThFileKey(srcFileInfo);
            destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                client.renameObject(new RenameObjectInput()
                        .setBucket(bucketName)
                        .setKey(srcThFileKey)
                        .setNewKey(destThFileKey)
                        .setForbidOverwrite(false)
                        .setRecursiveMkdir(true));
            } catch (Exception e) {
                throw ExceptionFactory.sameMoveTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 移动文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        try {
            ProgressListener.quickStart(pre.getProgressListener(), srcFile.getContentLength());
            client.renameObject(new RenameObjectInput()
                    .setBucket(bucketName)
                    .setKey(srcFileKey)
                    .setNewKey(destFileKey)
                    .setForbidOverwrite(false)
                    .setRecursiveMkdir(true));
            ProgressListener.quickFinish(pre.getProgressListener(), srcFile.getContentLength());
        } catch (Exception e) {
            if (destThFileKey != null)
                try {
                    client.renameObject(new RenameObjectInput()
                            .setBucket(bucketName)
                            .setKey(destThFileKey)
                            .setNewKey(srcThFileKey)
                            .setForbidOverwrite(false)
                            .setRecursiveMkdir(true));
                } catch (Exception ignored) {
                }
            try {
                if (exists(srcFileKey)) {
                    client.deleteObject(bucketName, destFileKey);
                } else {
                    client.renameObject(new RenameObjectInput()
                            .setBucket(bucketName)
                            .setKey(destFileKey)
                            .setNewKey(srcFileKey)
                            .setForbidOverwrite(false)
                            .setRecursiveMkdir(true));
                }
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameMove(srcFileInfo, destFileInfo, platform, e);
        }
    }
}
