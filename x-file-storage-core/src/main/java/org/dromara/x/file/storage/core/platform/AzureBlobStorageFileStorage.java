package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.map.CaseInsensitiveMap;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.text.NamingCase;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.azure.core.http.rest.ResponseBase;
import com.azure.core.util.Context;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.implementation.models.BlobItemPropertiesInternal;
import com.azure.storage.blob.implementation.models.ContainersListBlobHierarchySegmentHeaders;
import com.azure.storage.blob.implementation.models.ListBlobsHierarchySegmentResponse;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.azure.storage.blob.options.BlockBlobCommitBlockListOptions;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.azure.storage.common.sas.SasIpRange;
import com.azure.storage.common.sas.SasProtocol;
import com.azure.storage.file.datalake.*;
import com.azure.storage.file.datalake.models.PathAccessControlEntry;
import com.azure.storage.file.datalake.models.PathPermissions;
import com.azure.storage.file.datalake.models.RolePermissions;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.AzureBlobStorageConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.get.*;
import org.dromara.x.file.storage.core.platform.AzureBlobStorageFileStorageClientFactory.AzureBlobStorageClient;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlPretreatment;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;
import org.dromara.x.file.storage.core.upload.*;
import org.dromara.x.file.storage.core.util.KebabCaseInsensitiveMap;
import org.dromara.x.file.storage.core.util.Tools;
import reactor.core.publisher.Mono;

/**
 * Azure Blob Storage
 * @author dongfeng <dongfeng@51ddi.com> XuYanwu <1171736840@qq.com>
 */
@Getter
@Setter
@NoArgsConstructor
public class AzureBlobStorageFileStorage implements FileStorage {

    /**
     * 平台名称唯一标识，方便多个存储
     */
    private String platform;

    /**
     * 与s3的bucket大差不差
     */
    private String containerName;

    /**
     * 访问url的路径名称
     */
    private String domain;

    /**
     * 基础路径
     */
    private String basePath;

    /**
     * 默认的 ACL
     */
    private String defaultAcl;

    /**
     * {@link com.azure.storage.blob.implementation.util.ModelHelper#populateAndApplyDefaults(ParallelTransferOptions)}
     * 触发分片上传的阈值
     * 默认值256M
     */
    private Long multipartThreshold;

    /**
     * 触发分片后 ,分片块大小
     * 默认值 4M
     */
    private Long multipartPartSize;

    /**
     * 最大上传并行度
     * 分片后 同时进行上传的 数量
     * 数量太大会占用大量缓冲区
     * 默认 8
     */
    private Integer maxConcurrency;

    /**
     * 预签名 URL 时，传入的 HTTP method 与 Azure Blob Storage 中的 SAS 权限映射表
     * {@link com.azure.storage.blob.sas.BlobSasPermission}
     */
    private CaseInsensitiveMap<String, String> methodToPermissionMap;

    private FileStorageClientFactory<AzureBlobStorageClient> clientFactory;

    public AzureBlobStorageFileStorage(
            AzureBlobStorageConfig config, FileStorageClientFactory<AzureBlobStorageClient> clientFactory) {
        platform = config.getPlatform();
        containerName = config.getContainerName();
        domain = config.getDomain();
        basePath = config.getBasePath();
        defaultAcl = config.getDefaultAcl();
        multipartThreshold = config.getMultipartThreshold();
        multipartPartSize = config.getMultipartPartSize();
        maxConcurrency = config.getMaxConcurrency();
        methodToPermissionMap = new CaseInsensitiveMap<>(config.getMethodToPermissionMap());
        this.clientFactory = clientFactory;
    }

    public AzureBlobStorageClient getClient() {
        return clientFactory.getClient();
    }

    public BlobContainerClient getBlobClient() {
        return getClient().getBlobServiceClient().getBlobContainerClient(containerName);
    }

    public BlobClient getBlobClient(String fileKey) {
        if (StrUtil.isBlank(fileKey)) return null;
        return getBlobClient().getBlobClient(fileKey);
    }

    public DataLakeFileClient getDataLakeFileClient(String fileKey) {
        return getClient()
                .getDataLakeServiceClient()
                .getFileSystemClient(containerName)
                .getFileClient(fileKey);
    }

