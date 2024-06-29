package org.dromara.x.file.storage.core.platform;

import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.JschRuntimeException;
import cn.hutool.extra.ssh.Sftp;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.SftpConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.get.*;
import org.dromara.x.file.storage.core.move.MovePretreatment;

/**
 * SFTP 存储
 */
@Getter
@Setter
@NoArgsConstructor
public class SftpFileStorage implements FileStorage {
    private String platform;
    private String domain;
    private String basePath;
    private String storagePath;
    private FileStorageClientFactory<Sftp> clientFactory;

    public SftpFileStorage(SftpConfig config, FileStorageClientFactory<Sftp> clientFactory) {
        platform = config.getPlatform();
        domain = config.getDomain();
        basePath = config.getBasePath();
        storagePath = config.getStoragePath();
        this.clientFactory = clientFactory;
    }

    /**
     * 获取 Client ，使用完后需要归还
     */
    public Sftp getClient() {
        return clientFactory.getClient();
    }

    /**
     * 归还 Client
     */
    public void returnClient(Sftp client) {
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

        Sftp client = getClient();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            String path = getAbsolutePath(basePath + fileInfo.getPath());
            if (!client.exist(path)) {
                client.mkDirs(path);
            }
            client.upload(path, fileInfo.getFilename(), in);
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                client.upload(path, fileInfo.getThFilename(), new ByteArrayInputStream(thumbnailBytes));
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
        Sftp client = getClient();
        try {
            String path = getAbsolutePath(basePath + pre.getPath());
            List<LsEntry> fileList = Collections.emptyList();
            if (client.isDir(path)) {
                fileList = client.lsEntries(path).stream()
                        .filter(item ->
                                item.getAttrs().isDir() || item.getAttrs().isReg())
                        .collect(Collectors.toList());
            }
            ListFilesMatchResult<LsEntry> matchResult = listFilesMatch(fileList, LsEntry::getFilename, pre, true);
            ListFilesResult list = new ListFilesResult();
            list.setDirList(matchResult.getList().stream()
                    .filter(item -> item.getAttrs().isDir())
                    .map(item -> {
                        RemoteDirInfo dir = new RemoteDirInfo();
                        dir.setPlatform(pre.getPlatform());
                        dir.setBasePath(basePath);
                        dir.setPath(pre.getPath());
                        dir.setName(item.getFilename());
                        dir.setOriginal(item);
                        return dir;
                    })
                    .collect(Collectors.toList()));
            list.setFileList(matchResult.getList().stream()
                    .filter(item -> item.getAttrs().isReg())
                    .map(item -> {
                        RemoteFileInfo info = new RemoteFileInfo();
                        info.setPlatform(pre.getPlatform());
                        info.setBasePath(basePath);
                        info.setPath(pre.getPath());
                        info.setFilename(item.getFilename());
                        info.setSize(item.getAttrs().getSize());
                        info.setExt(FileNameUtil.extName(info.getFilename()));
                        info.setLastModified(DateUtil.date(item.getAttrs().getMTime() * 1000L));
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
        } finally {
            returnClient(client);
        }
    }

    @Override
    public RemoteFileInfo getFile(GetFilePretreatment pre) {
        String fileKey = getFileKey(new FileInfo(basePath, pre.getPath(), pre.getFilename()));
        Sftp client = getClient();
        try {
            String path = getAbsolutePath(basePath + pre.getPath());
            String filename = pre.getFilename();

            // 这种方式速度慢，列举文件接口返回数据相同
            //            final LsEntry[] files = {null};
            //            if (client.isDir(path)) {
            //                client.getClient().ls(path, entry -> {
            //                    if (pre.getFilename().equals(entry.getFilename())) {
            //                        files[0] = entry;
            //                        return ChannelSftp.LsEntrySelector.BREAK;
            //                    }
            //                    return ChannelSftp.LsEntrySelector.CONTINUE;
            //                });
            //            }
            //            LsEntry file = files[0];
            //            if (file == null) return null;

            // 这种方式速度快，但是 longname 与列举文件方法返回的 longname 不相同，
            // 可以使用列举文件方法代替：fileStorageService.listFiles().setPath("test/").setFilenamePrefix("a.jpg").setMaxFiles(1).listFiles();
            SftpATTRS attrs;
            try {
                attrs = client.getClient().stat(path + filename);
            } catch (Exception e) {
                return null;
            }
            if (attrs == null) return null;
            LsEntry file = ReflectUtil.newInstanceIfPossible(LsEntry.class);
            ReflectUtil.setFieldValue(file, "filename", filename);
            ReflectUtil.setFieldValue(file, "longname", attrs + " " + filename);
            ReflectUtil.setFieldValue(file, "attrs", attrs);

            RemoteFileInfo info = new RemoteFileInfo();
            info.setPlatform(pre.getPlatform());
            info.setBasePath(basePath);
            info.setPath(pre.getPath());
            info.setFilename(file.getFilename());
            info.setUrl(domain + fileKey);
            info.setSize(file.getAttrs().getSize());
            info.setExt(FileNameUtil.extName(info.getFilename()));
            info.setLastModified(DateUtil.date(file.getAttrs().getMTime() * 1000L));
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
        Sftp client = getClient();
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                delFile(client, getAbsolutePath(getThFileKey(fileInfo)));
            }
            delFile(client, getAbsolutePath(getFileKey(fileInfo)));
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        } finally {
            returnClient(client);
        }
    }

    public void delFile(Sftp client, String filename) {
        try {
            client.delFile(filename);
        } catch (JschRuntimeException e) {
            if (!(e.getCause() instanceof SftpException && ((SftpException) e.getCause()).id == SSH_FX_NO_SUCH_FILE)) {
                throw e;
            }
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        Sftp client = getClient();
        try {
            return client.exist(getAbsolutePath(getFileKey(fileInfo)));
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Sftp client = getClient();
        try (InputStream in = client.getClient().get(getAbsolutePath(getFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        } finally {
            returnClient(client);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);

        Sftp client = getClient();
        try (InputStream in = client.getClient().get(getAbsolutePath(getThFileKey(fileInfo)))) {
            consumer.accept(in);
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

        Sftp client = getClient();
        try {
            ChannelSftp ftpClient = client.getClient();
            client.cd(srcPath);

            SftpATTRS srcFile;
            try {
                srcFile = ftpClient.stat(srcFileInfo.getFilename());
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
                    if (client.exist(srcFileInfo.getFilename())) {
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
