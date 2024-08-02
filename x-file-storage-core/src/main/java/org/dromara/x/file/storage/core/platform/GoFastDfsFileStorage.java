package org.dromara.x.file.storage.core.platform;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.io.resource.InputStreamResource;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties;
import org.dromara.x.file.storage.core.UploadPretreatment;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.exception.ExceptionFactory;
import org.dromara.x.file.storage.core.get.*;
import org.dromara.x.file.storage.core.hash.HashInfo;

/**
 * @author fengheliang
 */
@Slf4j
public class GoFastDfsFileStorage implements FileStorage {

    /**
     * 上传文件服务器
     */
    private static final String URL_FILE_UPLOAD = "{}/{}/upload";

    /**
     * 删除文件服务器文件
     */
    private static final String URL_FILE_DELETE = "{}/{}/delete";

    /**
     * 获取文件信息
     */
    private static final String URL_GET_FILE_INFO = "{}/{}/get_file_info";

    /**
     * 获取文件列表
     */
    private static final String URL_LIST_DIR = "{}/{}/list_dir?dir={}";

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
    private Integer timeOut;

    private String domain;

    private String basePath;

    private Map<String, Object> attr;

    /**
     * 请求静态变量 path
     */
    private static final String PARAM_PATH = "path";

    /**
     * 请求静态变量 md5
     */
    private static final String PARAM_MD5 = "md5";

    /**
     * 请求静态变量 scene
     */
    private static final String PARAM_SCENE = "scene";

    /**
     * 请求静态变量 output
     */
    private static final String PARAM_OUTPUT = "output";

    /**
     * 请求静态变量 filename
     */
    private static final String PARAM_FILENAME = "filename";

    /**
     * 请求静态变量 file
     */
    private static final String PARAM_FILE = "file";

    /**
     * 请求静态变量 json
     */
    private static final String PARAM_RESULT_JSON = "json";

    /**
     * 请求静态变量 status
     */
    private static final String PARAM_RESULT_STATUS = "status";

    /**
     * 请求静态变量 message
     */
    private static final String PARAM_RESULT_MESSAGE = "message";

    /**
     * 请求静态变量 fail
     */
    private static final String PARAM_RESULT_FAIL = "fail";

    /**
     * 请求静态变量 data
     */
    private static final String PARAM_RESULT_DATA = "data";

    /**
     *
     */
    private static final String PARAM_RESULT_PERMISSIONS = "Can only be called by the cluster ip or 127.0.0.1";

    public GoFastDfsFileStorage(FileStorageProperties.GoFastDfsConfig goFastDfsConfig) {
        this.platform = goFastDfsConfig.getPlatform();
        this.server = goFastDfsConfig.getServer();
        this.group = goFastDfsConfig.getGroup();
        this.scene = goFastDfsConfig.getScene();
        this.timeOut = goFastDfsConfig.getTimeOut();
        this.domain = goFastDfsConfig.getDomain();
        this.attr = goFastDfsConfig.getAttr();
        this.basePath = goFastDfsConfig.getBasePath();
    }

    @Override
    public String getPlatform() {
        return this.platform;
    }

    @Override
    public void setPlatform(String platform) {
        this.platform = platform;
    }

    /**
     * 上传文件
     *
     * @param inputStream
     * @param fileName
     * @param pre
     * @return
     * @throws IOException
     */
    private UploadFileResult uploadFile(InputStream inputStream, String fileName, UploadPretreatment pre) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFilename(fileName);
        InputStreamResource inputStreamResource = new InputStreamResource(inputStream, fileName);

        Map<String, Object> params = new HashMap<>(10);
        params.put(PARAM_FILE, inputStreamResource);
        if (StrUtil.isNotEmpty(basePath)) {
            params.put(PARAM_PATH, basePath + "/" + pre.getPath());
        } else {
            params.put(PARAM_PATH, pre.getPath());
        }
        if (StrUtil.isNotEmpty(fileName)) {
            params.put(PARAM_FILENAME, fileName);
        }
        params.put(PARAM_SCENE, this.scene);
        params.put(PARAM_OUTPUT, PARAM_RESULT_JSON);
        HttpResponse httpResponse = HttpUtil.createPost(jointPath(URL_FILE_UPLOAD))
                .form(params)
                .timeout(this.timeOut)
                .execute();

