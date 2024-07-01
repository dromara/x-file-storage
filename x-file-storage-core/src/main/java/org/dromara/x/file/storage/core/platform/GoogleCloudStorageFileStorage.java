package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.NamingCase;
import cn.hutool.core.util.*;
import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.*;
import com.google.cloud.storage.Storage.CopyRequest;
import com.google.cloud.storage.Storage.PredefinedAcl;
import com.google.cloud.storage.Storage.SignUrlOption;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
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
import org.dromara.x.file.storage.core.get.*;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlPretreatment;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;
import org.dromara.x.file.storage.core.upload.*;
import org.dromara.x.file.storage.core.util.Tools;

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
    private String bucketName;
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
    public MultipartUploadSupportInfo isSupportMultipartUpload() {
        return MultipartUploadSupportInfo.supportAll().setListPartsSupportMaxParts(10000);
    }

    @Override
    public void initiateMultipartUpload(FileInfo fileInfo, InitiateMultipartUploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        Storage client = getClient();
        try {
            String uploadId = "multi_" + IdUtil.objectId();
            String path = Tools.getParent(newFileKey) + "/" + uploadId + "/";
            ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
            client.createFrom(BlobInfo.newBuilder(bucketName, path + "index").build(), in);
            fileInfo.setUploadId(uploadId);
        } catch (Exception e) {
            throw ExceptionFactory.initiateMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public FilePartInfo uploadPart(UploadPartPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        Storage client = getClient();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            String part = Tools.getParent(newFileKey) + "/" + fileInfo.getUploadId() + "/"
                    + StrUtil.padPre(String.valueOf(pre.getPartNumber()), 10, "0");
            Blob blob = client.createFrom(BlobInfo.newBuilder(bucketName, part).build(), in);
            FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
            filePartInfo.setETag(blob.getEtag());
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
        Storage client = getClient();
        try {
            ProgressListener.quickStart(pre.getProgressListener(), fileInfo.getSize());
            client.delete(BlobId.of(bucketName, newFileKey));
            String path = Tools.getParent(newFileKey) + "/" + fileInfo.getUploadId() + "/";
            LinkedList<String> sources = pre.getPartInfoList().stream()
                    .map(part -> path + StrUtil.padPre(String.valueOf(part.getPartNumber()), 10, "0"))
                    .collect(Collectors.toCollection(LinkedList::new));
            ArrayList<String> subSources = new ArrayList<>();
            for (int i = 0; i < sources.size(); i++) {
                subSources.add(sources.get(i));
                // 每次最多合并 32 个文件，超过部分需要多次合并
                if (subSources.size() == 32 || i + 1 == sources.size()) {
                    Storage.ComposeRequest request;
                    ArrayList<Storage.BlobTargetOption> optionList = null;
                    // 最后一次合并需要传入全部相关数据
                    if (i + 1 == sources.size()) {
                        optionList = new ArrayList<>();
                        BlobInfo.Builder blobInfoBuilder = BlobInfo.newBuilder(bucketName, newFileKey);
                        setMetadataOfBlobTargetOption(blobInfoBuilder, fileInfo, optionList);
                        request = Storage.ComposeRequest.newBuilder()
                                .setTarget(blobInfoBuilder.build())
                                .setTargetOptions(optionList)
                                .addSource(subSources)
                                .build();
                    } else {
                        request = Storage.ComposeRequest.of(bucketName, subSources, newFileKey);
                    }
                    Blob blob = client.compose(request);
                    if (CollUtil.isNotEmpty(optionList)) {
                        blob.update(optionList.toArray(optionList.toArray(new Storage.BlobTargetOption[0])));
                    }
                    ProgressListener.quickProgress(pre.getProgressListener(), blob.getSize(), fileInfo.getSize());
                    subSources.clear();
                    subSources.add(newFileKey);
                }
            }

            List<BlobId> blobIdList =
                    ListUtil.toList(client.list(bucketName, Storage.BlobListOption.prefix(path))
                                    .iterateAll())
                            .stream()
                            .map(BlobInfo::getBlobId)
                            .collect(Collectors.toList());
            if (!blobIdList.isEmpty()) client.delete(blobIdList);
            ProgressListener.quickFinish(pre.getProgressListener());
        } catch (Exception e) {
            try {
                client.delete(BlobId.of(bucketName, newFileKey));
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.completeMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        Storage client = getClient();
        try {
            String path = Tools.getParent(newFileKey) + "/" + fileInfo.getUploadId() + "/";
            List<BlobId> blobIdList =
                    ListUtil.toList(client.list(bucketName, Storage.BlobListOption.prefix(path))
                                    .iterateAll())
                            .stream()
                            .map(BlobInfo::getBlobId)
                            .collect(Collectors.toList());
            if (!blobIdList.isEmpty()) client.delete(blobIdList);
        } catch (Exception e) {
            throw ExceptionFactory.abortMultipartUpload(fileInfo, platform, e);
        }
    }

    @Override
    public FilePartInfoList listParts(ListPartsPretreatment pre) {
        FileInfo fileInfo = pre.getFileInfo();
        String newFileKey = getFileKey(fileInfo);
        Storage client = getClient();
        try {
            String path = Tools.getParent(newFileKey) + "/" + fileInfo.getUploadId() + "/";
            if (client.get(BlobId.of(bucketName, path + "index")) == null) {
                throw new FileNotFoundException(path + "index");
            }

            ArrayList<Storage.BlobListOption> options = new ArrayList<>();
            //            options.add(Storage.BlobListOption.pageSize(pre.getMaxParts()));
            //            if (pre.getPartNumberMarker() != null && pre.getPartNumberMarker() > 0) {
            //                options.add(Storage.BlobListOption.pageToken(String.valueOf(pre.getPartNumberMarker())));
            //            }
            options.add(Storage.BlobListOption.delimiter("/"));
            options.add(Storage.BlobListOption.prefix(path));
            Page<Blob> result = client.list(bucketName, options.toArray(new Storage.BlobListOption[] {}));

            FilePartInfoList list = new FilePartInfoList();
            list.setFileInfo(fileInfo);

            ArrayList<FilePartInfo> partList = new ArrayList<>();
            int i = 1;
            for (Blob p : result.iterateAll()) {
                String filename = FileNameUtil.getName(p.getName());
                int partNumber;
                try {
                    partNumber = Integer.parseInt(filename);
                } catch (Exception e) {
                    continue;
                }
                if (pre.getPartNumberMarker() != null && pre.getPartNumberMarker() > 0) {
                    if (partNumber <= pre.getPartNumberMarker()) continue;
                }
                FilePartInfo filePartInfo = new FilePartInfo(fileInfo);
                filePartInfo.setETag(p.getEtag());
                filePartInfo.setPartNumber(partNumber);
                filePartInfo.setPartSize(p.getSize());
                filePartInfo.setLastModified(DateUtil.date(p.getUpdateTimeOffsetDateTime()));
                partList.add(filePartInfo);
                // 这里多获取一个，用于判断是否还有更多分片待读取
                if (i == pre.getMaxParts() + 1) break;
                i++;
            }

            list.setList(partList);
            list.setMaxParts(pre.getMaxParts());
            list.setIsTruncated(partList.size() > pre.getMaxParts());
            list.setPartNumberMarker(pre.getPartNumberMarker());
            if (list.getIsTruncated()) {
                // 删除最后一个多获取的分片信息
                partList.remove(partList.size() - 1);
                list.setNextPartNumberMarker(partList.get(partList.size() - 1).getPartNumber());
            }
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
        Storage client = getClient();
        try {
            ArrayList<Storage.BlobListOption> options = new ArrayList<>();
            options.add(Storage.BlobListOption.pageSize(pre.getMaxFiles()));
            if (StrUtil.isNotBlank(pre.getMarker())) {
                options.add(Storage.BlobListOption.pageToken(pre.getMarker()));
            }
            options.add(Storage.BlobListOption.delimiter("/"));
            options.add(Storage.BlobListOption.prefix(basePath + pre.getPath() + pre.getFilenamePrefix()));
            Page<Blob> result = client.list(bucketName, options.toArray(new Storage.BlobListOption[] {}));
            ArrayList<Blob> values = ListUtil.toList(result.getValues());
            ListFilesResult list = new ListFilesResult();

            list.setDirList(values.stream()
                    .map(item -> {
                        if (!item.isDirectory()) return null;
                        RemoteDirInfo dir = new RemoteDirInfo();
                        dir.setPlatform(pre.getPlatform());
                        dir.setBasePath(basePath);
                        dir.setPath(pre.getPath());
                        dir.setName(FileNameUtil.getName(item.getName()));
                        dir.setOriginal(item);
                        return dir;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));

            list.setFileList(values.stream()
                    .map(item -> {
                        if (item.isDirectory()) return null;
                        RemoteFileInfo info = new RemoteFileInfo();
                        info.setPlatform(pre.getPlatform());
                        info.setBasePath(basePath);
                        info.setPath(pre.getPath());
                        info.setFilename(FileNameUtil.getName(item.getName()));
                        info.setUrl(domain + getFileKey(new FileInfo(basePath, info.getPath(), info.getFilename())));
                        info.setSize(item.getSize());
                        info.setExt(FileNameUtil.extName(info.getFilename()));
                        info.setETag(item.getEtag());
                        info.setContentDisposition(item.getContentDisposition());
                        info.setContentType(item.getContentType());
                        info.setContentMd5(item.getMd5());
                        info.setLastModified(DateUtil.date(item.getUpdateTimeOffsetDateTime()));
                        HashMap<String, Object> metadata = new HashMap<>();
                        if (item.getContentType() != null)
                            metadata.put(Constant.Metadata.CONTENT_TYPE, item.getContentType());
                        if (item.getContentEncoding() != null)
                            metadata.put(Constant.Metadata.CONTENT_ENCODING, item.getContentEncoding());
                        if (item.getContentDisposition() != null)
                            metadata.put(Constant.Metadata.CONTENT_DISPOSITION, item.getContentDisposition());
                        if (item.getContentLanguage() != null)
                            metadata.put(Constant.Metadata.CONTENT_LANGUAGE, item.getContentLanguage());
                        if (item.getStorageClass() != null) metadata.put("Storage-Class", item.getStorageClass());
                        if (item.getSize() != null) metadata.put(Constant.Metadata.CONTENT_LENGTH, item.getSize());
                        if (item.getMd5() != null) metadata.put(Constant.Metadata.CONTENT_MD5, item.getMd5());
                        if (item.getEtag() != null) metadata.put("E-Tag", item.getEtag());
                        if (item.getUpdateTimeOffsetDateTime() != null)
                            metadata.put(
                                    Constant.Metadata.LAST_MODIFIED,
                                    DateUtil.formatHttpDate(DateUtil.date(item.getUpdateTimeOffsetDateTime())));
                        info.setMetadata(metadata);
                        if (item.getMetadata() != null) info.setUserMetadata(new HashMap<>(item.getMetadata()));
                        info.setOriginal(item);
                        return info;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
            list.setPlatform(pre.getPlatform());
            list.setBasePath(basePath);
            list.setPath(pre.getPath());
            list.setFilenamePrefix(pre.getFilenamePrefix());
            list.setMaxFiles(pre.getMaxFiles());
            list.setIsTruncated(result.hasNextPage());
            list.setMarker(pre.getMarker());
            list.setNextMarker(result.getNextPageToken());
            return list;
        } catch (Exception e) {
            throw ExceptionFactory.listFiles(pre, basePath, e);
        }
    }

    @Override
    public RemoteFileInfo getFile(GetFilePretreatment pre) {
        String fileKey = getFileKey(new FileInfo(basePath, pre.getPath(), pre.getFilename()));
        Storage client = getClient();
        try {
            Blob file;
            try {
                file = client.get(bucketName, fileKey);
            } catch (Exception e) {
                return null;
            }
            if (file == null) return null;
            RemoteFileInfo info = new RemoteFileInfo();
            info.setPlatform(pre.getPlatform());
            info.setBasePath(basePath);
            info.setPath(pre.getPath());
            info.setFilename(FileNameUtil.getName(file.getName()));
            info.setUrl(domain + fileKey);
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

    /**
     * 设置对象的元数据
     */
    public void setMetadataOfBlobTargetOption(
            BlobInfo.Builder blobInfoBuilder, FileInfo fileInfo, ArrayList<Storage.BlobTargetOption> optionList) {
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
                optionList.add(Storage.BlobTargetOption.predefinedAcl(fileAcl.getPredefinedAcl()));
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
    public GeneratePresignedUrlResult generatePresignedUrl(GeneratePresignedUrlPretreatment pre) {
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(
                            bucketName, getFileKey(new FileInfo(basePath, pre.getPath(), pre.getFilename())))
                    .setMetadata(pre.getUserMetadata())
                    .build();
            long duration = pre.getExpiration().getTime() - System.currentTimeMillis();
            ArrayList<SignUrlOption> signUrlOptionList = new ArrayList<>();
            signUrlOptionList.add(SignUrlOption.withV4Signature());
            if (pre.getMethod() instanceof HttpMethod) {
                signUrlOptionList.add(SignUrlOption.httpMethod((HttpMethod) pre.getMethod()));
            } else {
                signUrlOptionList.add(SignUrlOption.httpMethod(
                        HttpMethod.valueOf(String.valueOf(pre.getMethod()).toUpperCase())));
            }
            HashMap<String, String> queryParams = new HashMap<>(pre.getQueryParams());
            pre.getResponseHeaders().forEach((k, v) -> queryParams.put("response-" + k.toLowerCase(), v));
            signUrlOptionList.add(SignUrlOption.withQueryParams(queryParams));
            Map<String, String> headers = new HashMap<>(pre.getHeaders());
            headers.putAll(pre.getUserMetadata().entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().startsWith("x-goog-meta-") ? e.getKey() : "x-goog-meta-" + e.getKey(),
                            Map.Entry::getValue)));
            signUrlOptionList.add(SignUrlOption.withExtHeaders(headers));
            URL url = getClient()
                    .signUrl(
                            blobInfo, duration, TimeUnit.MILLISECONDS, signUrlOptionList.toArray(new SignUrlOption[0]));
            GeneratePresignedUrlResult result = new GeneratePresignedUrlResult(platform, basePath, pre);
            result.setUrl(url.toString());
            result.setHeaders(headers);
            return result;
        } catch (Exception e) {
            throw ExceptionFactory.generatePresignedUrl(pre, e);
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
