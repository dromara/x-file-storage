package cn.xuyanwu.spring.file.storage.platform;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.PathUtil;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.github.sardine.impl.SardineException;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/**
 * WebDav 存储
 */
@Getter
@Setter
public class WebDavFileStorage implements FileStorage {

    private String server;
    private String user;
    private String password;
    private String platform;
    private String domain;
    private String basePath;
    private String storagePath;
    private Sardine client;

    /**
     * 不支持单例模式运行，每次使用完了需要销毁
     */
    public Sardine getClient() {
        if (client == null) {
            client = SardineFactory.begin(user,password);
        }
        return client;
    }

    /**
     * 仅在移除这个存储平台时调用
     */
    @SneakyThrows
    @Override
    public void close() {
        if (client != null) {
            client.shutdown();
            client = null;
        }
    }

    /**
     * 获取远程绝对路径
     */
    public String getUrl(String path) {
        return PathUtil.join(server,storagePath + path);
    }

    /**
     * 递归创建目录
     */
    public void createDirectory(Sardine client,String path) throws IOException {
        if (!client.exists(path)) {
            createDirectory(client,PathUtil.join(PathUtil.getParent(path),"/"));
            client.createDirectory(path);
        }
    }

    @Override
    public boolean save(FileInfo fileInfo,UploadPretreatment pre) {
        String path = basePath + fileInfo.getPath();
        String newFileKey = path + fileInfo.getFilename();
        fileInfo.setBasePath(basePath);
        fileInfo.setUrl(domain + newFileKey);

        Sardine client = getClient();
        try (InputStream in = pre.getFileWrapper().getInputStream()) {
            byte[] bytes = IoUtil.readBytes(in);
            createDirectory(client,getUrl(path));
            client.put(getUrl(newFileKey),bytes);

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                String newThFileKey = path + fileInfo.getThFilename();
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
                    client.delete(getUrl(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getThFilename()));
                } catch (SardineException e) {
                    if (e.getStatusCode() != 404) throw e;
                }
            }
            try {
                client.delete(getUrl(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename()));
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
            return getClient().exists(getUrl(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename()));
        } catch (IOException e) {
            throw new FileStorageRuntimeException("查询文件是否存在失败！fileInfo：" + fileInfo,e);
        }
    }

    @Override
    public void download(FileInfo fileInfo,Consumer<InputStream> consumer) {
        try (InputStream in = getClient().get(getUrl(fileInfo.getBasePath() + fileInfo.getPath() + fileInfo.getFilename()))) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("文件下载失败！platform：" + fileInfo,e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo,Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }

        try (InputStream in = getClient().get(getUrl(fileInfo.getBasePath() + fileInfo.getPath()) + fileInfo.getThFilename())) {
            consumer.accept(in);
        } catch (IOException e) {
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo,e);
        }
    }
}
