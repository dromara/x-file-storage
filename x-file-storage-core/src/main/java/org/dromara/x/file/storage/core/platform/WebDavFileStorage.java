package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.StrUtil;
import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.WebDavConfig;
import org.dromara.x.file.storage.core.ProgressInputStream;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.util.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

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

    public WebDavFileStorage(WebDavConfig config,FileStorageClientFactory<Sardine> clientFactory) {
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

    public String getFileKey(FileInfo fileInfo) {
        return fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename();
    }

    public String getThFileKey(FileInfo fileInfo) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) return null;
        return fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename();
    }

    /**
     * 获取远程绝对路径
     */
    public String getUrl(String path) {
        return Tools.join(server,storagePath + path);
    }

    public boolean existsDirectory(Sardine client,String path) throws IOException {
        if (server.equals(path)) return true;
        try {
            return client.list(path,0).size() > 0;
        } catch (SardineException e) {
            if (e.getStatusCode() == 404 || e.getStatusCode() == 409) return false;
            throw e;
        }
    }

    /**
     * 递归创建目录
     */
    public void createDirectory(Sardine client,String path) throws IOException {
        if (!existsDirectory(client,path)) {
            createDirectory(client,Tools.join(Tools.getParent(path),"/"));
            client.createDirectory(path);
        }
    }

    @Override
    public boolean save(FileInfo fileInfo,UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        if (fileInfo.getFileAcl() != null) {
            throw new FileStorageRuntimeException("文件上传失败，WebDAV 不支持设置 ACL！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename());
        }
        ProgressListener listener = pre.getProgressListener();

        Sardine client = getClient();
        try (InputStream in = pre.getFileWrapper().getInputStream()) {
            createDirectory(client,getUrl(fileInfo.getBasePath() + fileInfo.getPath()));
            client.put(getUrl(newFileKey),
                    listener == null ? in : new ProgressInputStream(in,listener,fileInfo.getSize()),
                    fileInfo.getContentType(),true,fileInfo.getSize());

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                client.put(getUrl(newThFileKey),thumbnailBytes);
            }

            return true;
        } catch (IOException | IORuntimeException e) {
            try {
                client.delete(getUrl(newFileKey));
            } catch (IOException ignored) {
            }
            throw new FileStorageRuntimeException("文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(),e);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        Sardine client = getClient();
        try {
            if (fileInfo.getThFilename() != null) {   //删除缩略图
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
        } catch (IOException e) {
            throw new FileStorageRuntimeException("文件删除失败！fileInfo：" + fileInfo,e);
        }
    }


    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            return getClient().exists(getUrl(getFileKey(fileInfo)));
        } catch (IOException e) {
            throw new FileStorageRuntimeException("查询文件是否存在失败！fileInfo：" + fileInfo,e);
        }
    }

    @Override
    public void download(FileInfo fileInfo,Consumer<InputStream> consumer) {
        try (InputStream in = getClient().get(getUrl(getFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("文件下载失败！fileInfo：" + fileInfo,e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo,Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }

        try (InputStream in = getClient().get(getUrl(getThFileKey(fileInfo)))) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo,e);
        }
    }
}
