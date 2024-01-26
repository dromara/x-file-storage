package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Multimap;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.minio.messages.ListBucketResultV1;
import io.minio.messages.ListPartsResult;
import io.minio.messages.Part;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.MinioConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.get.*;
import org.dromara.x.file.storage.core.upload.*;
import org.dromara.x.file.storage.core.util.Tools;

/**
 * MinIO 存储
 */
@Getter
@Setter
@NoArgsConstructor
public class MinioFileStorage implements FileStorage {
    private String platform;
    private String bucketName;
    private String domain;
    private String basePath;
    private int multipartThreshold;
    private int multipartPartSize;
    private FileStorageClientFactory<MinioClient> clientFactory;

    public MinioFileStorage(MinioConfig config, FileStorageClientFactory<MinioClient> clientFactory) {
        platform = config.getPlatform();
        bucketName = config.getBucketName();
        domain = config.getDomain();
        basePath = config.getBasePath();
        multipartThreshold = config.getMultipartThreshold();
        multipartPartSize = config.getMultipartPartSize();
        this.clientFactory = clientFactory;
    }

    public MinioClient getClient() {
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
        Check.uploadNotSupportAcl(platform, fileInfo, pre);
        MinioClient client = getClient();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            // MinIO 的 SDK 内部会自动分片上传
            Long objectSize = fileInfo.getSize();
            long partSize = -1;
            if (fileInfo.getSize() == null || fileInfo.getSize() >= multipartThreshold) {
                objectSize = -1L;
                partSize = multipartPartSize;
            }
            client.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(newFileKey).stream(in, objectSize, partSize)
                            .contentType(fileInfo.getContentType())
                            .headers(fileInfo.getMetadata())
                            .userMetadata(fileInfo.getUserMetadata())
                            .build());

            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                client.putObject(PutObjectArgs.builder().bucket(bucketName).object(newThFileKey).stream(
                                new ByteArrayInputStream(thumbnailBytes), thumbnailBytes.length, -1)
                        .contentType(fileInfo.getThContentType())
                        .headers(fileInfo.getThMetadata())
                        .userMetadata(fileInfo.getThUserMetadata())
                        .build());
            }

            return true;
        } catch (Exception e) {
            try {
                client.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(newFileKey)
                        .build());
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.upload(fileInfo, platform, e);
        }
    }

    /**
     * 通过反射获取 MinIO 异步操作对象
     */
    public MinioAsyncClient getMinioAsyncClient(MinioClient client) {
        return (MinioAsyncClient) ReflectUtil.getFieldValue(client, "asyncClient");
    }

    /**
     * 通过反射调用内部的分片上传初始化方法
     */
    public CreateMultipartUploadResponse initiateMultipartUpload(MinioClient client, PutObjectArgs args)
            throws ExecutionException, InterruptedException {
        MinioAsyncClient asyncClient = getMinioAsyncClient(client);

        Multimap<String, String> headers = ReflectUtil.invoke(asyncClient, "newMultimap", args.extraHeaders());
        headers.putAll(args.genHeaders());

        java.lang.reflect.Method method =
                ReflectUtil.getMethodByName(asyncClient.getClass(), "createMultipartUploadAsync");

        CompletableFuture<CreateMultipartUploadResponse> cf = ReflectUtil.invoke(
                asyncClient, method, args.bucket(), args.region(), args.object(), headers, args.extraQueryParams());

        return cf.get();
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
        Check.uploadNotSupportAcl(platform, fileInfo, pre);
        MinioClient client = getClient();
        try {
            PutObjectArgs.Builder builder =
                    PutObjectArgs.builder().bucket(bucketName).object(newFileKey);
            if (fileInfo.getContentType() != null) builder.contentType(fileInfo.getContentType());
            PutObjectArgs args =
                    builder.headers(fileInfo.getMetadata()).userMetadata(fileInfo.getUserMetadata()).stream(
                                    new ByteArrayInputStream(new byte[0]), 0, 0)
                            .build();

            String uploadId = initiateMultipartUpload(client, args).result().uploadId();
            fileInfo.setUploadId(uploadId);
        } catch (Exception e) {
            throw ExceptionFactory.initiateMultipartUpload(fileInfo, platform, e);
        }
    }

    /**
     * 通过反射调用内部的上传分片方法
     */
    public UploadPartResponse uploadPart(MinioClient client, String uploadId, int partNumber, PutObjectArgs args)
            throws ExecutionException, InterruptedException {
        MinioAsyncClient asyncClient = getMinioAsyncClient(client);

        java.lang.reflect.Method newPartReaderMethod =
                ReflectUtil.getMethodByName(asyncClient.getClass(), "newPartReader");

        Object partReader = ReflectUtil.invoke(
                asyncClient, newPartReaderMethod, args.stream(), args.objectSize(), args.partSize(), 1);
        Object partSource = ReflectUtil.invoke(partReader, "getPart");

        java.lang.reflect.Method uploadPartsMethod =
                ReflectUtil.getMethodByName(asyncClient.getClass(), "uploadPartAsync");
        CompletableFuture<UploadPartResponse> result = ReflectUtil.invoke(
                asyncClient,
                uploadPartsMethod,
                args.bucket(),
                args.region(),
                args.object(),
                partSource,
                partNumber,
                uploadId,
                null,
                null);
        return result.get();
    }

    @Override
    public FilePartInfo uploadPart(UploadPartPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        MinioClient client = getClient();
        FileWrapper partFileWrapper = pre.getPartFileWrapper();
        Long partSize = partFileWrapper.getSize();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            // MinIO 比较特殊，上传分片必须传入分片大小，这里强制获取，可能会占用大量内存
            if (partSize == null) partSize = partFileWrapper.getInputStreamMaskResetReturn(Tools::getSize);

            PutObjectArgs args = PutObjectArgs.builder().bucket(bucketName).object(newFileKey).stream(in, partSize, -1)
                    .build();

            UploadPartResponse part = uploadPart(client, fileInfo.getUploadId(), pre.getPartNumber(), args);
            FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
            filePartInfo.setETag(part.etag());
            filePartInfo.setPartNumber(part.partNumber());
            filePartInfo.setPartSize(in.getProgressSize());
            filePartInfo.setCreateTime(new Date());
            return filePartInfo;
        } catch (Exception e) {
            throw ExceptionFactory.uploadPart(fileInfo, platform, e);
        }
    }

    /**
     * 通过反射调用内部的分片上传完成方法
     */
    public ObjectWriteResponse completeMultipartUpload(
            MinioClient client, String uploadId, Part[] parts, PutObjectArgs args)
            throws ExecutionException, InterruptedException {
        MinioAsyncClient asyncClient = getMinioAsyncClient(client);

        java.lang.reflect.Method method =
                ReflectUtil.getMethodByName(asyncClient.getClass(), "completeMultipartUploadAsync");

        CompletableFuture<ObjectWriteResponse> cf = ReflectUtil.invoke(
                asyncClient, method, args.bucket(), args.region(), args.object(), uploadId, parts, null, null);

        return cf.get();
    }

    @Override
    public void completeMultipartUpload(CompleteMultipartUploadPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);

        MinioClient client = getClient();
        try {
            PutObjectArgs args = PutObjectArgs.builder().bucket(bucketName).object(newFileKey).stream(
                            new ByteArrayInputStream(new byte[0]), 0, 0)
                    .build();

            Part[] parts = pre.getPartInfoList().stream()
                    .map(part -> new Part(part.getPartNumber(), part.getETag()))
                    .toArray(Part[]::new);

            ProgressListener.quickStart(pre.getProgressListener(), fileInfo.getSize());
            completeMultipartUpload(client, fileInfo.getUploadId(), parts, args);
            ProgressListener.quickFinish(pre.getProgressListener(), fileInfo.getSize());
        } catch (Exception e) {
            throw ExceptionFactory.completeMultipartUpload(fileInfo, platform, e);
        }
    }

    /**
     * 通过反射调用内部的分片上传取消方法
     */
    public AbortMultipartUploadResponse abortMultipartUpload(MinioClient client, String uploadId, PutObjectArgs args)
            throws ExecutionException, InterruptedException {
        MinioAsyncClient asyncClient = getMinioAsyncClient(client);

        java.lang.reflect.Method method =
                ReflectUtil.getMethodByName(asyncClient.getClass(), "abortMultipartUploadAsync");

        CompletableFuture<AbortMultipartUploadResponse> cf = ReflectUtil.invoke(
                asyncClient, method, args.bucket(), args.region(), args.object(), uploadId, null, null);

        return cf.get();
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        MinioClient client = getClient();
        try {
            PutObjectArgs args = PutObjectArgs.builder().bucket(bucketName).object(newFileKey).stream(
                            new ByteArrayInputStream(new byte[0]), 0, 0)
                    .build();

            abortMultipartUpload(client, fileInfo.getUploadId(), args);
        } catch (Exception e) {
            throw ExceptionFactory.abortMultipartUpload(fileInfo, platform, e);
        }
    }

    /**
     * 通过反射调用内部的列举已上传的分片方法
     */
    public ListPartsResult listParts(
            MinioClient client, String uploadId, Integer maxParts, Integer partNumberMarker, PutObjectArgs args)
            throws ExecutionException, InterruptedException {
        MinioAsyncClient asyncClient = getMinioAsyncClient(client);

        java.lang.reflect.Method method = ReflectUtil.getMethodByName(asyncClient.getClass(), "listPartsAsync");

        Multimap<String, String> headers = ReflectUtil.invoke(asyncClient, "newMultimap", args.extraHeaders());
        headers.putAll(args.genHeaders());

        CompletableFuture<ListPartsResponse> result = ReflectUtil.invoke(
                asyncClient,
                method,
                args.bucket(),
                args.region(),
                args.object(),
                maxParts,
                partNumberMarker,
                uploadId,
                headers,
                args.extraQueryParams());
        return result.get().result();
    }

    @Override
    public FilePartInfoList listParts(ListPartsPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        MinioClient client = getClient();
        try {
            PutObjectArgs args = PutObjectArgs.builder().bucket(bucketName).object(newFileKey).stream(
                            new ByteArrayInputStream(new byte[0]), 0, 0)
                    .build();

            ListPartsResult result =
                    listParts(client, fileInfo.getUploadId(), pre.getMaxParts(), pre.getPartNumberMarker(), args);
            FilePartInfoList list = new FilePartInfoList();
            list.setFileInfo(fileInfo);
            list.setList(result.partList().stream()
                    .map(p -> {
                        FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
                        filePartInfo.setETag(p.etag());
                        filePartInfo.setPartNumber(p.partNumber());
                        filePartInfo.setPartSize(p.partSize());
                        filePartInfo.setLastModified(DateUtil.date(p.lastModified()));
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

    /**
     * 通过反射调用内部的列举文件方法
     */
    public ListBucketResultV1 listFiles(MinioClient client, ListObjectsArgs args)
            throws ExecutionException, InterruptedException {
        MinioAsyncClient asyncClient = getMinioAsyncClient(client);
        java.lang.reflect.Method method = ReflectUtil.getMethod(
                asyncClient.getClass(),
                "listObjectsV1",
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                Integer.class,
                String.class,
                Multimap.class,
                Multimap.class);
        ListObjectsV1Response result = ReflectUtil.invoke(
                asyncClient,
                method,
                args.bucket(),
                args.region(),
                args.delimiter(),
                args.useUrlEncodingType() ? "url" : null,
                args.marker(),
                args.maxKeys(),
                args.prefix(),
                args.extraHeaders(),
                args.extraQueryParams());
        return result.result();
    }

    @Override
    public ListFilesResult listFiles(ListFilesPretreatment pre) {
        MinioClient client = getClient();
        try {
            ListObjectsArgs args = ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .maxKeys(pre.getMaxFiles())
                    .marker(pre.getMarker())
                    .delimiter("/")
                    .prefix(basePath + pre.getPath() + pre.getFilenamePrefix())
                    .build();
            ListBucketResultV1 result = listFiles(client, args);
            ListFilesResult list = new ListFilesResult();
            list.setDirList(result.commonPrefixes().stream()
                    .map(p -> {
                        RemoteDirInfo dir = new RemoteDirInfo();
                        dir.setPlatform(pre.getPlatform());
                        dir.setBasePath(basePath);
                        dir.setPath(pre.getPath());
                        dir.setName(FileNameUtil.getName(p.toItem().objectName()));
                        return dir;
                    })
                    .collect(Collectors.toList()));

            list.setFileList(result.contents().stream()
                    .map(p -> {
                        RemoteFileInfo remoteFileInfo = new RemoteFileInfo();
                        remoteFileInfo.setPlatform(pre.getPlatform());
                        remoteFileInfo.setBasePath(basePath);
                        remoteFileInfo.setPath(pre.getPath());
                        remoteFileInfo.setFilename(FileNameUtil.getName(p.objectName()));
                        remoteFileInfo.setSize(p.size());
                        remoteFileInfo.setExt(FileNameUtil.extName(remoteFileInfo.getFilename()));
                        remoteFileInfo.setETag(p.etag());
                        remoteFileInfo.setLastModified(DateUtil.date(p.lastModified()));
                        remoteFileInfo.setOriginal(p);
                        return remoteFileInfo;
                    })
                    .collect(Collectors.toList()));
            list.setPlatform(pre.getPlatform());
            list.setBasePath(basePath);
            list.setPath(pre.getPath());
            list.setFilenamePrefix(pre.getFilenamePrefix());
            list.setMaxFiles(result.maxKeys());
            list.setIsTruncated(result.isTruncated());
            list.setMarker(result.marker());
            list.setNextMarker(result.nextMarker());
            return list;
        } catch (Exception e) {
            throw ExceptionFactory.listFiles(pre, basePath, e);
        }
    }

    @Override
    public boolean isSupportPresignedUrl() {
        return true;
    }

    @Override
    public String generatePresignedUrl(FileInfo fileInfo, Date expiration) {
        int expiry = (int) ((expiration.getTime() - System.currentTimeMillis()) / 1000);
        try {
            GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(getFileKey(fileInfo))
                    .method(Method.GET)
                    .expiry(expiry)
                    .build();
            return getClient().getPresignedObjectUrl(args);
        } catch (Exception e) {
            throw ExceptionFactory.generatePresignedUrl(fileInfo, platform, e);
        }
    }

    @Override
    public String generateThPresignedUrl(FileInfo fileInfo, Date expiration) {
        String key = getThFileKey(fileInfo);
        if (key == null) return null;
        int expiry = (int) ((expiration.getTime() - System.currentTimeMillis()) / 1000);
        try {
            GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .method(Method.GET)
                    .expiry(expiry)
                    .build();
            return getClient().getPresignedObjectUrl(args);
        } catch (Exception e) {
            throw ExceptionFactory.generateThPresignedUrl(fileInfo, platform, e);
        }
    }

    @Override
    public boolean isSupportMetadata() {
        return true;
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        MinioClient client = getClient();
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                client.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(getThFileKey(fileInfo))
                        .build());
            }
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(getFileKey(fileInfo))
                    .build());
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        MinioClient client = getClient();
        try {
            StatObjectResponse stat = client.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(getFileKey(fileInfo))
                    .build());
            return stat != null && stat.lastModified() != null;
        } catch (ErrorResponseException e) {
            String code = e.errorResponse().code();
            if ("NoSuchKey".equals(code)) {
                return false;
            }
            throw ExceptionFactory.exists(fileInfo, platform, e);
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        MinioClient client = getClient();
        try (InputStream in = client.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(getFileKey(fileInfo))
                .build())) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);

        MinioClient client = getClient();
        try (InputStream in = client.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(getThFileKey(fileInfo))
                .build())) {
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
        Check.sameCopyNotSupportAcl(platform, srcFileInfo, destFileInfo, pre);
        Check.sameCopyBasePath(platform, basePath, srcFileInfo, destFileInfo);
        MinioClient client = getClient();

        // 获取远程文件信息
        String srcFileKey = getFileKey(srcFileInfo);
        StatObjectResponse srcFile;
        try {
            srcFile = client.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(srcFileKey)
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
                client.copyObject(CopyObjectArgs.builder()
                        .bucket(bucketName)
                        .object(destThFileKey)
                        .source(CopySource.builder()
                                .bucket(bucketName)
                                .object(getThFileKey(srcFileInfo))
                                .build())
                        .build());
            } catch (Exception e) {
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 复制文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        long fileSize = srcFile.size();
        boolean useMultipartCopy = fileSize >= 1024 * 1024 * 1024; // 小于 1GB，走小文件复制
        try {
            ProgressListener.quickStart(pre.getProgressListener(), fileSize);
            if (useMultipartCopy) { // 大文件复制，MinIO 内部会自动走分片上传，不会自动复制 Metadata，需要重新设置
                Map<String, String> headers = new HashMap<>();
                headers.put(Constant.Metadata.CONTENT_TYPE, destFileInfo.getContentType());
                headers.putAll(destFileInfo.getMetadata());
                client.composeObject(ComposeObjectArgs.builder()
                        .bucket(bucketName)
                        .object(destFileKey)
                        .headers(headers)
                        .userMetadata(destFileInfo.getUserMetadata())
                        .sources(Collections.singletonList(ComposeSource.builder()
                                .bucket(bucketName)
                                .object(srcFileKey)
                                .offset(0L)
                                .length(fileSize)
                                .build()))
                        .build());
            } else { // 小文件复制，MinIO 内部会自动复制 Metadata
                client.copyObject(CopyObjectArgs.builder()
                        .bucket(bucketName)
                        .object(destFileKey)
                        .source(CopySource.builder()
                                .bucket(bucketName)
                                .object(srcFileKey)
                                .build())
                        .build());
            }
            ProgressListener.quickFinish(pre.getProgressListener(), fileSize);
        } catch (Exception e) {
            if (destThFileKey != null) {
                try {
                    client.removeObject(RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(destThFileKey)
                            .build());
                } catch (Exception ignored) {
                }
            }
            try {
                client.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(destFileKey)
                        .build());
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameCopy(srcFileInfo, destFileInfo, platform, e);
        }
    }
}
