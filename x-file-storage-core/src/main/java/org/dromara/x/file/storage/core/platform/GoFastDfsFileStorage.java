package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.io.file.FileNameUtil;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.get.*;
import org.dromara.x.file.storage.core.platform.GoFastDfsFileStorageClientFactory.GoFastDfsClient;
import org.dromara.x.file.storage.core.platform.GoFastDfsFileStorageClientFactory.GoFastDfsClient.GetFileInfo.GetFileInfoData;
import org.dromara.x.file.storage.core.platform.GoFastDfsFileStorageClientFactory.GoFastDfsClient.ListFileInfo.ListFileInfoDataItem;

/**
 * @author fengheliang
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class GoFastDfsFileStorage implements FileStorage {
    private String platform;
    private String domain;
    private String basePath;
    private FileStorageClientFactory<GoFastDfsClient> clientFactory;

    public GoFastDfsFileStorage(
            FileStorageProperties.GoFastDfsConfig config, FileStorageClientFactory<GoFastDfsClient> clientFactory) {
        this.platform = config.getPlatform();
        this.domain = config.getDomain();
        this.basePath = config.getBasePath();
        this.clientFactory = clientFactory;
    }

    /**
     * 获取 Client
     */
    public GoFastDfsClient getClient() {
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
        Check.uploadNotSupportMetadata(platform, fileInfo, pre);

        GoFastDfsClient client = getClient();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            client.uploadFile(newFileKey, in);
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                client.uploadFile(newThFileKey, new ByteArrayInputStream(thumbnailBytes));
            }
            return true;
        } catch (Exception e) {
            try {
                client.deleteFile(newFileKey);
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.upload(fileInfo, platform, e);
        }
    }

    @Override
    public ListFilesSupportInfo isSupportListFiles() {
        return ListFilesSupportInfo.supportAll().setSupportMaxFiles(Integer.MAX_VALUE);
    }

    @Override
    public ListFilesResult listFiles(ListFilesPretreatment pre) {
        GoFastDfsClient client = getClient();
        try {
            String path = basePath + pre.getPath();
            List<ListFileInfoDataItem> fileList = client.listFile(path).getData();
            ListFilesMatchResult<ListFileInfoDataItem> matchResult =
                    listFilesMatch(fileList, ListFileInfoDataItem::getName, pre, true);
            ListFilesResult list = new ListFilesResult();
            list.setDirList(matchResult.getList().stream()
                    .filter(ListFileInfoDataItem::getIsDir)
                    .map(item -> {
                        RemoteDirInfo dir = new RemoteDirInfo();
                        dir.setPlatform(pre.getPlatform());
                        dir.setBasePath(basePath);
                        dir.setPath(pre.getPath());
                        dir.setName(item.getName());
                        dir.setOriginal(item);
                        return dir;
                    })
                    .collect(Collectors.toList()));
            list.setFileList(matchResult.getList().stream()
                    .filter(v -> !v.getIsDir())
                    .map(item -> {
                        RemoteFileInfo info = new RemoteFileInfo();
                        info.setPlatform(pre.getPlatform());
                        info.setBasePath(basePath);
                        info.setPath(pre.getPath());
                        info.setFilename(item.getName());
                        info.setUrl(domain + getFileKey(new FileInfo(basePath, info.getPath(), info.getFilename())));
                        info.setSize(item.getSize());
                        info.setExt(FileNameUtil.extName(info.getFilename()));
                        info.setETag(item.getMd5());
                        info.setContentMd5(item.getMd5());
                        info.setLastModified(new Date(item.getMtime() * 1000));
                        info.setOriginal(item);
                        return info;
                    })
                    .collect(Collectors.toList()));
            list.setPlatform(pre.getPlatform());
            list.setBasePath(basePath);
            list.setPath(pre.getPath());
            list.setFilenamePrefix(pre.getFilenamePrefix());
            list.setMaxFiles(pre.getMaxFiles());
            list.setIsTruncated(matchResult.getIsTruncated());
            list.setMarker(pre.getMarker());
            list.setNextMarker(matchResult.getNextMarker());
            return list;
        } catch (Exception e) {
            throw ExceptionFactory.listFiles(pre, basePath, e);
        }
    }

    @Override
    public RemoteFileInfo getFile(GetFilePretreatment pre) {

        String fileKey = getFileKey(new FileInfo(basePath, pre.getPath(), pre.getFilename()));
        GoFastDfsClient client = getClient();
        try {
            GetFileInfoData file;
            try {
                file = client.getFile(fileKey).getData();
            } catch (Exception e) {
                return null;
            }
            if (file == null) return null;
            RemoteFileInfo info = new RemoteFileInfo();
            info.setPlatform(pre.getPlatform());
            info.setBasePath(basePath);
            info.setPath(pre.getPath());
            info.setFilename(FileNameUtil.getName(fileKey));
            info.setUrl(domain + fileKey);
            info.setSize(file.getSize());
            info.setExt(FileNameUtil.extName(info.getFilename()));
            info.setETag(file.getMd5());
            info.setContentMd5(file.getMd5());
            info.setLastModified(new Date(file.getTimeStamp() * 1000));
            info.setOriginal(file);
            return info;
        } catch (Exception e) {
            throw ExceptionFactory.getFile(pre, basePath, e);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        GoFastDfsClient client = getClient();
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                client.deleteFile(getThFileKey(fileInfo));
            }
            client.deleteFile(getFileKey(fileInfo));
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            return getClient().getFile(getFileKey(fileInfo)) != null;
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        try (InputStream in = getClient().downloadFile(getFileKey(fileInfo))) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);

        try (InputStream in = getClient().downloadFile(getThFileKey(fileInfo))) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.downloadTh(fileInfo, platform, e);
        }
    }
}
