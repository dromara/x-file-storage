package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.map.MapProxy;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.csource.common.MyException;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.UploadStream;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties.FastDfsConfig;
import org.dromara.x.file.storage.core.InputStreamPlus;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.exception.Check;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.get.GetFilePretreatment;
import org.dromara.x.file.storage.core.get.RemoteFileInfo;
import org.dromara.x.file.storage.core.util.KebabCaseInsensitiveMap;
import org.dromara.x.file.storage.core.util.Tools;

/**
 * FastDFS 存储
 *
 * @author XS <wanghaiqi@beeplay123.com> XuYanwu <1171736840@qq.com>
 * @version 1.0
 * @date 2023/10/19 11:35
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class FastDfsFileStorage implements FileStorage {

    /**
     * 配置
     */
    private FastDfsConfig config;

    /**
     *  Client 工厂
     */
    private FileStorageClientFactory<StorageClient> clientFactory;

    /**
     * 构造方法
     * @param config        {@link FastDfsConfig} 配置
     * @param clientFactory {@link FileStorageClientFactory} Client 工厂
     */
    public FastDfsFileStorage(FastDfsConfig config, FileStorageClientFactory<StorageClient> clientFactory) {
        this.config = config;
        this.clientFactory = clientFactory;
    }

    /**
     * 获取平台
     */
    @Override
    public String getPlatform() {
        return config.getPlatform();
    }

    /**
     * 设置平台
     */
    @Override
    public void setPlatform(String platform) {
        this.config.setPlatform(platform);
    }

    public StorageClient getClient() {
        return clientFactory.getClient();
    }

    /**
     * 保存文件
     * FastDFS比较特殊：
     * 1、不支持指定文件名、路径等。
     * 2、必须传入文件大小，这里如果获取不到，则通过将输入流全部读入内存的方式来获取，FastDFS主要用来存储小文件，所以问题不大。
     */
    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        Check.uploadNotSupportAcl(getPlatform(), fileInfo, pre);
        fileInfo.setBasePath(config.getBasePath());
        FileWrapper fileWrapper = pre.getFileWrapper();
        NameValuePair[] metadata = getObjectMetadata(fileInfo);

        ProgressListener listener = pre.getProgressListener();
        StorageClient client = getClient();
        boolean useMultipartUpload = fileInfo.getSize() == null || fileInfo.getSize() >= config.getMultipartThreshold();
        boolean hasListener = !useMultipartUpload;
        String[] fileUpload = null;
        try (InputStreamPlus in = pre.getInputStreamPlus(hasListener)) {
            if (useMultipartUpload) {
                int i = 0;
                AtomicLong progressSize = new AtomicLong();
                ProgressListener.quickStart(listener, fileInfo.getSize());
                while (true) {
                    byte[] bytes = IoUtil.readBytes(in, config.getMultipartPartSize());
                    if (bytes == null || bytes.length == 0) break;
                    UploadStream uploadStream = new UploadStream(
                            new InputStreamPlus(
                                    new ByteArrayInputStream(bytes),
                                    currentSize -> ProgressListener.quickProgress(
                                            listener, progressSize.get() + currentSize, fileInfo.getSize())),
                            bytes.length);
                    if (++i == 1) {
                        fileUpload = client.upload_appender_file(
                                config.getGroupName(), bytes.length, uploadStream, fileInfo.getExt(), metadata);
                        if (fileUpload == null) {
                            throw new RuntimeException("FastDFS 上传失败");
                        }
                    } else {
                        int code = client.append_file(fileUpload[0], fileUpload[1], bytes.length, uploadStream);
                        if (code != 0) throw new RuntimeException("errno " + code);
                    }

                    progressSize.addAndGet(bytes.length);
                }
                ProgressListener.quickFinish(listener);
            } else {
                Long size = fileWrapper.getSize();
                fileUpload = client.upload_file(
                        config.getGroupName(), size, new UploadStream(in, size), fileInfo.getExt(), metadata);
            }
            if (fileUpload == null) throw new RuntimeException("FastDFS 上传失败");
            setGroupAndFilename(fileInfo, fileUpload);

            if (fileInfo.getSize() == null) fileInfo.setSize(in.getProgressSize());

            // 缩略图（若包含）
            byte[] thumbnailBytes = pre.getThumbnailBytes();
            if (thumbnailBytes != null) {
                String prefixName = null;
                if (fileInfo.getThFilename().startsWith(fileInfo.getFilename())) {
                    try {
                        prefixName = FileNameUtil.mainName(fileInfo.getThFilename()
                                .substring(FileNameUtil.mainName(fileInfo.getFilename())
                                        .length()));
                    } catch (Exception ignored) {
                    }
                }
                if (StrUtil.isBlank(prefixName)) {
                    prefixName = FileNameUtil.extName(fileInfo.getFilename()) + ".th";
                    if (!prefixName.startsWith(".")) prefixName = "." + prefixName;
                }

                // 使用从文件方式保存且保存到同一个 Group 中，这样可以保证缩略图文件路径前半部分与源文件保持一致
                String[] thumbnailUpload = client.upload_file(
                        fileUpload[0],
                        fileUpload[1],
                        prefixName,
                        thumbnailBytes,
                        FileNameUtil.extName(fileInfo.getThFilename()),
                        getThObjectMetadata(fileInfo));

                if (thumbnailUpload == null) throw new RuntimeException("FastDFS 上传失败");
                setThGroupAndFilename(fileInfo, thumbnailUpload);
            }
            return true;
        } catch (Exception e) {
            if (fileUpload != null) {
                try {
                    client.delete_file(fileUpload[0], fileUpload[1]);
                } catch (Exception ignore) {
                }
            }
            throw ExceptionFactory.upload(fileInfo, getPlatform(), e);
        }
    }

    @Override
    public RemoteFileInfo getFile(GetFilePretreatment pre) {
        StorageClient client = getClient();
        try {
            String[] arr = getGroupAndFilename(new FileInfo()
                    .setPath(pre.getPath())
                    .setFilename(pre.getFilename())
                    .setUrl(pre.getUrl()));
            org.csource.fastdfs.FileInfo file;
            try {
                file = client.get_file_info(arr[0], arr[1]);
                if (file == null) return null;
            } catch (Exception e) {
                return null;
            }
            NameValuePair[] metadata = null;
            try {
                metadata = client.get_metadata(arr[0], arr[1]);
            } catch (Exception ignored) {
            }
            if (metadata == null) metadata = new NameValuePair[0];

            KebabCaseInsensitiveMap<String, String> headers = new KebabCaseInsensitiveMap<>(
                    Arrays.stream(metadata).collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue)));
            MapProxy headersProxy = MapProxy.create(headers);

            RemoteFileInfo info = new RemoteFileInfo();
            info.setPlatform(pre.getPlatform());
            info.setBasePath(config.getBasePath());
            setGroupAndFilename(info, arr);
            info.setSize(file.getFileSize());
            info.setExt(FileNameUtil.extName(info.getFilename()));
            info.setContentDisposition(headersProxy.getStr(Constant.Metadata.CONTENT_DISPOSITION));
            info.setContentType(headersProxy.getStr(Constant.Metadata.CONTENT_TYPE));
            info.setContentMd5(headersProxy.getStr(Constant.Metadata.CONTENT_MD5));
            info.setLastModified(file.getCreateTimestamp());
            info.setMetadata(headers.entrySet().stream()
                    .filter(e -> !e.getKey().startsWith("x-amz-meta-"))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            info.setUserMetadata(headers.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("x-amz-meta-"))
                    .collect(Collectors.toMap(e -> e.getKey().substring(11), Map.Entry::getValue)));
            info.setOriginal(new FastDfsFileInfo(file, metadata));
            return info;
        } catch (Exception e) {
            throw ExceptionFactory.getFile(pre, config.getBasePath(), e);
        }
    }

    /**
     * 获取对象的元数据
     * 注意，这里不支持 UserMetadata 用户元数据，所以使用 Amazon S3 的规则进行模拟，所有用户元数据都自动增加 "x-amz-meta-" 前缀
     */
    public NameValuePair[] getObjectMetadata(FileInfo fileInfo) {
        Map<String, String> map = new LinkedHashMap<>();
        if (fileInfo.getMetadata() != null) map.putAll(fileInfo.getMetadata());
        if (fileInfo.getUserMetadata() != null)
            fileInfo.getUserMetadata().forEach((key, value) -> map.put("x-amz-meta-" + key, value));
        return map.entrySet().stream()
                .map(e -> new NameValuePair(e.getKey(), e.getValue()))
                .toArray(NameValuePair[]::new);
    }

    /**
     * 获取缩略图对象的元数据
     * 注意，这里不支持 UserMetadata 用户元数据，所以使用 Amazon S3 的规则进行模拟，所有用户元数据都自动增加 "x-amz-meta-" 前缀
     */
    public NameValuePair[] getThObjectMetadata(FileInfo fileInfo) {
        Map<String, String> map = new LinkedHashMap<>();
        if (fileInfo.getThMetadata() != null) map.putAll(fileInfo.getThMetadata());
        if (fileInfo.getThUserMetadata() != null)
            fileInfo.getThUserMetadata().forEach((key, value) -> map.put("x-amz-meta-" + key, value));
        return map.entrySet().stream()
                .map(e -> new NameValuePair(e.getKey(), e.getValue()))
                .toArray(NameValuePair[]::new);
    }

    /**
     * 删除文件，仅限内部使用
     */
    public void delete(StorageClient client, String group, String filename) throws MyException, IOException {
        int code = client.delete_file(group, filename);
        // 0 成功，2 文件不存在（猜的）
        if (code != 0 && code != 2) throw new RuntimeException("errno " + code);
    }

    /**
     * 删除文件
     */
    @Override
    public boolean delete(FileInfo fileInfo) {
        StorageClient client = getClient();
        try {
            // 删除缩略图
            String[] thArr = getThGroupAndFilename(fileInfo);
            if (thArr != null) {
                delete(client, thArr[0], thArr[1]);
            }

            String[] arr = getGroupAndFilename(fileInfo);
            delete(client, arr[0], arr[1]);
            return true;
        } catch (Exception e) {
            throw ExceptionFactory.delete(fileInfo, getPlatform(), e);
        }
    }

    /**
     * 文件是否存在
     */
    @Override
    public boolean exists(FileInfo fileInfo) {
        try {
            String[] arr = getGroupAndFilename(fileInfo);
            org.csource.fastdfs.FileInfo remoteFileInfo = getClient().get_file_info(arr[0], arr[1]);
            return remoteFileInfo != null;
        } catch (Exception e) {
            throw ExceptionFactory.exists(fileInfo, getPlatform(), e);
        }
    }

    /**
     * 下载文件，仅限内部使用
     */
    public void download(
            StorageClient client,
            String group,
            String filename,
            Consumer<InputStream> consumer,
            Function<Exception, RuntimeException> exceptionConsumer) {
        try (PipedInputStream pis = new PipedInputStream()) {
            PipedOutputStream pos = new PipedOutputStream(pis);
            // 这部分现在是用的 Hutool 的全局线程池，后期可以优化一下，通过独立线程池来实现
            // 后续版本会重构下载功能，到时候也可能会有其它更好的版本
            ThreadUtil.execAsync(() -> {
                try {
                    client.download_file(group, filename, (fileSize, data, bytes) -> {
                        try {
                            pos.write(data, 0, bytes);
                        } catch (Exception e) {
                            throw exceptionConsumer.apply(e);
                        }
                        return 0;
                    });
                } catch (Exception e) {
                    throw exceptionConsumer.apply(e);
                } finally {
                    IoUtil.close(pos);
                }
            });
            consumer.accept(pis);
        } catch (Exception e) {
            throw exceptionConsumer.apply(e);
        }
    }

    /**
     * 下载文件
     */
    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        String[] arr = getGroupAndFilename(fileInfo);
        if (arr == null) {
            throw ExceptionFactory.download(fileInfo, getPlatform(), new NullPointerException());
        }
        download(
                clientFactory.getClient(),
                arr[0],
                arr[1],
                consumer,
                e -> ExceptionFactory.download(fileInfo, getPlatform(), e));
    }

    /**
     * 下载缩略图文件
     */
    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        String[] arr = getThGroupAndFilename(fileInfo);
        if (arr == null) {
            throw ExceptionFactory.downloadThNotFound(fileInfo, getPlatform());
        }
        download(
                clientFactory.getClient(),
                arr[0],
                arr[1],
                consumer,
                e -> ExceptionFactory.downloadTh(fileInfo, getPlatform(), e));
    }

    @Override
    public boolean isSupportMetadata() {
        return true;
    }

    /**
     * 释放相关资源
     */
    @Override
    public void close() {
        clientFactory.close();
    }

    /**
     * 将 FastDFS 返回的 group 和 filename 保存到 FileInfo 文件信息中
     */
    public void setGroupAndFilename(FileInfo fileInfo, String[] arr) {
        fileInfo.setUrl(config.getDomain() + arr[0] + "/" + arr[1]);
        if (config.getRunMod() == FastDfsConfig.RunMod.COVER) {
            Path path = Paths.get(arr[0], arr[1]);
            fileInfo.setPath(path.getParent().toString().replace("\\", "/") + "/");
            fileInfo.setFilename(path.getFileName().toString());
        }
    }

    /**
     * 将 FastDFS 返回的 group 和 filename 保存到 FileInfo 缩略图文件信息中
     */
    public void setThGroupAndFilename(FileInfo fileInfo, String[] arr) {
        fileInfo.setThUrl(config.getDomain() + arr[0] + "/" + arr[1]);
        if (config.getRunMod() == FastDfsConfig.RunMod.COVER) {
            Path path = Paths.get(arr[0], arr[1]);
            fileInfo.setPath(path.getParent().toString().replace("\\", "/") + "/");
            fileInfo.setThFilename(path.getFileName().toString());
        }
    }

    /**
     * 将 FastDFS 返回的 group 和 filename 保存到 RemoteFileInfo 文件信息中
     */
    public void setGroupAndFilename(RemoteFileInfo info, String[] arr) {
        info.setUrl(config.getDomain() + arr[0] + "/" + arr[1]);
        Path path = Paths.get(arr[0], arr[1]);
        info.setPath(path.getParent().toString().replace("\\", "/") + "/");
        info.setFilename(path.getFileName().toString());
    }

    /**
     * 获取文件对应的 FastDFS 支持的 group 和 filename，失败返回 null
     * @param fileInfo 文件信息
     * @return 0：group，1：filename
     */
    public String[] getGroupAndFilename(FileInfo fileInfo) {
        if (config.getRunMod() == FastDfsConfig.RunMod.COVER) {
            String url = config.getDomain()
                    + Tools.getNotNull(fileInfo.getPath(), StrUtil.EMPTY)
                    + Tools.getNotNull(fileInfo.getFilename(), StrUtil.EMPTY);
            return getGroupAndFilenameByUrl(url);
        } else if (config.getRunMod() == FastDfsConfig.RunMod.URL) {
            return getGroupAndFilenameByUrl(fileInfo.getUrl());
        }
        return null;
    }

    /**
     * 获取缩略图文件对应的 FastDFS 支持的 group 和 filename，失败返回 null
     * @param fileInfo 文件信息
     * @return 0：group，1：filename
     */
    public String[] getThGroupAndFilename(FileInfo fileInfo) {
        if (config.getRunMod() == FastDfsConfig.RunMod.COVER) {
            if (StrUtil.isBlank(fileInfo.getThFilename())) return null;
            String url = config.getDomain()
                    + Tools.getNotNull(fileInfo.getPath(), StrUtil.EMPTY)
                    + Tools.getNotNull(fileInfo.getThFilename(), StrUtil.EMPTY);
            return getGroupAndFilenameByUrl(url);
        } else if (config.getRunMod() == FastDfsConfig.RunMod.URL) {
            if (StrUtil.isBlank(fileInfo.getThUrl())) return null;
            return getGroupAndFilenameByUrl(fileInfo.getThUrl());
        }
        return null;
    }

    /**
     * 从 URL 中解析 FastDFS 支持的 group 和 filename，失败返回 null
     * @return 0：group，1：filename
     */
    public String[] getGroupAndFilenameByUrl(String url) {
        if (StrUtil.isBlank(url)) return null;
        if (!url.startsWith(config.getDomain())) return null;
        try {
            String sub = url.substring(config.getDomain().length());
            int index = sub.indexOf("/");
            String group = sub.substring(0, index);
            String filename = sub.substring(index + 1);
            return new String[] {group, filename};
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * FastDFS 的文件信息
     */
    @Data
    @Accessors(chain = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FastDfsFileInfo {
        /**
         * 文件信息
         */
        private org.csource.fastdfs.FileInfo fileInfo;
        /**
         * 元数据
         */
        private NameValuePair[] metadata;
    }
}
