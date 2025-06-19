package org.dromara.x.file.storage.core.platform;

import static org.dromara.x.file.storage.core.platform.AmazonS3V2FileStorageClientFactory.AmazonS3V2Client;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.map.MapProxy;
import cn.hutool.core.text.NamingCase;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.StrUtil;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.*;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.get.*;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlPretreatment;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;
import org.dromara.x.file.storage.core.upload.*;
import org.dromara.x.file.storage.core.util.KebabCaseInsensitiveMap;
import org.dromara.x.file.storage.core.util.Tools;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

/**
 * Amazon S3 存储<br/>
 * 适用于AWS SDK for Java 2.x，根据 Amazon 公告，AWS SDK for Java 1.x 自 2024 年 7 月 31 日起将进入维护模式，并于 2025 年 12 月 31 日终止支持<br/>
 * 公告链接地址：<a href="https://aws.amazon.com/blogs/developer/the-aws-sdk-for-java-1-x-is-in-maintenance-mode-effective-july-31-2024/">The AWS SDK for Java 1.x is in maintenance mode, effective July 31, 2024</a>
 * @author zhangxin
 * @date 2024-12-08
 */
@Getter
@Setter
@NoArgsConstructor
public class AmazonS3V2FileStorage implements FileStorage {
    private String platform;
    private String region;
    private String bucketName;
    private String domain;
    private String basePath;
    private String defaultAcl;
    private int multipartThreshold;
    private int multipartPartSize;
    private FileStorageClientFactory<AmazonS3V2Client> clientFactory;

    public AmazonS3V2FileStorage(
            FileStorageProperties.AmazonS3V2Config config, FileStorageClientFactory<AmazonS3V2Client> clientFactory) {
        platform = config.getPlatform();
        region = config.getRegion();
        bucketName = config.getBucketName();
        domain = config.getDomain();
        basePath = config.getBasePath();
        defaultAcl = config.getDefaultAcl();
        multipartThreshold = config.getMultipartThreshold();
        multipartPartSize = config.getMultipartPartSize();
        this.clientFactory = clientFactory;
    }

