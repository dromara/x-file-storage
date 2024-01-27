package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ftp.Ftp;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.FtpConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.get.*;
import org.dromara.x.file.storage.core.move.MovePretreatment;

/**
 * FTP 存储
 */
@Getter
@Setter
@NoArgsConstructor
public class FtpFileStorage implements FileStorage {
    private String platform;
    private String domain;
    private String basePath;
    private String storagePath;
    private FileStorageClientFactory<Ftp> clientFactory;

    public FtpFileStorage(FtpConfig config, FileStorageClientFactory<Ftp> clientFactory) {
        platform = config.getPlatform();
        domain = config.getDomain();
        basePath = config.getBasePath();
        storagePath = config.getStoragePath();
        this.clientFactory = clientFactory;
    }

    /**
     * 获取 Client ，使用完后需要归还
     */
    public Ftp getClient() {
        return clientFactory.getClient();
    }

    /**
     * 归还 Client
     */
    public void returnClient(Ftp client) {
        clientFactory.returnClient(client);
    }

    @Override
    public void close() {
        clientFactory.close();
    }

    /**
     * 获取远程绝对路径
     */
    public String getAbsolutePath(String path) {
        return storagePath + path;
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        Check.uploadNotSupportAcl(platform, fileInfo, pre);
        Check.uploadNotSupportMetadata(platform, fileInfo, pre);

        Ftp client = getClient();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            client.upload(getAbsolutePath(basePath + fileInfo.getPath()), fileInfo.getFilename(), in);
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                client.upload(
                        getAbsolutePath(basePath + fileInfo.getPath()),
                        fileInfo.getThFilename(),
                        new ByteArrayInputStream(thumbnailBytes));
            }

