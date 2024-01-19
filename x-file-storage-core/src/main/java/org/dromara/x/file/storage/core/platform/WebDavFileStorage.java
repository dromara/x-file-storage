package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.util.StrUtil;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineException;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.WebDavConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.move.MovePretreatment;
import org.dromara.x.file.storage.core.util.Tools;

/**
 * WebDAV 存储
 */
@Getter
@Setter
@NoArgsConstructor
public class WebDavFileStorage implements FileStorage {
    private String platform;
    private String server;
    private String domain;
    private String basePath;
    private String storagePath;
    private FileStorageClientFactory<Sardine> clientFactory;

    public WebDavFileStorage(WebDavConfig config, FileStorageClientFactory<Sardine> clientFactory) {
        platform = config.getPlatform();
        server = config.getServer();
        domain = config.getDomain();
        basePath = config.getBasePath();
        storagePath = config.getStoragePath();
        this.clientFactory = clientFactory;
    }

    public Sardine getClient() {
        return clientFactory.getClient();
    }

    @Override
    public void close() {
        clientFactory.close();
    }

    /**
     * 获取远程绝对路径
     */
    public String getUrl(String path) {
        return Tools.join(server, storagePath + path);
    }

    public boolean existsDirectory(Sardine client, String path) throws IOException {
        if (server.equals(path)) return true;
        try {
            return client.list(path, 0).size() > 0;
        } catch (SardineException e) {
            if (e.getStatusCode() == 404 || e.getStatusCode() == 409) return false;
            throw e;
        }
    }

    /**
     * 递归创建目录
     */
    public void createDirectory(Sardine client, String path) throws IOException {
        if (!existsDirectory(client, path)) {
            createDirectory(client, Tools.join(Tools.getParent(path), "/"));
            client.createDirectory(path);
        }
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        Check.uploadNotSupportAcl(platform, fileInfo, pre);
        Check.uploadNotSupportMetadata(platform, fileInfo, pre);

        Sardine client = getClient();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            createDirectory(client, getUrl(fileInfo.getBasePath() + fileInfo.getPath()));
            client.put(
                    getUrl(newFileKey),
                    in,
                    fileInfo.getContentType(),
                    true,
                    fileInfo.getSize() == null ? -1 : fileInfo.getSize());
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { // 上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                client.put(getUrl(newThFileKey), thumbnailBytes);
            }

            return true;
        } catch (Exception e) {
            try {
                client.delete(getUrl(newFileKey));
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.upload(fileInfo, platform, e);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        Sardine client = getClient();
        try {
            if (fileInfo.getThFilename() != null) { // 删除缩略图
                try {
                    client.delete(getUrl(getThFileKey(fileInfo)));
                } catch (SardineException e) {
                    if (e.getStatusCode() != 404) throw e;
                }
            }
            try {
                client.delete(getUrl(getFileKey(fileInfo)));
            } catch (SardineException e) {
                if (e.getStatusCode() != 404) throw e;
            }
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, platform, e);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            return getClient().exists(getUrl(getFileKey(fileInfo)));
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, platform, e);
        }
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        try (InputStream in = getClient().get(getUrl(getFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (Exception e) {
            throw ExceptionFactory.download(fileInfo, platform, e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        Check.downloadThBlankThFilename(platform, fileInfo);

        try (InputStream in = getClient().get(getUrl(getThFileKey(fileInfo)))) {
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
        Check.sameCopyNotSupportMetadata(platform, srcFileInfo, destFileInfo, pre);
        Check.sameCopyBasePath(platform, basePath, srcFileInfo, destFileInfo);

        Sardine client = getClient();

        // 获取远程文件信息
        String srcFileUrl = getUrl(getFileKey(srcFileInfo));
        DavResource srcFile;
        try {
            srcFile = client.list(srcFileUrl, 0, false).get(0);
        } catch (Exception e) {
            throw ExceptionFactory.sameCopyNotFound(srcFileInfo, destFileInfo, platform, e);
        }

        // 检查并创建父路径
        try {
            createDirectory(client, getUrl(destFileInfo.getBasePath() + destFileInfo.getPath()));
        } catch (Exception e) {
            throw ExceptionFactory.sameCopyCreatePath(srcFileInfo, destFileInfo, platform, e);
        }

        // 复制缩略图文件
        String destThFileUrl = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            String destThFileKey = getThFileKey(destFileInfo);
            destThFileUrl = getUrl(destThFileKey);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                client.copy(getUrl(getThFileKey(srcFileInfo)), destThFileUrl);
            } catch (Exception e) {
                throw ExceptionFactory.sameCopyTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 复制文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        String destFileUrl = getUrl(destFileKey);
        try {
            ProgressListener.quickStart(pre.getProgressListener(), srcFile.getContentLength());
            client.copy(srcFileUrl, destFileUrl);
            ProgressListener.quickFinish(pre.getProgressListener(), srcFile.getContentLength());
        } catch (Exception e) {
            try {
                if (destThFileUrl != null) client.delete(destThFileUrl);
            } catch (Exception ignored) {
            }
            try {
                client.delete(destFileUrl);
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
        Check.sameMoveNotSupportAcl(platform, srcFileInfo, destFileInfo, pre);
        Check.sameMoveNotSupportMetadata(platform, srcFileInfo, destFileInfo, pre);
        Check.sameMoveBasePath(platform, basePath, srcFileInfo, destFileInfo);

        Sardine client = getClient();

        // 获取远程文件信息
        String srcFileUrl = getUrl(getFileKey(srcFileInfo));
        DavResource srcFile;
        try {
            srcFile = client.list(srcFileUrl, 0, false).get(0);
        } catch (Exception e) {
            throw ExceptionFactory.sameMoveNotFound(srcFileInfo, destFileInfo, platform, e);
        }

        // 检查并创建父路径
        try {
            createDirectory(client, getUrl(destFileInfo.getBasePath() + destFileInfo.getPath()));
        } catch (Exception e) {
            throw ExceptionFactory.sameMoveCreatePath(srcFileInfo, destFileInfo, platform, e);
        }

        // 移动缩略图文件
        String srcThFileUrl = null;
        String destThFileUrl = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            srcThFileUrl = getUrl(getThFileKey(srcFileInfo));
            String destThFileKey = getThFileKey(destFileInfo);
            destThFileUrl = getUrl(destThFileKey);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                client.move(srcThFileUrl, destThFileUrl);
            } catch (Exception e) {
                throw ExceptionFactory.sameMoveTh(srcFileInfo, destFileInfo, platform, e);
            }
        }

        // 移动文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        String destFileUrl = getUrl(destFileKey);
        try {
            ProgressListener.quickStart(pre.getProgressListener(), srcFile.getContentLength());
            client.move(srcFileUrl, destFileUrl);
            ProgressListener.quickFinish(pre.getProgressListener(), srcFile.getContentLength());
        } catch (Exception e) {
            try {
                if (destThFileUrl != null) {
                    client.move(destThFileUrl, srcThFileUrl);
                }
            } catch (Exception ignored) {
            }
            try {
                if (client.exists(srcFileUrl)) {
                    client.delete(destFileUrl);
                } else {
                    client.move(destFileUrl, srcFileUrl);
                }
            } catch (Exception ignored) {
            }
            throw ExceptionFactory.sameMove(srcFileInfo, destFileInfo, platform, e);
        }
    }
}