    public AmazonS3V2Client getClient() {
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
        ProgressListener listener = pre.getProgressListener();
        S3Client client = getClient().getClient(); // 使用 S3Client 替代 AmazonS3
        boolean useMultipartUpload = fileInfo.getSize() == null || fileInfo.getSize() >= multipartThreshold;
        String uploadId = null;
        try (InputStreamPlus in = pre.getInputStreamPlus(false)) {
            if (useMultipartUpload) { // 分片上传
                uploadId = client.createMultipartUpload(setMetadata(
                                        CreateMultipartUploadRequest.builder()
                                                .bucket(bucketName)
                                                .key(newFileKey),
                                        fileInfo)
                                .build())
                        .uploadId();
                List<CompletedPart> partList = new ArrayList<>();
                int i = 0;
                AtomicLong progressSize = new AtomicLong();
                if (listener != null) listener.start();
                while (true) {
                    byte[] bytes = IoUtil.readBytes(in, multipartPartSize);
                    if (bytes == null || bytes.length == 0) break;
                    UploadPartRequest part = UploadPartRequest.builder()
                            .bucket(bucketName)
                            .key(newFileKey)
                            .uploadId(uploadId)
                            .partNumber(
                                    ++i) // 设置分片号。每一个上传的分片都有一个分片号，取值范围是1~10000，如果超出此范围，AmazonS3将返回InvalidArgument错误码。
                            .contentLength((long) bytes.length)
                            .build();
                    RequestBody body;
                    if (listener != null) {
                        body = RequestBody.fromInputStream(
                                new InputStreamPlus(
                                        new ByteArrayInputStream(bytes),
                                        currentSize -> ProgressListener.quickProgress(
                                                listener, progressSize.get() + currentSize, fileInfo.getSize())),
                                bytes.length);
                    } else {
                        body = RequestBody.fromBytes(bytes);
                    }
                    partList.add(CompletedPart.builder()
                            .partNumber(i)
                            .eTag(client.uploadPart(part, body).eTag())
                            .build());
                }
                client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(newFileKey)
                        .uploadId(uploadId)
                        .multipartUpload(CompletedMultipartUpload.builder()
                                .parts(partList)
                                .build())
                        .build());
                if (listener != null) listener.finish();
            } else { // 普通上传
                client.putObject(
                        setMetadata(
                                        PutObjectRequest.builder()
                                                .bucket(bucketName)
                                                .key(newFileKey),
                                        fileInfo)
                                .build(),
                        RequestBody.fromInputStream(
                                new InputStreamPlus(in, listener, fileInfo.getSize()), fileInfo.getSize()));
            }
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());
            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                client.putObject(
                        setThMetadata(
                                        PutObjectRequest.builder()
                                                .bucket(bucketName)
                                                .key(newThFileKey),
                                        fileInfo)
                                .build(),
                        RequestBody.fromBytes(thumbnailBytes));
            }
            return true;
        } catch (Exception e) {
            try {
                if (useMultipartUpload) {
                    AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                            .bucket(bucketName)
                            .key(newFileKey)
                            .uploadId(uploadId)
                            .build();
                    client.abortMultipartUpload(abortRequest);
                } else {
                    DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(newFileKey)
                            .build();
                    client.deleteObject(deleteRequest);
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
        S3Client client = getClient().getClient();
        try {
            String uploadId = client.createMultipartUpload(setMetadata(
                                    CreateMultipartUploadRequest.builder()
                                            .bucket(bucketName)
                                            .key(newFileKey),
                                    fileInfo)
                            .build())
                    .uploadId();
            fileInfo.setUploadId(uploadId);
        } catch (Exception e) {
            throw ExceptionFactory.initiateMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public FilePartInfo uploadPart(UploadPartPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        S3Client client = getClient().getClient();
        FileWrapper partFileWrapper = pre.getPartFileWrapper();
        Long partSize = partFileWrapper.getSize();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            // Amazon S3 比较特殊，上传分片必须传入分片大小，这里强制获取，可能会占用大量内存
            if (partSize == null) partSize = partFileWrapper.getInputStreamMaskResetReturn(Tools::getSize);
            UploadPartRequest partRequest = UploadPartRequest.builder()
                    .bucket(bucketName)
                    .key(newFileKey)
                    .uploadId(fileInfo.getUploadId())
                    .partNumber(pre.getPartNumber())
                    .contentLength(partSize)
                    .build();
            UploadPartResponse partResponse = client.uploadPart(partRequest, RequestBody.fromInputStream(in, partSize));
            FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
            filePartInfo.setETag(partResponse.eTag());
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
        S3Client client = getClient().getClient();
        try {
            List<CompletedPart> partList = pre.getPartInfoList().stream()
                    .map(part -> CompletedPart.builder()
                            .partNumber(part.getPartNumber())
                            .eTag(part.getETag())
                            .build())
                    .collect(Collectors.toList());
            ProgressListener.quickStart(pre.getProgressListener(), fileInfo.getSize());
            client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(newFileKey)
                    .uploadId(fileInfo.getUploadId())
                    .multipartUpload(
                            CompletedMultipartUpload.builder().parts(partList).build())
                    .build());
            ProgressListener.quickFinish(pre.getProgressListener(), fileInfo.getSize());
        } catch (Exception e) {
            throw ExceptionFactory.completeMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        S3Client client = getClient().getClient();
        try {
            client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(newFileKey)
                    .uploadId(fileInfo.getUploadId())
                    .build());
        } catch (Exception e) {
            throw ExceptionFactory.abortMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public FilePartInfoList listParts(ListPartsPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        S3Client client = getClient().getClient();
        try {
            ListPartsRequest request = ListPartsRequest.builder()
                    .bucket(bucketName)
                    .key(newFileKey)
                    .uploadId(fileInfo.getUploadId())
                    .maxParts(pre.getMaxParts())
                    .partNumberMarker(pre.getPartNumberMarker())
                    .build();
            ListPartsResponse result = client.listParts(request);
            FilePartInfoList list = new FilePartInfoList();
            list.setFileInfo(fileInfo);
            list.setList(result.parts().stream()
                    .map(p -> {
                        FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
                        filePartInfo.setETag(p.eTag());
                        filePartInfo.setPartNumber(p.partNumber());
                        filePartInfo.setPartSize(p.size());
                        filePartInfo.setLastModified(Date.from(p.lastModified()));
                        return filePartInfo;
                    })
                    .collect(Collectors.toList()));
            list.setMaxParts(result.maxParts());
            list.setIsTruncated(result.isTruncated());
            list.setPartNumberMarker(result.partNumberMarker());
            list.setNextPartNumberMarker(result.nextPartNumberMarker());
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
        S3Client client = getClient().getClient();
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .maxKeys(pre.getMaxFiles())
                    .continuationToken(pre.getMarker()) // 使用 startAfter 替代 marker
                    .delimiter("/")
                    .prefix(basePath + pre.getPath() + pre.getFilenamePrefix())
                    .build();
            ListObjectsV2Response result = client.listObjectsV2(request);
            ListFilesResult list = new ListFilesResult();

            list.setDirList(result.commonPrefixes().stream()
                    .map(item -> {
                        RemoteDirInfo dir = new RemoteDirInfo();
                        dir.setPlatform(pre.getPlatform());
                        dir.setBasePath(basePath);
                        dir.setPath(pre.getPath());
                        dir.setName(FileNameUtil.getName(item.prefix()));
                        dir.setOriginal(item);
                        return dir;
                    })
                    .collect(Collectors.toList()));

            list.setFileList(result.contents().stream()
                    .map(item -> {
                        RemoteFileInfo info = new RemoteFileInfo();
                        info.setPlatform(pre.getPlatform());
                        info.setBasePath(basePath);
                        info.setPath(pre.getPath());
                        info.setFilename(FileNameUtil.getName(item.key()));
                        info.setUrl(domain + getFileKey(new FileInfo(basePath, info.getPath(), info.getFilename())));
                        info.setSize(item.size());
                        info.setExt(FileNameUtil.extName(info.getFilename()));
                        info.setETag(item.eTag());
                        info.setLastModified(Date.from(item.lastModified()));
                        info.setOriginal(item);
                        return info;
                    })
                    .collect(Collectors.toList()));
            list.setPlatform(pre.getPlatform());
            list.setBasePath(basePath);
            list.setPath(pre.getPath());
            list.setFilenamePrefix(pre.getFilenamePrefix());
            list.setMaxFiles(result.maxKeys());
            list.setIsTruncated(result.isTruncated());
            list.setMarker(pre.getMarker());
            list.setNextMarker(result.nextContinuationToken());
            return list;
        } catch (Exception e) {
            throw ExceptionFactory.listFiles(pre, basePath, e);
        }
    }

    @Override
    public RemoteFileInfo getFile(GetFilePretreatment pre) {
        String fileKey = getFileKey(new FileInfo(basePath, pre.getPath(), pre.getFilename()));
        S3Client client = getClient().getClient();
        try {
            HeadObjectResponse file;
            try {
                file = client.headObject(HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileKey)
                        .build());
            } catch (Exception e) {
                return null;
            }
            if (file == null) return null;
            KebabCaseInsensitiveMap<String, String> headers =
                    new KebabCaseInsensitiveMap<>(file.sdkHttpResponse().headers().entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, v -> CollUtil.get(v.getValue(), 0))));
            MapProxy headersProxy = MapProxy.create(headers);
            RemoteFileInfo info = new RemoteFileInfo();
            info.setPlatform(pre.getPlatform());
            info.setBasePath(basePath);
            info.setPath(pre.getPath());
            info.setFilename(FileNameUtil.getName(fileKey));
            info.setUrl(domain + fileKey);
            info.setSize(file.contentLength());
            info.setExt(FileNameUtil.extName(info.getFilename()));
            info.setETag(file.eTag());
            info.setContentDisposition(file.contentDisposition());
            info.setContentType(file.contentType());
            info.setContentMd5(headersProxy.getStr(Constant.Metadata.CONTENT_MD5));
            info.setLastModified(Date.from(file.lastModified()));
            info.setMetadata(headers.entrySet().stream()
                    .filter(e -> !e.getKey().startsWith("x-amz-meta-"))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            if (file.metadata() != null) info.setUserMetadata(new HashMap<>(file.metadata()));
            info.setOriginal(file);
            return info;
        } catch (Exception e) {
            throw ExceptionFactory.getFile(pre, basePath, e);
        }
    }

    /**
     * 获取文件的访问控制列表
     */
    public String getAcl(Object acl) {
        if (acl instanceof ObjectCannedACL) {
            return acl.toString();
        } else if (acl instanceof String || acl == null) {
            String sAcl = (String) acl;
            if (StrUtil.isEmpty(sAcl)) sAcl = defaultAcl; // 如果为空则使用默认 ACL
            for (ObjectCannedACL item : ObjectCannedACL.values()) {
                if (item.toString().equalsIgnoreCase(sAcl)) { // 忽略大小写比较
                    return item.toString();
                }
            }
            return StrUtil.isEmpty(sAcl) ? null : sAcl; // 如果未匹配到，返回 null
        } else {
            throw ExceptionFactory.unrecognizedAcl(acl, platform);
        }
    }

    /**
     * 设置对象的元数据
     */
    public CreateMultipartUploadRequest.Builder setMetadata(
            CreateMultipartUploadRequest.Builder builder, FileInfo fileInfo) {
        builder.contentType(fileInfo.getContentType()).metadata(fileInfo.getUserMetadata());
        if (CollUtil.isNotEmpty(fileInfo.getMetadata())) {
            CopyOptions copyOptions = CopyOptions.create()
                    .ignoreCase()
                    .setFieldNameEditor(name -> NamingCase.toCamelCase(name, CharUtil.DASHED));
            BeanUtil.copyProperties(fileInfo.getMetadata(), builder, copyOptions);
        }
        builder.acl(getAcl(fileInfo.getFileAcl()));
        return builder;
    }

    /**
     * 设置对象的元数据
     */
    public PutObjectRequest.Builder setMetadata(PutObjectRequest.Builder builder, FileInfo fileInfo) {
        builder.contentType(fileInfo.getContentType()).metadata(fileInfo.getUserMetadata());
        if (CollUtil.isNotEmpty(fileInfo.getMetadata())) {
            CopyOptions copyOptions = CopyOptions.create()
                    .ignoreCase()
                    .setFieldNameEditor(name -> NamingCase.toCamelCase(name, CharUtil.DASHED));
            BeanUtil.copyProperties(fileInfo.getMetadata(), builder, copyOptions);
        }
        builder.acl(getAcl(fileInfo.getFileAcl()));
        return builder;
    }

    /**
     * 设置对象的元数据
     */
    public CopyObjectRequest.Builder setMetadata(CopyObjectRequest.Builder builder, FileInfo fileInfo) {
        builder.contentType(fileInfo.getContentType()).metadata(fileInfo.getUserMetadata());
        if (CollUtil.isNotEmpty(fileInfo.getMetadata())) {
            CopyOptions copyOptions = CopyOptions.create()
                    .ignoreCase()
                    .setFieldNameEditor(name -> NamingCase.toCamelCase(name, CharUtil.DASHED));
            BeanUtil.copyProperties(fileInfo.getMetadata(), builder, copyOptions);
        }
        builder.acl(getAcl(fileInfo.getFileAcl()));
        return builder;
    }

    /**
     * 设置缩略图对象的元数据
     */
    public PutObjectRequest.Builder setThMetadata(PutObjectRequest.Builder builder, FileInfo fileInfo) {
        builder.contentType(fileInfo.getThContentType()).metadata(fileInfo.getThUserMetadata());
        if (CollUtil.isNotEmpty(fileInfo.getThMetadata())) {
            CopyOptions copyOptions = CopyOptions.create()
                    .ignoreCase()
                    .setFieldNameEditor(name -> NamingCase.toCamelCase(name, CharUtil.DASHED));
            BeanUtil.copyProperties(fileInfo.getThMetadata(), builder, copyOptions);
        }
        builder.acl(getAcl(fileInfo.getThFileAcl()));
        return builder;
    }

    @Override
    public boolean isSupportPresignedUrl() {
        return true;
    }

    @Override
    public GeneratePresignedUrlResult generatePresignedUrl(GeneratePresignedUrlPretreatment pre) {

        S3Presigner presigner = getClient().getPresigner();
        try {
            String fileKey = getFileKey(new FileInfo(basePath, pre.getPath(), pre.getFilename()));

            // 将过期时间转换为 Duration
            Duration duration = Duration.ofMillis(pre.getExpiration().getTime() - System.currentTimeMillis());

            GeneratePresignedUrlResult result = new GeneratePresignedUrlResult(platform, basePath, pre);

            Map<String, String> responseHeaders = pre.getResponseHeaders().entrySet().stream()
                    .collect(Collectors.toMap(e -> "response-" + e.getKey(), Map.Entry::getValue));

            String method = String.valueOf(pre.getMethod());

            S3Request.Builder requestBuilder;
            if ("GET".equalsIgnoreCase(method)) {
                requestBuilder = GetObjectRequest.builder().bucket(bucketName).key(fileKey);
            } else if ("PUT".equalsIgnoreCase(method)) {
                requestBuilder = PutObjectRequest.builder().bucket(bucketName).key(fileKey);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                requestBuilder =
                        DeleteObjectRequest.builder().bucket(bucketName).key(fileKey);
            } else if ("CreateMultipartUpload".equalsIgnoreCase(method)) {
                requestBuilder = CreateMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(fileKey);
            } else if ("CompleteMultipartUpload".equalsIgnoreCase(method)) {
                requestBuilder = CompleteMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(fileKey);
            } else if ("AbortMultipartUpload".equalsIgnoreCase(method)) {
                requestBuilder =
                        AbortMultipartUploadRequest.builder().bucket(bucketName).key(fileKey);
            } else if ("UploadPart".equalsIgnoreCase(method)) {
                requestBuilder = UploadPartRequest.builder().bucket(bucketName).key(fileKey);
            } else {
                throw new RuntimeException("暂不支持 method：" + method);
            }

            // 合并用户元数据和请求头
            Map<String, String> headers = new HashMap<>(pre.getHeaders());
            pre.getUserMetadata()
                    .forEach((key, value) ->
                            headers.put(key.startsWith("x-amz-meta-") ? key : "x-amz-meta-" + key, value));

            AwsRequestOverrideConfiguration.Builder configBuilder = AwsRequestOverrideConfiguration.builder();
            headers.forEach(configBuilder::putHeader);
            pre.getQueryParams().forEach(configBuilder::putRawQueryParameter);
            requestBuilder.overrideConfiguration(configBuilder.build());

            CopyOptions copyOptions = CopyOptions.create()
                    .ignoreCase()
                    .setFieldNameEditor(name -> NamingCase.toCamelCase(name, CharUtil.DASHED));
            BeanUtil.copyProperties(responseHeaders, requestBuilder, copyOptions);

            // 生成预签名 URL
            PresignedRequest presigned;
            if ("GET".equalsIgnoreCase(method)) {
                presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .getObjectRequest((GetObjectRequest) requestBuilder.build())
                        .signatureDuration(duration)
                        .build());
            } else if ("PUT".equalsIgnoreCase(method)) {
                presigned = presigner.presignPutObject(PutObjectPresignRequest.builder()
                        .putObjectRequest((PutObjectRequest) requestBuilder.build())
                        .signatureDuration(duration)
                        .build());
            } else if ("DELETE".equalsIgnoreCase(method)) {
                presigned = presigner.presignDeleteObject(DeleteObjectPresignRequest.builder()
                        .deleteObjectRequest((DeleteObjectRequest) requestBuilder.build())
                        .signatureDuration(duration)
                        .build());
            } else if ("CreateMultipartUpload".equalsIgnoreCase(method)) {
                presigned = presigner.presignCreateMultipartUpload(CreateMultipartUploadPresignRequest.builder()
                        .createMultipartUploadRequest((CreateMultipartUploadRequest) requestBuilder.build())
                        .signatureDuration(duration)
                        .build());
            } else if ("CompleteMultipartUpload".equalsIgnoreCase(method)) {
                presigned = presigner.presignCompleteMultipartUpload(CompleteMultipartUploadPresignRequest.builder()
                        .completeMultipartUploadRequest((CompleteMultipartUploadRequest) requestBuilder.build())
                        .signatureDuration(duration)
                        .build());
            } else if ("AbortMultipartUpload".equalsIgnoreCase(method)) {
                presigned = presigner.presignAbortMultipartUpload(AbortMultipartUploadPresignRequest.builder()
                        .abortMultipartUploadRequest((AbortMultipartUploadRequest) requestBuilder.build())
                        .signatureDuration(duration)
                        .build());
            } else if ("UploadPart".equalsIgnoreCase(method)) {
                presigned = presigner.presignUploadPart(UploadPartPresignRequest.builder()
                        .uploadPartRequest((UploadPartRequest) requestBuilder.build())
                        .signatureDuration(duration)
                        .build());
            } else {
                throw new RuntimeException("暂不支持 method：" + method);
            }

            // 将结果封装到 GeneratePresignedUrlResult 中
            result.setUrl(presigned.url().toString());
            result.setHeaders(presigned.signedHeaders().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, v -> CollUtil.get(v.getValue(), 0))));
            return result;
        } catch (Exception e) {
            throw ExceptionFactory.generatePresignedUrl(pre, e);
        }
    }

    public boolean isSupportAcl() {
        return true;
    }

    @Override
    public boolean setFileAcl(FileInfo fileInfo, Object acl) {
        String oAcl = getAcl(acl);
        try {
            if (oAcl == null) return false;
            getClient()
                    .getClient()
                    .putObjectAcl(PutObjectAclRequest.builder()
                            .bucket(bucketName)
                            .key(getFileKey(fileInfo))
                            .acl(oAcl)
                            .build());
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.setFileAcl(fileInfo, oAcl, platform, e);
        }
    }

    @Override
    public boolean setThFileAcl(FileInfo fileInfo, Object acl) {
        String oAcl = getAcl(acl);
        try {
            if (oAcl == null) return false;
            getClient()
                    .getClient()
                    .putObjectAcl(PutObjectAclRequest.builder()
                            .bucket(bucketName)
                            .key(getThFileKey(fileInfo))
                            .acl(oAcl)
                            .build());
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
        S3Client client = getClient().getClient();
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(getThFileKey(fileInfo))
                        .build());
            }
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(getFileKey(fileInfo))
                    .build());
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            // 如果文件存在，headObject 不会抛出异常
            getClient()
                    .getClient()
                    .headObject(HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(getFileKey(fileInfo))
                            .build());
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false; // 如果状态码是 404，则表示文件不存在
            }
            throw ExceptionFactory.exists(fileInfo, platform, e);
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        S3Client client = getClient().getClient();
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(getFileKey(fileInfo))
                    .build();

            // 下载文件，获取对象的 InputStream
            try (InputStream in = client.getObject(request)) {
                consumer.accept(in);
            }
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);

        S3Client client = getClient().getClient();
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(getThFileKey(fileInfo))
                    .build();

            // 下载文件，获取对象的 InputStream
            try (InputStream in = client.getObject(request)) {
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

        S3Client client = getClient().getClient();

        // 获取远程文件信息
        String srcFileKey = getFileKey(srcFileInfo);
        HeadObjectResponse srcFile;
        try {
            srcFile = client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(srcFileKey)
                    .build());
        } catch (Exception e) {
            throw ExceptionFactory.sameCopyNotFound(srcFileInfo, destFileInfo, platform, e);
        }

        // 复制缩略图文件
        String destThFileKey = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                client.copyObject(CopyObjectRequest.builder()
                        .sourceBucket(bucketName)
                        .sourceKey(getThFileKey(srcFileInfo))
                        .destinationBucket(bucketName)
                        .destinationKey(destThFileKey)
                        .acl(getAcl(destFileInfo.getThFileAcl()))
                        .build());
            } catch (Exception e) {
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 复制文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        long fileSize = srcFile.contentLength();
        boolean useMultipartCopy = fileSize >= 1024 * 1024 * 1024; // 小于 1GB，走小文件复制
        String uploadId = null;
        try {
            if (useMultipartCopy) { // 大文件复制
                uploadId = client.createMultipartUpload(setMetadata(
                                        CreateMultipartUploadRequest.builder()
                                                .bucket(bucketName)
                                                .key(destFileKey),
                                        destFileInfo)
                                .build())
                        .uploadId();
                ProgressListener.quickStart(pre.getProgressListener(), fileSize);
                ArrayList<CompletedPart> partList = new ArrayList<>();
                long progressSize = 0;
                int i = 0;
                while (progressSize < fileSize) {
                    long partSize = Math.min(256 * 1024 * 1024, fileSize - progressSize);
                    UploadPartCopyRequest part = UploadPartCopyRequest.builder()
                            .sourceBucket(bucketName)
                            .sourceKey(srcFileKey)
                            .destinationBucket(bucketName)
                            .destinationKey(destFileKey)
                            .uploadId(uploadId)
                            .partNumber(++i)
                            .copySourceRange("bytes=" + progressSize + "-" + (progressSize + partSize - 1))
                            .build();
                    partList.add(CompletedPart.builder()
                            .partNumber(i)
                            .eTag(client.uploadPartCopy(part).copyPartResult().eTag())
                            .build());
                    ProgressListener.quickProgress(pre.getProgressListener(), progressSize += partSize, fileSize);
                }
                client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(destFileKey)
                        .uploadId(uploadId)
                        .multipartUpload(CompletedMultipartUpload.builder()
                                .parts(partList)
                                .build())
                        .build());
                ProgressListener.quickFinish(pre.getProgressListener());
            } else { // 小文件复制
                ProgressListener.quickStart(pre.getProgressListener(), fileSize);
                client.copyObject(setMetadata(
                                CopyObjectRequest.builder()
                                        .sourceBucket(bucketName)
                                        .sourceKey(srcFileKey)
                                        .destinationBucket(bucketName)
                                        .destinationKey(destFileKey),
                                destFileInfo)
                        .build());
                ProgressListener.quickFinish(pre.getProgressListener(), fileSize);
            }
        } catch (Exception e) {
            if (destThFileKey != null) {
                try {
                    client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(destThFileKey)
                            .build());
                } catch (Exception ignored) {
                }
            }
            try {
                if (useMultipartCopy) {
                    client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                            .bucket(bucketName)
                            .key(destFileKey)
                            .uploadId(uploadId)
                            .build());
                } else {
                    client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(destFileKey)
                            .build());
                }
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameCopy(srcFileInfo, destFileInfo, platform, e);
        }
    }
}