            return true;
        } catch (Exception e) {
            try {
                client.delFile(getAbsolutePath(newFileKey));
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.upload(fileInfo, platform, e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public ListFilesSupportInfo isSupportListFiles() {
        return ListFilesSupportInfo.supportAll().setSupportMaxFiles(Integer.MAX_VALUE);
    }

    @Override
    public ListFilesResult listFiles(ListFilesPretreatment pre) {
        Ftp client = getClient();
        try {
            String path = getAbsolutePath(basePath + pre.getPath());
            List<FTPFile> fileList = Arrays.stream(client.isDir(path) ? client.lsFiles(path) : new FTPFile[0])
                    .filter(f -> f.isFile() || f.isDirectory())
                    .filter(f -> !(".".equals(f.getName()) || "..".equals(f.getName())))
                    .collect(Collectors.toList());
            ListFilesMatchResult<FTPFile> matchResult = listFilesMatch(fileList, FTPFile::getName, pre, true);
            ListFilesResult list = new ListFilesResult();
            list.setDirList(matchResult.getList().stream()
                    .filter(FTPFile::isDirectory)
                    .map(p -> {
                        RemoteDirInfo dir = new RemoteDirInfo();
                        dir.setPlatform(pre.getPlatform());
                        dir.setBasePath(basePath);
                        dir.setPath(pre.getPath());
                        dir.setName(p.getName());
                        return dir;
                    })
                    .collect(Collectors.toList()));
            list.setFileList(matchResult.getList().stream()
                    .filter(FTPFile::isFile)
                    .map(p -> {
                        RemoteFileInfo remoteFileInfo = new RemoteFileInfo();
                        remoteFileInfo.setPlatform(pre.getPlatform());
                        remoteFileInfo.setBasePath(basePath);
                        remoteFileInfo.setPath(pre.getPath());
                        remoteFileInfo.setFilename(p.getName());
                        remoteFileInfo.setSize(p.getSize());
                        remoteFileInfo.setExt(FileNameUtil.extName(remoteFileInfo.getFilename()));
                        remoteFileInfo.setLastModified(DateUtil.date(p.getTimestamp()));
                        remoteFileInfo.setOriginal(p);
                        return remoteFileInfo;
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
        } finally {
            returnClient(client);
        }
    }

    @Override
    public RemoteFileInfo getFile(GetFilePretreatment pre) {
        Ftp client = getClient();
        try {
            String path = getAbsolutePath(basePath + pre.getPath());
            FTPFile file;
            try {
                client.cd(path);
                file = client.getClient().listFiles(pre.getFilename())[0];
            } catch (Exception e) {
                return null;
            }

            RemoteFileInfo info = new RemoteFileInfo();
            info.setPlatform(pre.getPlatform());
            info.setBasePath(basePath);
            info.setPath(pre.getPath());
            info.setFilename(file.getName());
            info.setSize(file.getSize());
            info.setExt(FileNameUtil.extName(info.getFilename()));
            info.setLastModified(DateUtil.date(file.getTimestamp()));
            info.setOriginal(file);
            return info;
        } catch (Exception e) {
            throw ExceptionFactory.getFile(pre, basePath, e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        Ftp client = getClient();
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                client.delFile(getAbsolutePath(getThFileKey(fileInfo)));
            }
            client.delFile(getAbsolutePath(getFileKey(fileInfo)));
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        Ftp client = getClient();
        try {
            client.cd(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath()));
            return client.existFile(fileInfo.getFilename());
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Ftp client = getClient();
        try {
            client.cd(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath()));
            FTPClient ftpClient = client.getClient();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            try (InputStream in = ftpClient.retrieveFileStream(fileInfo.getFilename())) {
                if (in == null) {
                    throw ExceptionFactory.download(fileInfo, platform, null);
                }
                consumer.accept(in);
                ftpClient.completePendingCommand();
            }
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);

        Ftp client = getClient();
        try {
            client.cd(getAbsolutePath(fileInfo.getBasePath() + fileInfo.getPath()));
            FTPClient ftpClient = client.getClient();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            try (InputStream in = ftpClient.retrieveFileStream(fileInfo.getThFilename())) {
                if (in == null) {
                    throw ExceptionFactory.downloadTh(fileInfo, platform, null);
                }
                consumer.accept(in);
                ftpClient.completePendingCommand();
            }
        } catch (Exception e) {
            throw ExceptionFactory.downloadTh(fileInfo, platform, e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public boolean isSupportSameMove() {
        return true;
    }

    @Override
    public void sameMove(FileInfo srcFileInfo, FileInfo destFileInfo, MovePretreatment pre) {
        Check.sameMoveNotSupportAcl(platform, srcFileInfo, destFileInfo, pre);
        Check.sameMoveNotSupportMetadata(platform, srcFileInfo, destFileInfo, pre);
        Check.sameMoveBasePath(platform, basePath, srcFileInfo, destFileInfo);

        String srcPath = getAbsolutePath(srcFileInfo.getBasePath() + srcFileInfo.getPath());
        String destPath = getAbsolutePath(destFileInfo.getBasePath() + destFileInfo.getPath());
        String relativizePath =
                Paths.get(srcPath).relativize(Paths.get(destPath)).toString().replace("\\", "/") + "/";

        Ftp client = getClient();
        try {
            FTPClient ftpClient = client.getClient();
            client.cd(srcPath);

            FTPFile srcFile;
            try {
                srcFile = ftpClient.listFiles(srcFileInfo.getFilename())[0];
            } catch (Exception e) {
                throw ExceptionFactory.sameMoveNotFound(srcFileInfo, destFileInfo, platform, e);
            }

            // 移动缩略图文件
            String destThFileRelativizeKey = null;
            if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
                destFileInfo.setThUrl(domain + getThFileKey(destFileInfo));
                destThFileRelativizeKey = relativizePath + destFileInfo.getThFilename();
                try {
                    client.mkDirs(destPath);
                    ftpClient.rename(srcFileInfo.getThFilename(), destThFileRelativizeKey);
                } catch (Exception e) {
                    throw ExceptionFactory.sameMoveTh(srcFileInfo, destFileInfo, platform, e);
                }
            }

            // 移动文件
            String destFileKey = getFileKey(destFileInfo);
            destFileInfo.setUrl(domain + destFileKey);
            String destFileRelativizeKey = relativizePath + destFileInfo.getFilename();
            try {
                ProgressListener.quickStart(pre.getProgressListener(), srcFile.getSize());
                ftpClient.rename(srcFileInfo.getFilename(), destFileRelativizeKey);
                ProgressListener.quickFinish(pre.getProgressListener(), srcFile.getSize());
            } catch (Exception e) {
                if (destThFileRelativizeKey != null) {
                    try {
                        ftpClient.rename(destThFileRelativizeKey, srcFileInfo.getThFilename());
                    } catch (Exception ignored) {
                    }
                }
                try {
                    if (client.existFile(srcFileInfo.getFilename())) {
                        client.delFile(destFileRelativizeKey);
                    } else {
                        ftpClient.rename(destFileRelativizeKey, srcFileInfo.getFilename());
                    }
                } catch (Exception ignored) {
                }
                throw ExceptionFactory.sameMove(srcFileInfo, destFileInfo, platform, e);
            }
        } finally {
            returnClient(client);
        }
    }
}