        if (!httpResponse.isOk()) {
            ExceptionFactory.upload(fileInfo, platform, new RuntimeException(httpResponse.body()));
            return null;
        }

        JSONObject jsonObject = JSONUtil.parseObj(httpResponse.body());
        if (ObjUtil.isNotEmpty(jsonObject.getStr(PARAM_RESULT_STATUS))) {
            ExceptionFactory.upload(fileInfo, platform, new RuntimeException(jsonObject.getStr(PARAM_RESULT_MESSAGE)));
            return null;
        }

        return JSONUtil.toBean(jsonObject, UploadFileResult.class);
    }

    private void fillFileInfo(UploadFileResult uploadFileResult, FileInfo fileInfo, UploadPretreatment pre) {
        fileInfo.setHashInfo(new HashInfo());

        String md5 = String.valueOf(uploadFileResult.getMd5());
        pre.getHashCalculatorManager().getHashInfo().put(Constant.Hash.MessageDigest.MD5, uploadFileResult.getMd5());
        fileInfo.getHashInfo().put(Constant.Hash.MessageDigest.MD5, md5);

        String urlPath = StrUtil.isBlank(this.domain)
                ? String.valueOf(uploadFileResult.getPath())
                : String.valueOf(uploadFileResult.getPath()).substring(1);

        fileInfo.setUrl(domain + urlPath);
        fileInfo.setPath(uploadFileResult.getPath());
        fileInfo.setMetadata(new HashMap<>());
        fileInfo.setUserMetadata(new HashMap<>());
        fileInfo.setSize(uploadFileResult.getSize());
        if (ObjUtil.isEmpty(this.attr)) {
            Dict dict = Dict.create();
            dict.putAll(this.attr);
            fileInfo.setAttr(dict);
        }
    }

    private void fillThFileInfo(UploadFileResult uploadFileResult, FileInfo fileInfo, UploadPretreatment pre) {
        String urlPath = StrUtil.isBlank(this.domain)
                ? uploadFileResult.getPath()
                : uploadFileResult.getPath().substring(1);
        fileInfo.setThUrl(domain + urlPath);
        fileInfo.setThMetadata(new HashMap<>());
        fileInfo.setThUserMetadata(new HashMap<>());
        fileInfo.setThUserMetadata(new HashMap<>());
        fileInfo.setThSize(uploadFileResult.getSize());
    }

    @Override
    public boolean save(FileInfo fileInfo, UploadPretreatment pre) {
        try {
            String fileName = fileInfo.getFilename();
            String thFileName = fileInfo.getThFilename();
            UploadFileResult uploadFileResult = uploadFile(pre.getFileWrapper().getInputStream(), fileName, pre);

            if (ObjUtil.isEmpty(uploadFileResult)) {
                return false;
            }

            fillFileInfo(uploadFileResult, fileInfo, pre);

            // 上传小图
            if (ObjUtil.isNotEmpty(pre.getThumbnailBytes())) {
                UploadFileResult thUploadFileResult =
                        uploadFile(new ByteArrayInputStream(pre.getThumbnailBytes()), thFileName, pre);
                if (ObjUtil.isEmpty(thUploadFileResult)) {
                    throw new RuntimeException("上传thumbnail文件异常");
                } else {
                    fillThFileInfo(thUploadFileResult, fileInfo, pre);
                }
            }
        } catch (Exception e) {
            ExceptionFactory.upload(fileInfo, platform, e);
            return false;
        }
        return true;
    }

    private String jointPath(String methodUrl) {
        return this.jointPath(methodUrl, null);
    }

    private String jointPath(String methodUrl, String args) {
        if (StrUtil.isBlank(args)) {
            return StrUtil.format(methodUrl, this.server, this.group);
        } else {
            return StrUtil.format(methodUrl, this.server, this.group, args);
        }
    }

    @Override
    public boolean delete(FileInfo fileInfo) {
        HttpResponse httpResponse = HttpUtil.createPost(jointPath(URL_FILE_DELETE))
                .form(MapUtil.builder(new HashMap<String, Object>(5))
                        .put(PARAM_MD5, fileInfo.getHashInfo().getMd5())
                        .build())
                .timeout(this.timeOut)
                .execute();

        if (!httpResponse.isOk()) {
            ExceptionFactory.delete(fileInfo, platform, new RuntimeException(httpResponse.body()));
            return false;
        }

        String body = httpResponse.body();
        if (body.startsWith(PARAM_RESULT_PERMISSIONS)) {
            ExceptionFactory.delete(fileInfo, platform, new RuntimeException(body));
            return false;
        }

        JSONObject jsonObject = JSONUtil.parseObj(body);

        if (ObjUtil.isEmpty(jsonObject.get(PARAM_RESULT_STATUS))
                || PARAM_RESULT_FAIL.equalsIgnoreCase(jsonObject.getStr(PARAM_RESULT_STATUS))) {
            ExceptionFactory.delete(fileInfo, platform, new RuntimeException(jsonObject.getStr(PARAM_RESULT_MESSAGE)));
            return false;
        }

        return true;
    }

    private JSONObject getFile(String md5, String path, String fileName, String url) {

        GetFilePretreatment pre = new GetFilePretreatment();
        pre.setPlatform(this.platform);
        pre.setPath(path);
        pre.setFilename(fileName);
        pre.setUrl(url);

        HttpResponse httpResponse = null;
        HttpRequest post = HttpUtil.createPost(jointPath(URL_GET_FILE_INFO));
        if (StrUtil.isNotBlank(md5)) {
            httpResponse = post.form(MapUtil.builder(new HashMap<String, Object>(5))
                            .put(PARAM_MD5, md5)
                            .build())
                    .timeout(this.timeOut)
                    .execute();
        } else if (StrUtil.isNotBlank(path)) {
            httpResponse = post.form(MapUtil.builder(new HashMap<String, Object>(5))
                            .put(PARAM_PATH, path)
                            .build())
                    .timeout(this.timeOut)
                    .execute();
        } else {
            ExceptionFactory.getFile(pre, basePath, new RuntimeException("获取文件失败，md5与path同时为空"));
            return null;
        }

        if (!httpResponse.isOk()) {
            ExceptionFactory.getFile(pre, basePath, new RuntimeException(httpResponse.body()));
            return null;
        }

        JSONObject jsonObject = JSONUtil.parseObj(httpResponse.body());
        if (PARAM_RESULT_FAIL.startsWith(jsonObject.getStr(PARAM_RESULT_STATUS))) {
            ExceptionFactory.getFile(pre, basePath, new RuntimeException(jsonObject.getStr(PARAM_RESULT_MESSAGE)));
            return null;
        }
        return jsonObject;
    }

    @Override
    public boolean exists(FileInfo fileInfo) {
        JSONObject file =
                getFile(fileInfo.getHashInfo().getMd5(), fileInfo.getPath(), fileInfo.getFilename(), fileInfo.getUrl());
        if (ObjUtil.isEmpty(file)) {
            return false;
        }
        return true;
    }

    @Override
    public void download(FileInfo fileInfo, Consumer<InputStream> consumer) {
        String downloadUrl = this.server + fileInfo.getUrl();
        try {
            consumer.accept(getFile(downloadUrl));
        } catch (Exception e) {
            ExceptionFactory.download(fileInfo, this.platform, new RuntimeException(e.getMessage()));
        }
    }

    @Override
    public void downloadTh(FileInfo fileInfo, Consumer<InputStream> consumer) {
        String downloadUrl = this.server + fileInfo.getThUrl();
        try {
            consumer.accept(getFile(downloadUrl));
        } catch (Exception e) {
            ExceptionFactory.downloadTh(fileInfo, this.platform, new RuntimeException(e.getMessage()));
        }
    }

    private InputStream getFile(String fileUrl) {
        String url = fileUrl.replaceAll(" ", "%20");
        HttpResponse execute = HttpUtil.createGet(url).execute();
        if (execute.isOk()) {
            return execute.bodyStream();
        } else {
            throw new RuntimeException("下载文件异常");
        }
    }

    @Override
    public RemoteFileInfo getFile(GetFilePretreatment pre) {

        JSONObject file = this.getFile(null, pre.getPath(), pre.getFilename(), pre.getUrl());
        if (ObjUtil.isEmpty(file)) {
            return null;
        }
        GoFastFileBean data = JSONUtil.toBean(file.getJSONObject(PARAM_RESULT_DATA), GoFastFileBean.class);

        RemoteFileInfo info = new RemoteFileInfo();
        info.setPlatform(pre.getPlatform());
        info.setBasePath(basePath);
        info.setPath(pre.getPath());
        info.setFilename(data.getName());
        info.setUrl(domain + pre.getPath());
        info.setSize(data.getSize());
        info.setExt(FileNameUtil.extName(info.getFilename()));
        info.setETag(null);
        info.setContentDisposition(null);
        info.setContentType(null);
        info.setContentMd5(data.getMd5());
        info.setLastModified(DateUtil.date(data.getTimeStamp() * 1000));
        info.setMetadata(MapUtil.newHashMap());
        info.setUserMetadata(MapUtil.newHashMap());
        info.setOriginal(file);
        return info;
    }

    @Override
    public ListFilesSupportInfo isSupportListFiles() {
        return ListFilesSupportInfo.supportAll();
    }

    @Override
    public ListFilesResult listFiles(ListFilesPretreatment pre) {
        HttpResponse httpResponse = HttpUtil.createGet(jointPath(URL_LIST_DIR, pre.getPath()))
                .timeout(this.timeOut)
                .execute();

        if (!httpResponse.isOk()) {
            ExceptionFactory.listFiles(pre, this.basePath, new RuntimeException(httpResponse.body()));
            return null;
        }

        String body = httpResponse.body();
        if (body.startsWith(PARAM_RESULT_PERMISSIONS)) {
            ExceptionFactory.listFiles(pre, this.basePath, new RuntimeException(body));
            return null;
        }

        JSONObject jsonObject = JSONUtil.parseObj(body);

        if (StrUtil.isBlank(jsonObject.getStr(PARAM_RESULT_STATUS))) {
            ExceptionFactory.listFiles(
                    pre, this.basePath, new RuntimeException(jsonObject.getStr(PARAM_RESULT_MESSAGE)));
            return null;
        }

        ListFilesResult list = new ListFilesResult();

        List<ListItemBean> listItemBeans =
                JSONUtil.toList(jsonObject.getJSONArray(PARAM_RESULT_DATA), ListItemBean.class);

        List<RemoteDirInfo> dirList = ListUtil.toList();
        List<RemoteFileInfo> fileList = ListUtil.toList();
        list.setDirList(dirList);
        list.setFileList(fileList);
        for (ListItemBean listItemBean : listItemBeans) {
            if (listItemBean.isDir) {
                RemoteDirInfo dir = new RemoteDirInfo();
                dir.setPlatform(pre.getPlatform());
                dir.setBasePath(basePath);
                dir.setPath(pre.getPath());
                dir.setName(listItemBean.getName());
                dirList.add(dir);
            } else {
                RemoteFileInfo info = new RemoteFileInfo();
                info.setPlatform(pre.getPlatform());
                info.setBasePath(basePath);
                info.setPath(pre.getPath());
                info.setFilename(listItemBean.getName());
                info.setSize(listItemBean.getSize());
                info.setExt(FileNameUtil.extName(info.getFilename()));
                info.setLastModified(DateUtil.date(listItemBean.getMtime() * 1000));
                fileList.add(info);
            }
        }

        list.setPlatform(pre.getPlatform());
        list.setBasePath(basePath);
        list.setPath(pre.getPath());
        list.setFilenamePrefix(pre.getFilenamePrefix());
        list.setMaxFiles(listItemBeans.size());
        list.setIsTruncated(false);
        list.setMarker(null);
        list.setNextMarker(null);

        return list;
    }

    @Data
    public static class GoFastFileBean implements Serializable {
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

    @Data
    public static class ListItemBean implements Serializable {
        private boolean isDir;
        private String md5;
        private long mtime;
        private String path;
        private long size;
        private String name;
    }

    @Data
    public static class UploadFileResult implements Serializable {
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
}
