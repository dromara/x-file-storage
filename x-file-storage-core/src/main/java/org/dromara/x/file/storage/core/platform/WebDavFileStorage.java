package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.StrUtil;
import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.WebDavConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
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
        if (fileInfo.getFileAcl() != null && pre.getNotSupportAclThrowException()) {
            throw new FileStorageRuntimeException("文件上传失败，WebDAV 不支持设置 ACL！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename());
        }
        if (CollUtil.isNotEmpty(fileInfo.getUserMetadata()) && pre.getNotSupportMetadataThrowException()) {
            throw new FileStorageRuntimeException("文件上传失败，WebDAV 不支持设置 Metadata！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename());
        }

        Sardine client = getClient();
        try (InputStreamPlus in = pre.getInputStreamPlus()) {
            createDirectory(client,getUrl(fileInfo.getBasePath() + fileInfo.getPath()));
            client.put(getUrl(newFileKey),in,fileInfo.getContentType(),true,fileInfo.getSize() == null ? -1 : fileInfo.getSize());
            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());

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

    @Override
    public boolean isSupportCopy() {
        return true;
    }

    @Override
    public void copy(FileInfo srcFileInfo,FileInfo destFileInfo,ProgressListener progressListener) {
        if (!basePath.equals(srcFileInfo.getBasePath())) {
            throw new FileStorageRuntimeException("文件复制失败，源文件 basePath 与当前存储平台 " + platform + " 的 basePath " + basePath + " 不同！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
        }

        Sardine client = getClient();
        String srcFileUrl = getUrl(getFileKey(srcFileInfo));
        try {
            if (!client.exists(srcFileUrl)) {
                throw new FileStorageRuntimeException("文件复制失败，源文件不存在！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo);
            }
        } catch (IOException e) {
            throw new FileStorageRuntimeException("文件复制失败，检查源文件失败！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo,e);
        }
        try {
            createDirectory(client,getUrl(destFileInfo.getBasePath() + destFileInfo.getPath()));
        } catch (IOException e) {
            throw new FileStorageRuntimeException("文件复制失败，检查并创建父路径失败！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo,e);
        }

        //复制缩略图文件
        String destThFileUrl = null;
        if (StrUtil.isNotBlank(srcFileInfo.getThFilename())) {
            String destThFileKey = getThFileKey(destFileInfo);
            destThFileUrl = getUrl(destThFileKey);
            destFileInfo.setThUrl(domain + destThFileKey);
            try {
                client.copy(getUrl(getThFileKey(srcFileInfo)),destThFileUrl);
            } catch (IOException e) {
                throw new FileStorageRuntimeException("缩略图文件复制失败！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo,e);
            }
        }

        //复制文件
        String destFileKey = getFileKey(destFileInfo);
        destFileInfo.setUrl(domain + destFileKey);
        String destFileUrl = getUrl(destFileKey);
        try {
            if (progressListener != null) {
                progressListener.start();
                progressListener.progress(0,srcFileInfo.getSize());
            }
            client.copy(srcFileUrl,destFileUrl);
            if (progressListener != null) {
                Long progressSize = srcFileInfo.getSize();
                if (progressSize == null) {
                    progressSize = client.list(destFileUrl,0,false).get(0).getContentLength();
                }
                progressListener.progress(progressSize,srcFileInfo.getSize());
                progressListener.finish();
            }
        } catch (IOException e) {
            try {
                if (destThFileUrl != null) client.delete(destThFileUrl);
            } catch (IOException ignored) {
            }
            try {
                client.delete(destFileUrl);
            } catch (IOException ignored) {
            }
            throw new FileStorageRuntimeException("文件复制失败！srcFileInfo：" + srcFileInfo + "，destFileInfo：" + destFileInfo,e);
        }
    }
}
