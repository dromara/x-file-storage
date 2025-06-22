package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.InputStreamResource;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties;
import org.dromara.x.file.storage.core.util.Tools;

/**
 * 七牛云 Kodo 存储平台的 Client 工厂
 */
@Getter
@Setter
@NoArgsConstructor
public class GoFastDfsFileStorageClientFactory
        implements FileStorageClientFactory<GoFastDfsFileStorageClientFactory.GoFastDfsClient> {
    private String platform;
    /**
     * 服务器地址
     */
    private String server;

    /**
     * 服务器组名，用于拼接url
     */
    private String group;
    /**
     * 服务器场景
     */
    private String scene;
    /**
     * 超时时间
     */
    private Integer timeout;

    private String domain;
    private String basePath;
    private Map<String, Object> attr;
    private volatile GoFastDfsClient client;

    public GoFastDfsFileStorageClientFactory(FileStorageProperties.GoFastDfsConfig config) {
        this.platform = config.getPlatform();
        this.server = config.getServer();
        this.group = config.getGroup();
        this.scene = config.getScene();
        this.timeout = config.getTimeout();
        this.domain = config.getDomain();
        this.attr = config.getAttr();
        this.basePath = config.getBasePath();
    }

    @Override
    public GoFastDfsClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = new GoFastDfsClient(server, group, scene, basePath, timeout);
                }
            }
        }
        return client;
    }

    @Override
    public void close() {
        client = null;
    }

    @Getter
    @Setter
    public static class GoFastDfsClient {
        private String server;
        private String group;
        private String scene;
        private String basePath;
        private Integer timeout;

        public GoFastDfsClient(String server, String group, String scene, String basePath, Integer timeout) {
            this.server = server;
            this.group = group;
            this.scene = scene;
            this.basePath = basePath;
            this.timeout = timeout;
        }

        /**
         * 上传文件
         */
        public UploadFileResult uploadFile(String fileKey, InputStream inputStream) {
            String filename = FileUtil.getName(fileKey);
            String path = Tools.getParent(fileKey);

            FileInfo fileInfo = new FileInfo();
            fileInfo.setFilename(fileKey);

            Map<String, Object> data = new HashMap<>();
            data.put("file", new InputStreamResource(inputStream, filename));
            data.put("path", path);
            data.put("filename", filename);
            data.put("scene", scene);
            data.put("output", "json");
            UploadFileResult result = send(Tools.join(server, group, "upload"), data, UploadFileResult.class);

            if (StrUtil.isBlank(result.getPath())) {
                throw new GoFastDfsWorkException(result.getRawBody());
            }
            return result;
        }

        /**
         * 删除文件
         */
        public void deleteFile(String fileKey) {
            Map<String, Object> data = new HashMap<>();
            data.put("path", Tools.join("/", group, fileKey));
            DefaultResult<?> result = send(Tools.join(server, group, "delete"), data, DefaultResult.class);

            if ("leveldb: not found".equals(result.getMessage())) {
                return;
            }
            if (!"ok".equals(result.getStatus())) {
                throw new GoFastDfsWorkException(result.getRawBody());
            }
        }

        /**
         * 获取文件信息
         */
        public GetFileInfo getFile(String fileKey) {
            Map<String, Object> data = new HashMap<>();
            data.put("path", Tools.join("/", group, fileKey));
            GetFileInfo result = send(Tools.join(server, group, "get_file_info"), data, GetFileInfo.class);

            if ("leveldb: not found".equals(result.getMessage())) {
                return null;
            }
            if (!"ok".equals(result.getStatus())) {
                throw new GoFastDfsWorkException(result.getRawBody());
            }
            if (result.getData() == null || StrUtil.isBlank(result.getData().getPath())) {
                throw new GoFastDfsWorkException(result.getRawBody());
            }
            return result;
        }

        /**
         * 获取文件列表
         */
        public ListFileInfo listFile(String dir) {
            Map<String, Object> data = new HashMap<>();
            data.put("dir", dir);
            ListFileInfo result = send(Tools.join(server, group, "list_dir"), data, ListFileInfo.class);

            if ("leveldb: not found".equals(result.getMessage())) {
                return null;
            }
            if (!"ok".equals(result.getStatus())) {
                throw new GoFastDfsWorkException(result.getRawBody());
            }
            if (result.getData() == null) {
                throw new GoFastDfsWorkException(result.getRawBody());
            }
            return result;
        }

        /**
         * 下载文件
         */
        public InputStream downloadFile(String fileKey) {
            String url = Tools.join(server, group, fileKey);
            HttpResponse response = HttpUtil.createPost(url)
                    //                    .form(data)
                    .timeout(timeout)
                    .execute();
            if (!response.isOk()) {
                throw new GoFastDfsHttpException(response.body());
            }
            return response.bodyStream();
        }

        /**
         * 发送请求
         */
        public <T> T send(String url, Map<String, Object> data, Class<T> clazz) {
            String body = null;
            try (HttpResponse response =
                    HttpUtil.createPost(url).form(data).timeout(timeout).execute()) {
                body = response.body();
                if (!response.isOk()) {
                    throw new GoFastDfsHttpException(body);
                }
                T result = JSONUtil.toBean(body, clazz);
                if (result instanceof DefaultResult) {
                    ((DefaultResult<?>) result).setRawBody(body);
                }
                return result;
            } catch (GoFastDfsHttpException e) {
                throw e;
            } catch (Exception e) {
                throw new GoFastDfsWorkException(body, e);
            }
        }

        /**
         * go-fastdfs 中由 HTTP 错误引发的异常
         */
        public static class GoFastDfsHttpException extends RuntimeException {
            public GoFastDfsHttpException(String message) {
                super(message);
            }
        }

        /**
         * go-fastdfs 中由业务错误引发的异常
         */
        public static class GoFastDfsWorkException extends RuntimeException {
            public GoFastDfsWorkException(String message) {
                super(message);
            }

            public GoFastDfsWorkException(String message, Throwable cause) {
                super(message, cause);
            }
        }

        @Data
        public static class DefaultResult<T> {
            public String rawBody;
            public T data;
            public String message;
            public String status;
        }

        @Data
        public static class UploadFileResult extends DefaultResult<Object> {
            /**
             * 文件服务器地址
             */
            private String domain;

            /**
             * 文件的md5值
             */
            private String md5;

            /**
             * 媒体时间
             */
            private String mtime;

            /**
             * 文件服务器存放地址
             */
            private String path;

            /**
             * 回调结果
             */
            private Long retcode;

            /**
             * 回调结果信息
             */
            private String retmsg;

            /**
             * 文件服务器的场景
             */
            private String scene;

            /**
             * 文件的大小
             */
            private Long size;

            /**
             * 文件的源地址
             */
            private String src;
            /**
             * 文件查看下载地址
             */
            private String url;
        }

        @Data
        public static class GetFileInfo extends DefaultResult<GetFileInfo.GetFileInfoData> {

            @Data
            public static class GetFileInfoData {
                private String md5;
                private String name;
                private int offset;
                private String path;
                private List<String> peers;
                private String rename;
                private String scene;
                private long size;
                private long timeStamp;
            }
        }

        @Data
        public static class ListFileInfo extends DefaultResult<List<ListFileInfo.ListFileInfoDataItem>> {

            @Data
            public static class ListFileInfoDataItem {
                private Boolean isDir;
                private String md5;
                private Long mtime;
                private String name;
                private String path;
                private Long size;
            }
        }
    }
}