    public String getUrl(String fileKey) {
        return domain + containerName + "/" + fileKey;
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(getUrl(newFileKey));
        AclWrapper acl = getAcl(fileInfo.getFileAcl());
        ProgressListener listener = pre.getProgressListener();
        BlobClient blobClient = getBlobClient(newFileKey);
        BlobClient thBlobClient = null;
        try (InputStreamPlus in = pre.getInputStreamPlus(false)) {
            // 构建上传参数，经测试，大文件会自动多线程分片上传，且无需指定文件大小
            BlobParallelUploadOptions options = new BlobParallelUploadOptions(in);
            options.setMetadata(fileInfo.getUserMetadata());
            options.setHeaders(getBlobHttpHeaders(fileInfo.getContentType(), fileInfo.getMetadata()));
            options.setParallelTransferOptions(new ParallelTransferOptions()
                    .setBlockSizeLong(multipartPartSize)
                    .setMaxConcurrency(maxConcurrency)
                    .setMaxSingleUploadSizeLong(multipartThreshold));
            if (listener != null) {
                options.getParallelTransferOptions()
                        .setProgressListener(progressSize -> listener.progress(progressSize, fileInfo.getSize()));
            }
            ProgressListener.quickStart(listener, fileInfo.getSize());
            blobClient.uploadWithResponse(options, null, Context.NONE);
            setFileAcl(newFileKey, acl);
            ProgressListener.quickFinish(listener);
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());
            // 上传缩略图
            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) {
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(getUrl(newThFileKey));
                AclWrapper thAcl = getAcl(fileInfo.getThFileAcl());
                BlobParallelUploadOptions thOptions =
                        new BlobParallelUploadOptions(new ByteArrayInputStream(thumbnailBytes));
                thOptions.setMetadata(fileInfo.getThUserMetadata());
                thOptions.setHeaders(getBlobHttpHeaders(fileInfo.getThContentType(), fileInfo.getThMetadata()));
                thBlobClient = getBlobClient(newThFileKey);
                thBlobClient.uploadWithResponse(thOptions, null, Context.NONE);
                setFileAcl(newFileKey, thAcl);
            }
            return true;
        } catch (Exception e) {
            try {
                blobClient.deleteIfExists();
            } catch (Exception ignored) {
            }
            if (thBlobClient != null) {
                try {
                    thBlobClient.deleteIfExists();
                } catch (Exception ignored) {
                }
            }
            throw ExceptionFactory.upload(fileInfo, platform, e);
        }
    }

    public MultipartUploadSupportInfo isSupportMultipartUpload() {
        return MultipartUploadSupportInfo.supportAll().setListPartsSupportMaxParts(50000);
    }

    public void initiateMultipartUpload(FileInfo fileInfo, InitiateMultipartUploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(getUrl(newFileKey));
        BlockBlobClient blobClient = getBlobClient(getFileKey(fileInfo)).getBlockBlobClient();
        try {
            String uploadId = IdUtil.objectId();
            fileInfo.setUploadId(uploadId);
            String blockIdBase64 = Base64.encode(String.format("%06d", 0));
            byte[] bytes = fileInfo.getUploadId().getBytes(StandardCharsets.UTF_8);
            blobClient.stageBlock(blockIdBase64, new ByteArrayInputStream(bytes), bytes.length);
        } catch (Exception e) {
            throw ExceptionFactory.initiateMultipartUpload(fileInfo, platform, e);
        }
    }

    /**
     * 手动分片上传-上传分片
     */
    public FilePartInfo uploadPart(UploadPartPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        BlockBlobClient blobClient = getBlobClient(getFileKey(fileInfo)).getBlockBlobClient();

        FileWrapper partFileWrapper = pre.getPartFileWrapper();
        Long partSize = partFileWrapper.getSize();
        pre.setHashCalculatorMd5();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            // Azure Blob Storage 比较特殊，上传分片必须传入分片大小，这里强制获取，可能会占用大量内存
            if (partSize == null) partSize = partFileWrapper.getInputStreamMaskResetReturn(Tools::getSize);
            String blockIdBase64 = Base64.encode(String.format("%06d", pre.getPartNumber()));
            blobClient.stageBlock(blockIdBase64, in, partSize);
            String etag = pre.getHashCalculatorManager().getHashInfo().getMd5();
            FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
            filePartInfo.setETag(etag);
            filePartInfo.setPartNumber(pre.getPartNumber());
            filePartInfo.setPartSize(in.getProgressSize());
            filePartInfo.setCreateTime(new Date());
            return filePartInfo;
        } catch (Exception e) {
            throw ExceptionFactory.uploadPart(fileInfo, platform, e);
        }
    }

    public void completeMultipartUpload(CompleteMultipartUploadPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        AclWrapper acl = getAcl(fileInfo.getFileAcl());
        BlockBlobClient client = getBlobClient(newFileKey).getBlockBlobClient();
        try {
            List<String> partList = pre.getPartInfoList().stream()
                    .sorted(Comparator.comparingInt(FilePartInfo::getPartNumber))
                    .map(p -> Base64.encode(String.format("%06d", p.getPartNumber())))
                    .collect(Collectors.toList());
            BlockBlobCommitBlockListOptions options = new BlockBlobCommitBlockListOptions(partList);
            options.setMetadata(fileInfo.getUserMetadata());
            options.setHeaders(getBlobHttpHeaders(fileInfo.getContentType(), fileInfo.getMetadata()));
            setFileAcl(newFileKey, acl);
            client.commitBlockListWithResponse(options, null, Context.NONE).getValue();
            if (fileInfo.getSize() == null)
                fileInfo.setSize(client.getProperties().getBlobSize());
        } catch (Exception e) {
            throw ExceptionFactory.completeMultipartUpload(fileInfo, platform, e);
        }
    }

    /**
     * 手动分片上传-取消
     */
    public void abortMultipartUpload(AbortMultipartUploadPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        try {
            getBlobClient(getFileKey(fileInfo)).getBlockBlobClient().deleteIfExists();
        } catch (Exception e) {
            throw ExceptionFactory.abortMultipartUpload(fileInfo, platform, e);
        }
    }

    /**
     * 手动分片上传-列举已上传的分片
     */
    public FilePartInfoList listParts(ListPartsPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        BlockBlobClient client = getBlobClient(getFileKey(fileInfo)).getBlockBlobClient();
        try {
            BlockList blockList = client.listBlocks(BlockListType.UNCOMMITTED);

            List<FilePartInfo> partList = blockList.getUncommittedBlocks().stream()
                    .map(p -> {
                        FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
                        filePartInfo.setPartSize(p.getSizeLong());
                        filePartInfo.setPartNumber(Integer.parseInt(Base64.decodeStr(p.getName())));
                        return filePartInfo;
                    })
                    .filter(p -> p.getPartNumber() > pre.getPartNumberMarker())
                    .filter(v -> v.getPartNumber() > 0)
                    .sorted(Comparator.comparingInt(FilePartInfo::getPartNumber))
                    .collect(Collectors.toList());

            FilePartInfoList list = new FilePartInfoList();
            list.setFileInfo(fileInfo);
            list.setMaxParts(pre.getMaxParts());
            list.setPartNumberMarker(pre.getPartNumberMarker());

            if (partList.size() > pre.getMaxParts()) {
                list.setIsTruncated(true);
                partList = partList.subList(0, pre.getMaxParts());
                list.setNextPartNumberMarker(partList.get(partList.size() - 1).getPartNumber());
            } else {
                list.setIsTruncated(false);
            }
            list.setList(partList);

            return list;
        } catch (Exception e) {
            throw ExceptionFactory.listParts(fileInfo, platform, e);
        }
    }

    @Override
    public ListFilesSupportInfo isSupportListFiles() {
        return ListFilesSupportInfo.supportAll().setSupportMaxFiles(5000);
    }

    /**
     * 通过反射调用内部的列举文件方法
     */
    public ListBlobsHierarchySegmentResponse listFiles(
            BlobContainerClient client, String marker, String delimiter, ListBlobsOptions options, Duration timeout)
            throws ExecutionException, InterruptedException {
        BlobContainerAsyncClient asyncClient = (BlobContainerAsyncClient) ReflectUtil.getFieldValue(client, "client");
        Method method = ReflectUtil.getMethodByName(asyncClient.getClass(), "listBlobsHierarchySegment");
        Mono<ResponseBase<ContainersListBlobHierarchySegmentHeaders, ListBlobsHierarchySegmentResponse>> result =
                ReflectUtil.invoke(asyncClient, method, marker, delimiter, options, timeout);
        return Objects.requireNonNull(result.block()).getValue();
    }

    @Override
    public ListFilesResult listFiles(ListFilesPretreatment pre) {
        BlobContainerClient client = getBlobClient();
        try {

            ListBlobsOptions options = new ListBlobsOptions()
                    .setPrefix(basePath + pre.getPath() + pre.getFilenamePrefix())
                    .setMaxResultsPerPage(pre.getMaxFiles())
                    .setDetails(new BlobListDetails().setRetrieveMetadata(true));

            ListBlobsHierarchySegmentResponse result = listFiles(client, pre.getMarker(), "/", options, null);

            ListFilesResult list = new ListFilesResult();
            list.setDirList(result.getSegment().getBlobPrefixes().stream()
                    .map(item -> {
                        RemoteDirInfo dir = new RemoteDirInfo();
                        dir.setPlatform(pre.getPlatform());
                        dir.setBasePath(basePath);
                        dir.setPath(pre.getPath());
                        dir.setName(FileNameUtil.getName(item.getName().getContent()));
                        dir.setOriginal(item);
                        return dir;
                    })
                    .collect(Collectors.toList()));
            list.setFileList(result.getSegment().getBlobItems().stream()
                    .map(item -> {
                        RemoteFileInfo info = new RemoteFileInfo();
                        info.setPlatform(pre.getPlatform());
                        info.setBasePath(basePath);
                        info.setPath(pre.getPath());
                        info.setFilename(FileNameUtil.getName(item.getName().getContent()));
                        info.setUrl(domain + getFileKey(new FileInfo(basePath, info.getPath(), info.getFilename())));
                        BlobItemPropertiesInternal properties = item.getProperties();
                        info.setSize(properties.getContentLength());
                        info.setExt(FileNameUtil.extName(info.getFilename()));
                        info.setContentDisposition(properties.getContentDisposition());
                        info.setETag(properties.getETag());
                        info.setContentType(properties.getContentType());
                        info.setContentMd5(Base64.encode(properties.getContentMd5()));
                        info.setLastModified(DateUtil.date(properties.getLastModified()));
                        try {
                            info.setMetadata(BeanUtil.beanToMap(properties, false, true));
                        } catch (Exception ignored) {
                        }
                        if (item.getMetadata() != null) info.setUserMetadata(new HashMap<>(item.getMetadata()));
                        info.setOriginal(item);
                        return info;
                    })
                    .collect(Collectors.toList()));
            list.setPlatform(pre.getPlatform());
            list.setBasePath(basePath);
            list.setPath(pre.getPath());
            list.setFilenamePrefix(pre.getFilenamePrefix());
            list.setMaxFiles(result.getMaxResults());
            list.setIsTruncated(StrUtil.isNotBlank(result.getNextMarker()));
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
        try {
            BlobClient client = getBlobClient(fileKey);
            BlobProperties file;
            try {
                file = client.getProperties();
            } catch (Exception e) {
                return null;
            }
            if (file == null) return null;
            RemoteFileInfo info = new RemoteFileInfo();
            info.setPlatform(pre.getPlatform());
            info.setBasePath(basePath);
            info.setPath(pre.getPath());
            info.setFilename(FileNameUtil.getName(client.getBlobName()));
            info.setUrl(domain + fileKey);
            info.setSize(file.getBlobSize());
            info.setExt(FileNameUtil.extName(info.getFilename()));
            info.setETag(file.getETag());
            info.setContentDisposition(file.getContentDisposition());
            info.setContentType(file.getContentType());
            info.setContentMd5(Base64.encode(file.getContentMd5()));
            info.setLastModified(DateUtil.date(file.getLastModified()));
            try {
                info.setMetadata(BeanUtil.beanToMap(
                        ReflectUtil.getFieldValue(ReflectUtil.getFieldValue(file, "internalProperties"), "headers"),
                        false,
                        true));
            } catch (Exception ignored) {
            }
            if (file.getMetadata() != null) info.setUserMetadata(new HashMap<>(file.getMetadata()));
            info.setOriginal(file);
            return info;
        } catch (Exception e) {
            throw ExceptionFactory.getFile(pre, basePath, e);
        }
    }

    @Override
    public boolean isSupportAcl() {
        return true;
    }

    public AclWrapper getAcl(Object acl) {
        if (acl instanceof PathPermissions) {
            return new AclWrapper((PathPermissions) acl);
        } else if (acl instanceof String || acl == null) {
            String sAcl = (String) acl;
            if (StrUtil.isEmpty(sAcl)) sAcl = defaultAcl;
            if (StrUtil.isEmpty(sAcl)) return null;
            if (Constant.AzureBlobStorageACL.PRIVATE.equalsIgnoreCase(sAcl)) {
                return new AclWrapper(new PathPermissions()
                        .setGroup(new RolePermissions().setReadPermission(true))
                        .setOwner(new RolePermissions().setReadPermission(true).setWritePermission(true)));
            } else if (Constant.AzureBlobStorageACL.PUBLIC_READ.equalsIgnoreCase(sAcl)) {
                return new AclWrapper(new PathPermissions()
                        .setGroup(new RolePermissions().setReadPermission(true))
                        .setOwner(new RolePermissions().setReadPermission(true).setWritePermission(true))
                        .setOther(new RolePermissions().setReadPermission(true)));
            } else if (Constant.AzureBlobStorageACL.PUBLIC_READ_WRITE.equalsIgnoreCase(sAcl)) {
                return new AclWrapper(new PathPermissions()
                        .setGroup(new RolePermissions().setReadPermission(true).setWritePermission(true))
                        .setOwner(new RolePermissions().setReadPermission(true).setWritePermission(true))
                        .setOther(new RolePermissions().setReadPermission(true).setWritePermission(true)));
            }
        } else if (acl instanceof PathAccessControlEntry) {
            return new AclWrapper(Collections.singletonList((PathAccessControlEntry) acl));
        } else if (acl instanceof Collection) {
            List<PathAccessControlEntry> aclList = ((Collection<?>) acl)
                    .stream()
                            .map(item -> {
                                if (item instanceof PathAccessControlEntry) {
                                    return (PathAccessControlEntry) item;
                                } else {
                                    throw new FileStorageRuntimeException("不支持的ACL：" + item);
                                }
                            })
                            .collect(Collectors.toList());
            return new AclWrapper(aclList);
        }
        throw ExceptionFactory.unrecognizedAcl(acl, platform);
    }

    public void setFileAcl(String fileKey, AclWrapper acl) {
        if (acl == null) return;
        if (StrUtil.isBlank(fileKey)) return;
        DataLakeFileClient fileClient = getDataLakeFileClient(fileKey);
        //        PathAccessControl fileAccessControl = fileClient.getAccessControl();
        //        List<PathAccessControlEntry> pathPermissions = fileAccessControl.getAccessControlList();
        //        System.out.println(PathAccessControlEntry.serializeList(pathPermissions));
        if (acl.getPermissions() != null) {
            fileClient.setPermissions(acl.getPermissions(), null, null);
        } else if (acl.getAclList() != null) {
            fileClient.setAccessControlList(acl.getAclList(), null, null);
        } else {
            throw new NullPointerException();
        }
        //        pathPermissions = fileClient.getAccessControl().getAccessControlList();
        //        System.out.println(PathAccessControlEntry.serializeList(pathPermissions));
    }

    @Override
    public boolean setFileAcl(FileInfo fileInfo, Object acl) {
        AclWrapper oAcl = getAcl(acl);
        if (oAcl == null) return false;
        try {
            setFileAcl(getFileKey(fileInfo), oAcl);
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.setFileAcl(fileInfo, oAcl, platform, e);
        }
    }

    @Override
    public boolean setThFileAcl(FileInfo fileInfo, Object acl) {
        AclWrapper oAcl = getAcl(acl);
        if (oAcl == null) return false;
        try {
            setFileAcl(getThFileKey(fileInfo), oAcl);
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.setThFileAcl(fileInfo, oAcl, platform, e);
        }
    }

    public boolean isSupportMetadata() {
        return true;
    }

    /**
     * 获取 Blob 支持的请求头，通过 ContentType 及 Metadata
     */
    public BlobHttpHeaders getBlobHttpHeaders(String contentType, Map<String, String> metadata) {
        BlobHttpHeaders headers = new BlobHttpHeaders();
        if (StrUtil.isNotBlank(contentType)) headers.setContentType(contentType);
        if (CollUtil.isNotEmpty(metadata)) {
            CopyOptions copyOptions = CopyOptions.create()
                    .ignoreCase()
                    .setFieldNameEditor(name -> NamingCase.toCamelCase(name, CharUtil.DASHED));
            BeanUtil.copyProperties(metadata, headers, copyOptions);
        }
        return headers;
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                getBlobClient(getThFileKey(fileInfo)).deleteIfExists();
            }
            getBlobClient(getFileKey(fileInfo)).deleteIfExists();
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            return getBlobClient(getFileKey(fileInfo)).exists();
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        BlobClient blobClient = getBlobClient(getFileKey(fileInfo));
        try (InputStream in = blobClient.openInputStream()) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);
        BlobClient blobClient = getBlobClient(getThFileKey(fileInfo));
        try (InputStream in = blobClient.openInputStream()) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.downloadTh(fileInfo, platform, e);
        }
    }

    @Override
    public boolean isSupportPresignedUrl() {
        return true;
    }

    public BlobSasPermission getBlobSasPermission(Object object) {
        if (object instanceof BlobSasPermission) {
            return (BlobSasPermission) object;
        } else if (object instanceof String) {
            String permission = methodToPermissionMap.get(object);
            if (permission == null) permission = object.toString();
            return BlobSasPermission.parse(permission);
        }
        throw new IllegalArgumentException("无法识别的权限");
    }

    @Override
    public GeneratePresignedUrlResult generatePresignedUrl(GeneratePresignedUrlPretreatment pre) {
        try {
            String fileKey = getFileKey(new FileInfo(basePath, pre.getPath(), pre.getFilename()));
            Map<String, String> headers = new HashMap<>(pre.getHeaders());
            headers.putAll(pre.getUserMetadata().entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().startsWith("x-ms-meta-") ? e.getKey() : "x-ms-meta-" + e.getKey(),
                            Map.Entry::getValue)));

            BlobClient blobClient = getBlobClient(fileKey);
            BlobSasPermission permission = getBlobSasPermission(pre.getMethod());
            OffsetDateTime expiration =
                    pre.getExpiration().toInstant().atZone(ZoneOffset.UTC).toOffsetDateTime();

            // 生成签名
            BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiration, permission);
            KebabCaseInsensitiveMap<String, String> responseHeaders =
                    new KebabCaseInsensitiveMap<>(pre.getResponseHeaders());
            values.setCacheControl(responseHeaders.get(Constant.Metadata.CACHE_CONTROL));
            values.setContentDisposition(responseHeaders.get(Constant.Metadata.CONTENT_DISPOSITION));
            values.setContentEncoding(responseHeaders.get(Constant.Metadata.CONTENT_ENCODING));
            values.setContentLanguage(responseHeaders.get(Constant.Metadata.CONTENT_LANGUAGE));
            values.setContentType(responseHeaders.get(Constant.Metadata.CONTENT_TYPE));
            if (pre.getStartTime() != null)
                values.setStartTime(
                        pre.getStartTime().toInstant().atZone(ZoneOffset.UTC).toOffsetDateTime());

            Map<String, String> queryParams = pre.getQueryParams();
            if (CollUtil.isNotEmpty(queryParams)) {
                if (queryParams.get("protocol") != null)
                    values.setProtocol(SasProtocol.parse(queryParams.get("protocol")));
                if (queryParams.get("sasIpRange") != null)
                    values.setSasIpRange(SasIpRange.parse(queryParams.get("sasIpRange")));
                try {
                    values.setSnapshotId(queryParams.get("snapshotId"));
                } catch (Exception ignored) {
                }
                values.setIdentifier(queryParams.get("identifier"));
                values.setPreauthorizedAgentObjectId(queryParams.get("preauthorizedAgentObjectId"));
                values.setCorrelationId(queryParams.get("correlationId"));
            }

            StringBuilder url = new StringBuilder();
            url.append(blobClient.getBlobUrl()).append("?");
            if (CollUtil.isNotEmpty(queryParams)) {
                url.append(UrlQuery.of(queryParams).build(StandardCharsets.UTF_8))
                        .append("&");
            }
            url.append(blobClient.generateSas(values));

            GeneratePresignedUrlResult result = new GeneratePresignedUrlResult(platform, basePath, pre);
            result.setUrl(url.toString());
            Map<String, String> resHeaders = new HashMap<>(headers);
            resHeaders.put("x-ms-blob-type", "BlockBlob");
            result.setHeaders(resHeaders);
            return result;
        } catch (Exception e) {
            throw ExceptionFactory.generatePresignedUrl(pre, e);
        }
    }

    @Override
    public boolean isSupportSameCopy() {
        return true;
    }

    /**
     * 等待复制完成并处理复制结果
     */
    public void awaitCopy(SyncPoller<BlobCopyInfo, Void> copySyncPoller) {
        while (true) {
            PollResponse<BlobCopyInfo> copyInfo = copySyncPoller.poll();
            CopyStatusType copyStatus = copyInfo.getValue().getCopyStatus();
            if (copyStatus == CopyStatusType.PENDING) continue;
            else if (copyStatus == CopyStatusType.SUCCESS) break;
            else throw new RuntimeException(copyStatus.toString());
        }
    }

    @Override
    public void sameCopy(FileInfo srcFileInfo, FileInfo destFileInfo, CopyPretreatment pre) {
        Check.sameCopyNotSupportAcl(platform, srcFileInfo, destFileInfo, pre);
        Check.sameCopyBasePath(platform, basePath, srcFileInfo, destFileInfo);
        // 获取远程文件信息
        String destFileKey = getFileKey(destFileInfo);
        String destThFileKey = getThFileKey(destFileInfo);
        BlobClient srcClient = getBlobClient(getFileKey(srcFileInfo));
        BlobClient destClient = getBlobClient(destFileKey);
        BlobClient srcThClient = getBlobClient(getThFileKey(srcFileInfo));
        BlobClient destThClient = getBlobClient(destThFileKey);
        if (!Boolean.TRUE.equals(srcClient.exists())) {
            throw ExceptionFactory.sameCopyNotFound(srcFileInfo, destFileInfo, platform, null);
        }
        // 复制缩略图文件
        if (destThClient != null) {
            destFileInfo.setThUrl(getUrl(destThFileKey));
            try {
                awaitCopy(destThClient.beginCopy(srcThClient.getBlobUrl(), Duration.ofSeconds(1)));
                setFileAcl(destThFileKey, getAcl(srcFileInfo.getThFileAcl()));
            } catch (Exception e) {
                try {
                    destThClient.deleteIfExists();
                } catch (Exception ignored) {
                }
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 复制文件
        destFileInfo.setUrl(getUrl(destFileKey));
        try {
            long size = srcClient.getProperties().getBlobSize();
            ProgressListener.quickStart(pre.getProgressListener(), size);
            awaitCopy(destClient.beginCopy(srcClient.getBlobUrl(), Duration.ofSeconds(1)));
            setFileAcl(destFileKey, getAcl(srcFileInfo.getFileAcl()));
            ProgressListener.quickFinish(pre.getProgressListener(), size);
        } catch (Exception e) {
            if (destThClient != null)
                try {
                    destThClient.deleteIfExists();
                } catch (Exception ignored) {
                }
            try {
                destClient.deleteIfExists();
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameCopy(srcFileInfo, destFileInfo, platform, e);
        }
    }

    /**
     * 构建sas参数,设置操作权限和过期时间
     */
    private BlobServiceSasSignatureValues getBlobServiceSasSignatureValues(Date expiration) {
        // 设置只读权限
        BlobSasPermission blobPermission = new BlobSasPermission().setReadPermission(true);
        OffsetDateTime offsetDateTime =
                expiration.toInstant().atZone(ZoneOffset.UTC).toOffsetDateTime();

        // 生成签名
        return new BlobServiceSasSignatureValues(offsetDateTime, blobPermission);
    }

    @Override
    public void close() {
        clientFactory.close();
    }

    @Data
    public static class AclWrapper {
        private List<PathAccessControlEntry> aclList;
        private PathPermissions permissions;

        public AclWrapper(List<PathAccessControlEntry> aclList) {
            this.aclList = aclList;
        }

        public AclWrapper(PathPermissions permissions) {
            this.permissions = permissions;
        }
    }
}
