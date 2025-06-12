package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.MongoGridFsConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.get.*;
import org.dromara.x.file.storage.core.move.MovePretreatment;
import org.dromara.x.file.storage.core.platform.MongoGridFsFileStorageClientFactory.MongoGridFsClient;
import org.dromara.x.file.storage.core.upload.MultipartUploadSupportInfo;
import org.dromara.x.file.storage.core.util.Tools;

/**
 * Mongo GridFS 存储
 */
@Getter
@Setter
@NoArgsConstructor
public class MongoGridFsFileStorage implements FileStorage {
    private String platform;
    private String domain;
    private String basePath;
    private FileStorageClientFactory<MongoGridFsClient> clientFactory;

    public MongoGridFsFileStorage(MongoGridFsConfig config, FileStorageClientFactory<MongoGridFsClient> clientFactory) {
        platform = config.getPlatform();
        domain = config.getDomain();
        basePath = config.getBasePath();
        this.clientFactory = clientFactory;
    }

    public MongoGridFsClient getClient() {
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
        Document metadata = getObjectMetadata(fileInfo);
        GridFSBucket gridFsBucket = getClient().getGridFsBucket();
        ObjectId objectId = null;
        try (InputStreamPlus in = pre.getInputStreamPlus(false)) {
            GridFSUploadOptions options = new GridFSUploadOptions();
            options.metadata(metadata);
            delete(gridFsBucket, newFileKey);
            objectId = gridFsBucket.uploadFromStream(newFileKey, in, options);
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());
            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                GridFSUploadOptions thOptions = new GridFSUploadOptions();
                thOptions.metadata(getThObjectMetadata(fileInfo));
                delete(gridFsBucket, newThFileKey);
                gridFsBucket.uploadFromStream(newThFileKey, new ByteArrayInputStream(thumbnailBytes), thOptions);
            }
            return true;
        } catch (Exception e) {
            try {
                if (objectId != null) {
                    gridFsBucket.delete(objectId);
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
    public ListFilesSupportInfo isSupportListFiles() {
        return ListFilesSupportInfo.supportAll().setSupportMaxFiles(Integer.MAX_VALUE);
    }

    @Override
    public ListFilesResult listFiles(ListFilesPretreatment pre) {
        GridFSBucket gridFsBucket = getClient().getGridFsBucket();
        try {
            // 因为 Mongo GridFS 没有目录层级关系，这里获取所有路径开头匹配的文件后，进行模拟
            String path = basePath + pre.getPath();
            ArrayList<GridFSFile> queryResult = gridFsBucket
                    .find()
                    .filter(Filters.regex("filename", Pattern.compile("^" + path)))
                    .into(new ArrayList<>());
            List<GridFSFileWrapper> dirList = queryResult.stream()
                    .map(file -> {
                        String substring = file.getFilename().substring(path.length());
                        int index = substring.indexOf("/");
                        return index >= 0 ? new GridFSFileWrapper(substring.substring(0, index)) : null;
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            List<GridFSFileWrapper> fileList = queryResult.stream()
                    .map(file -> file.getFilename().substring(path.length()).contains("/")
                            ? null
                            : new GridFSFileWrapper(file))
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            List<GridFSFileWrapper> allList =
                    Stream.concat(dirList.stream(), fileList.stream()).collect(Collectors.toList());
            ListFilesMatchResult<GridFSFileWrapper> matchResult =
                    listFilesMatch(allList, GridFSFileWrapper::getName, pre, true);

            ListFilesResult list = new ListFilesResult();
            list.setDirList(matchResult.getList().stream()
                    .filter(GridFSFileWrapper::isDir)
                    .map(item -> {
                        RemoteDirInfo dir = new RemoteDirInfo();
                        dir.setPlatform(pre.getPlatform());
                        dir.setBasePath(basePath);
                        dir.setPath(pre.getPath());
                        dir.setName(FileNameUtil.getName(item.name));
                        dir.setOriginal(item.name);
                        return dir;
                    })
                    .collect(Collectors.toList()));

            list.setFileList(matchResult.getList().stream()
                    .filter(item -> !item.isDir())
                    .map(GridFSFileWrapper::getFile)
                    .map(item -> {
                        RemoteFileInfo info = new RemoteFileInfo();
                        info.setPlatform(pre.getPlatform());
                        info.setBasePath(basePath);
                        info.setPath(pre.getPath());
                        info.setFilename(FileNameUtil.getName(item.getFilename()));
                        info.setUrl(domain + getFileKey(new FileInfo(basePath, info.getPath(), info.getFilename())));
                        info.setSize(item.getLength());
                        info.setExt(FileNameUtil.extName(info.getFilename()));
                        info.setLastModified(item.getUploadDate());
                        Document metadata = item.getMetadata();
                        info.setMetadata(Tools.stream(
                                metadata, s -> s.filter(e -> !e.getKey().startsWith("x-amz-meta-"))));
                        info.setUserMetadata(Tools.stream(
                                metadata, s -> s.filter(e -> e.getKey().startsWith("x-amz-meta-")), e -> e.getKey()
                                        .substring(11)));
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
        try {
            GridFSFile file;
            try {
                file = getFile(fileKey);
            } catch (Exception e) {
                return null;
            }
            if (file == null) return null;
            RemoteFileInfo info = new RemoteFileInfo();
            info.setPlatform(pre.getPlatform());
            info.setBasePath(basePath);
            info.setPath(pre.getPath());
            info.setFilename(FileNameUtil.getName(file.getFilename()));
            info.setUrl(domain + fileKey);
            info.setSize(file.getLength());
            info.setExt(FileNameUtil.extName(info.getFilename()));
            info.setLastModified(file.getUploadDate());
            Document metadata = file.getMetadata();
            if (metadata != null) {
                info.setMetadata(
                        Tools.stream(metadata, s -> s.filter(e -> !e.getKey().startsWith("x-amz-meta-"))));
                info.setUserMetadata(
                        Tools.stream(metadata, s -> s.filter(e -> e.getKey().startsWith("x-amz-meta-")), e -> e.getKey()
                                .substring(11)));
            }
            info.setOriginal(file);
            return info;
        } catch (Exception e) {
            throw ExceptionFactory.getFile(pre, basePath, e);
        }
    }

    /**
     * 获取对象的元数据
     * 注意，这里不支持 UserMetadata 用户元数据，所以使用 Amazon S3 的规则进行模拟，所有用户元数据都自动增加 "x-amz-meta-" 前缀
     */
    public Document getObjectMetadata(FileInfo fileInfo) {
        Document metadata = new Document();
        if (fileInfo.getMetadata() != null) metadata.putAll(fileInfo.getMetadata());
        if (fileInfo.getUserMetadata() != null)
            fileInfo.getUserMetadata().forEach((key, value) -> metadata.put("x-amz-meta-" + key, value));
        return metadata;
    }

    /**
     * 获取缩略图对象的元数据
     * 注意，这里不支持 UserMetadata 用户元数据，所以使用 Amazon S3 的规则进行模拟，所有用户元数据都自动增加 "x-amz-meta-" 前缀
     */
    public Document getThObjectMetadata(FileInfo fileInfo) {
        Document metadata = new Document();
        if (fileInfo.getThMetadata() != null) metadata.putAll(fileInfo.getThMetadata());
        if (fileInfo.getThUserMetadata() != null)
            fileInfo.getThUserMetadata().forEach((key, value) -> metadata.put("x-amz-meta-" + key, value));
        return metadata;
    }

    @Override
    public boolean isSupportMetadata() {
        return true;
    }

    /**
     * 删除所有同名文件（Mongo GridFS 支持相同文件名）
     */
    public void delete(GridFSBucket gridFsBucket, String filename) {
        for (GridFSFile file : gridFsBucket.find().filter(Filters.eq("filename", filename))) {
            gridFsBucket.delete(file.getObjectId());
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        GridFSBucket gridFsBucket = getClient().getGridFsBucket();
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                delete(gridFsBucket, getThFileKey(fileInfo));
            }
            delete(gridFsBucket, getFileKey(fileInfo));
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            return getFile(getFileKey(fileInfo)) != null;
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        try (InputStream in = getClient().getGridFsBucket().openDownloadStream(getFileKey(fileInfo))) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);
        try (InputStream in = getClient().getGridFsBucket().openDownloadStream(getThFileKey(fileInfo))) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.downloadTh(fileInfo, platform, e);
        }
    }

    @Override
    public boolean isSupportSameMove() {
        return true;
    }

    @Override
    public void sameMove(FileInfo srcFileInfo, FileInfo destFileInfo, MovePretreatment pre) {
        Check.sameMoveNotSupportAcl(platform, srcFileInfo, destFileInfo, pre);
        Check.sameMoveBasePath(platform, basePath, srcFileInfo, destFileInfo);
        GridFSBucket gridFsBucket = getClient().getGridFsBucket();

        // 获取远程文件信息
        String srcFileKey = getFileKey(srcFileInfo);
        GridFSFile srcFile;
        try {
            srcFile = getFile(gridFsBucket, srcFileKey);
            if (srcFile == null) {
                throw ExceptionFactory.sameMoveNotFound(srcFileInfo, destFileInfo, platform, null);
            }
        } catch (Exception e) {
            throw ExceptionFactory.sameMoveNotFound(srcFileInfo, destFileInfo, platform, e);
        }

        // 移动缩略图文件
        String srcThFileKey = null;
        String destThFileKey = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            srcThFileKey = getThFileKey(srcFileInfo);
            destThFileKey = getThFileKey(destFileInfo);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                GridFSFile srcThFile = getFile(gridFsBucket, srcThFileKey);
                if (srcThFile == null) throw new FileNotFoundException(srcThFileKey);
                delete(gridFsBucket, destThFileKey);
                gridFsBucket.rename(srcThFile.getObjectId(), destThFileKey);
            } catch (Exception e) {
                throw ExceptionFactory.sameMoveTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 移动文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        try {
            delete(gridFsBucket, destFileKey);
            ProgressListener.quickStart(pre.getProgressListener(), srcFile.getLength());
            gridFsBucket.rename(srcFile.getObjectId(), destFileKey);
            ProgressListener.quickFinish(pre.getProgressListener(), srcFile.getLength());
        } catch (Exception e) {
            if (destThFileKey != null)
                try {
                    GridFSFile destThFile = getFile(gridFsBucket, destThFileKey);
                    if (destThFile == null) throw new FileNotFoundException(srcThFileKey);
                    delete(gridFsBucket, srcThFileKey);
                    gridFsBucket.rename(destThFile.getObjectId(), srcThFileKey);
                } catch (Exception ignored) {
                }
            try {
                GridFSFile destFile = getFile(gridFsBucket, destFileKey);
                if (destFile != null) {
                    srcFile = getFile(gridFsBucket, srcFileKey);
                    if (srcFile != null) {
                        gridFsBucket.delete(destFile.getObjectId());
                    } else {
                        delete(gridFsBucket, srcFileKey);
                        gridFsBucket.rename(destFile.getObjectId(), srcFileKey);
                    }
                }
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameMove(srcFileInfo, destFileInfo, platform, e);
        }
    }

    /**
     * 获取文件
     */
    public GridFSFile getFile(String filename) {
        return getFile(getClient().getGridFsBucket(), filename);
    }

    /**
     * 获取文件
     */
    public GridFSFile getFile(GridFSBucket gridFsBucket, String filename) {
        return gridFsBucket.find().filter(Filters.eq("filename", filename)).first();
    }

    /**
     *  GridFSFile 包装类，主要用户同时兼容目录及文件
     */
    @Data
    @Accessors(chain = true)
    public static class GridFSFileWrapper {
        private boolean isDir;
        private GridFSFile file;
        private String name;

        public GridFSFileWrapper(GridFSFile file) {
            this.isDir = false;
            this.file = file;
            this.name = FileNameUtil.getName(file.getFilename());
        }

        public GridFSFileWrapper(String name) {
            this.isDir = true;
            this.name = name;
        }
    }
}
