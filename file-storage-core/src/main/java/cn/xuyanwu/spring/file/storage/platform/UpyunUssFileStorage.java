package cn.xuyanwu.spring.file.storage.platform;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.FileStorageProperties.UpyunUssConfig;
import cn.xuyanwu.spring.file.storage.ProgressInputStream;
import cn.xuyanwu.spring.file.storage.ProgressListener;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import com.upyun.RestManager;
import com.upyun.UpException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * 又拍云 USS 存储
 */
@Getter
@Setter
@NoArgsConstructor
public class UpyunUssFileStorage implements FileStorage {
    private String platform;
    private String domain;
    private String basePath;
    private FileStorageClientFactory<RestManager> clientFactory;


    public UpyunUssFileStorage(UpyunUssConfig config,FileStorageClientFactory<RestManager> clientFactory) {
        platform = config.getPlatform();
        domain = config.getDomain();
        basePath = config.getBasePath();
        this.clientFactory = clientFactory;
    }

    public RestManager getClient() {
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

    @Override
    public boolean save(FileInfo fileInfo,UploadPretreatment pre) {
        fileInfo.setBasePath(basePath);
        String newFileKey = getFileKey(fileInfo);
        fileInfo.setUrl(domain + newFileKey);
        if (fileInfo.getFileAcl() != null) {
            throw new FileStorageRuntimeException("文件上传失败，七牛云 Kodo 不支持设置 ACL！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename());
        }
        ProgressListener listener = pre.getProgressListener();

        RestManager manager = getClient();
        try (InputStream in = pre.getFileWrapper().getInputStream()) {
            //又拍云 USS 的 SDK 使用的是 REST API ，看文档不是区分大小文件的，测试大文件也是流式传输的，边读边传，不会占用大量内存
            HashMap<String,String> params = new HashMap<>();
            params.put(RestManager.PARAMS.CONTENT_TYPE.getValue(),fileInfo.getContentType());
            params.put("Content-Length",String.valueOf(fileInfo.getSize()));
            try (Response result = manager.writeFile(newFileKey,
                    listener == null ? in : new ProgressInputStream(in,listener,fileInfo.getSize()),
                    params)) {
                if (!result.isSuccessful()) {
                    throw new UpException(result.toString());
                }
            }

            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) { //上传缩略图
                String newThFileKey = getThFileKey(fileInfo);
                fileInfo.setThUrl(domain + newThFileKey);
                HashMap<String,String> thParams = new HashMap<>();
                thParams.put(RestManager.PARAMS.CONTENT_TYPE.getValue(),fileInfo.getThContentType());
                Response thResult = manager.writeFile(newThFileKey,new ByteArrayInputStream(thumbnailBytes),thParams);
                if (!thResult.isSuccessful()) {
                    throw new UpException(thResult.toString());
                }
            }

            return true;
        } catch (IOException | UpException e) {
            try {
                manager.deleteFile(newFileKey,null).close();
            } catch (IOException | UpException ignored) {
            }
            throw new FileStorageRuntimeException("文件上传失败！platform：" + platform + "，filename：" + fileInfo.getOriginalFilename(),e);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        RestManager manager = getClient();
        String file = getFileKey(fileInfo);
        String thFile = getThFileKey(fileInfo);

        try (Response ignored = fileInfo.getThFilename() != null ? manager.deleteFile(thFile,null) : null;
             Response ignored2 = manager.deleteFile(file,null)) {
            return true;
        } catch (IOException | UpException e) {
            throw new FileStorageRuntimeException("文件删除失败！fileInfo：" + fileInfo,e);
        }
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        try (Response response = getClient().getFileInfo(getFileKey(fileInfo))) {
            return StrUtil.isNotBlank(response.header("x-upyun-file-size"));
        } catch (IOException | UpException e) {
            throw new FileStorageRuntimeException("判断文件是否存在失败！fileInfo：" + fileInfo,e);
        }
    }

    @Override
    public void download(FileInfo fileInfo,Consumer<InputStream> consumer) {
        try (Response response = getClient().readFile(getFileKey(fileInfo));
             ResponseBody body = response.body();
             InputStream in = body == null ? null : body.byteStream()) {
            if (body == null) {
                throw new FileStorageRuntimeException("文件下载失败，结果为 null ！fileInfo：" + fileInfo);
            }
            if (!response.isSuccessful()) {
                throw new UpException(IoUtil.read(in,StandardCharsets.UTF_8));
            }
            consumer.accept(in);
        } catch (IOException | UpException e) {
            throw new FileStorageRuntimeException("文件下载失败！fileInfo：" + fileInfo,e);
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo,Consumer<InputStream> consumer) {
        if (StrUtil.isBlank(fileInfo.getThFilename())) {
            throw new FileStorageRuntimeException("缩略图文件下载失败，文件不存在！fileInfo：" + fileInfo);
        }
        try (Response response = getClient().readFile(getThFileKey(fileInfo));
             ResponseBody body = response.body();
             InputStream in = body == null ? null : body.byteStream()) {
            if (body == null) {
                throw new FileStorageRuntimeException("缩略图文件下载失败，结果为 null ！fileInfo：" + fileInfo);
            }
            if (!response.isSuccessful()) {
                throw new UpException(IoUtil.read(in,StandardCharsets.UTF_8));
            }
            consumer.accept(in);
        } catch (IOException | UpException e) {
            throw new FileStorageRuntimeException("缩略图文件下载失败！fileInfo：" + fileInfo,e);
        }
    }
}
