package cn.xuyanwu.spring.file.storage;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.xuyanwu.spring.file.storage.aspect.DeleteAspectChain;
import cn.xuyanwu.spring.file.storage.aspect.ExistsAspectChain;
import cn.xuyanwu.spring.file.storage.aspect.FileStorageAspect;
import cn.xuyanwu.spring.file.storage.aspect.UploadAspectChain;
import cn.xuyanwu.spring.file.storage.exception.FileStorageRuntimeException;
import cn.xuyanwu.spring.file.storage.platform.FileStorage;
import cn.xuyanwu.spring.file.storage.recorder.FileRecorder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;


/**
 * 用来处理文件存储，对接多个平台
 */
@Slf4j
@Getter
@Setter
public class FileStorageService implements DisposableBean {

    private FileStorageService self;
    private FileRecorder fileRecorder;
    private CopyOnWriteArrayList<FileStorage> fileStorageList;
    private FileStorageProperties properties;
    private CopyOnWriteArrayList<FileStorageAspect> aspectList;


    /**
     * 获取默认的存储平台
     */
    public FileStorage getFileStorage() {
        return getFileStorage(properties.getDefaultPlatform());
    }

    /**
     * 获取对应的存储平台
     */
    public FileStorage getFileStorage(String platform) {
        for (FileStorage fileStorage : fileStorageList) {
            if (fileStorage.getPlatform().equals(platform)) {
                return fileStorage;
            }
        }
        return null;
    }

    /**
     * 获取对应的存储平台，如果存储平台不存在则抛出异常
     */
    public FileStorage getFileStorageVerify(FileInfo fileInfo) {
        FileStorage fileStorage = getFileStorage(fileInfo.getPlatform());
        if (fileStorage == null) throw new FileStorageRuntimeException("没有找到对应的存储平台！");
        return fileStorage;
    }

    /**
     * 上传文件，成功返回文件信息，失败返回 null
     */
    public FileInfo upload(UploadPretreatment pre) {
        MultipartFile file = pre.getFileWrapper();
        if (file == null) throw new FileStorageRuntimeException("文件不允许为 null ！");
        if (pre.getPlatform() == null) throw new FileStorageRuntimeException("platform 不允许为 null ！");

        FileInfo fileInfo = new FileInfo();
        fileInfo.setCreateTime(new Date());
        fileInfo.setSize(file.getSize());
        fileInfo.setOriginalFilename(file.getOriginalFilename());
        fileInfo.setExt(FileNameUtil.getSuffix(file.getOriginalFilename()));
        fileInfo.setObjectId(pre.getObjectId());
        fileInfo.setObjectType(pre.getObjectType());
        fileInfo.setPath(pre.getPath());
        fileInfo.setPlatform(pre.getPlatform());
        fileInfo.setAttr(pre.getAttr());
        if (StrUtil.isNotBlank(pre.getSaveFilename())) {
            fileInfo.setFilename(pre.getSaveFilename());
        } else {
            fileInfo.setFilename(IdUtil.objectId() + (StrUtil.isEmpty(fileInfo.getExt()) ? StrUtil.EMPTY : "." + fileInfo.getExt()));
        }
        if (pre.getContentType() != null) {
            fileInfo.setContentType(pre.getContentType());
        } else if (pre.getFileWrapper().getContentType() != null) {
            fileInfo.setContentType(pre.getFileWrapper().getContentType());
        } else {
            String contentType = URLConnection.guessContentTypeFromName(fileInfo.getFilename());
            fileInfo.setContentType(contentType != null ? contentType : "application/octet-stream");
        }

        byte[] thumbnailBytes = pre.getThumbnailBytes();
        if (thumbnailBytes != null) {
            fileInfo.setThSize((long) thumbnailBytes.length);
            if (StrUtil.isNotBlank(pre.getSaveThFilename())) {
                fileInfo.setThFilename(pre.getSaveThFilename() + pre.getThumbnailSuffix());
            } else {
                fileInfo.setThFilename(fileInfo.getFilename() + pre.getThumbnailSuffix());
            }
            String contentType = URLConnection.guessContentTypeFromName(fileInfo.getThFilename());
            fileInfo.setThContentType(contentType != null ? contentType : "application/octet-stream");
        }

        FileStorage fileStorage = getFileStorage(pre.getPlatform());
        if (fileStorage == null) throw new FileStorageRuntimeException("没有找到对应的存储平台！");

        //处理切面
        return new UploadAspectChain(aspectList,(_fileInfo,_pre,_fileStorage,_fileRecorder) -> {
            //真正开始保存
            if (_fileStorage.save(_fileInfo,_pre)) {
                if (_fileRecorder.record(_fileInfo)) {
                    return _fileInfo;
                }
            }
            return null;
        }).next(fileInfo,pre,fileStorage,fileRecorder);
    }

    /**
     * 根据 url 获取 FileInfo
     */
    public FileInfo getFileInfoByUrl(String url) {
        return fileRecorder.getByUrl(url);
    }

    /**
     * 根据 url 删除文件
     */
    public boolean delete(String url) {
        return delete(getFileInfoByUrl(url));
    }

    /**
     * 根据 url 删除文件
     */
    public boolean delete(String url,Predicate<FileInfo> predicate) {
        return delete(getFileInfoByUrl(url),predicate);
    }

    /**
     * 根据条件
     */
    public boolean delete(FileInfo fileInfo) {
        return delete(fileInfo,null);
    }

    /**
     * 根据条件删除文件
     */
    public boolean delete(FileInfo fileInfo,Predicate<FileInfo> predicate) {
        if (fileInfo == null) return true;
        if (predicate != null && !predicate.test(fileInfo)) return false;
        FileStorage fileStorage = getFileStorage(fileInfo.getPlatform());
        if (fileStorage == null) throw new FileStorageRuntimeException("没有找到对应的存储平台！");

        return new DeleteAspectChain(aspectList,(_fileInfo,_fileStorage,_fileRecorder) -> {
            if (_fileStorage.delete(_fileInfo)) {   //删除文件
                return _fileRecorder.delete(_fileInfo.getUrl());  //删除文件记录
            }
            return false;
        }).next(fileInfo,fileStorage,fileRecorder);
    }

    /**
     * 文件是否存在
     */
    public boolean exists(String url) {
        return exists(getFileInfoByUrl(url));
    }

    /**
     * 文件是否存在
     */
    public boolean exists(FileInfo fileInfo) {
        if (fileInfo == null) return false;
        return new ExistsAspectChain(aspectList,(_fileInfo,_fileStorage) ->
                _fileStorage.exists(_fileInfo)
        ).next(fileInfo,getFileStorageVerify(fileInfo));
    }


    /**
     * 获取文件下载器
     */
    public Downloader download(FileInfo fileInfo) {
        return new Downloader(fileInfo,aspectList,getFileStorageVerify(fileInfo),Downloader.TARGET_FILE);
    }

    /**
     * 获取文件下载器
     */
    public Downloader download(String url) {
        return download(getFileInfoByUrl(url));
    }

    /**
     * 获取缩略图文件下载器
     */
    public Downloader downloadTh(FileInfo fileInfo) {
        return new Downloader(fileInfo,aspectList,getFileStorageVerify(fileInfo),Downloader.TARGET_TH_FILE);
    }

    /**
     * 获取缩略图文件下载器
     */
    public Downloader downloadTh(String url) {
        return downloadTh(getFileInfoByUrl(url));
    }


    /**
     * 创建上传预处理器
     */
    public UploadPretreatment of() {
        UploadPretreatment pre = new UploadPretreatment();
        pre.setFileStorageService(self);
        pre.setPlatform(properties.getDefaultPlatform());
        pre.setThumbnailSuffix(properties.getThumbnailSuffix());
        return pre;
    }

    /**
     * 根据 MultipartFile 创建上传预处理器
     */
    public UploadPretreatment of(MultipartFile file) {
        UploadPretreatment pre = of();
        pre.setFileWrapper(new MultipartFileWrapper(file));
        return pre;
    }

    /**
     * 根据 byte[] 创建上传预处理器，name 为空字符串
     */
    public UploadPretreatment of(byte[] bytes) {
        UploadPretreatment pre = of();
        pre.setFileWrapper(new MultipartFileWrapper(new MockMultipartFile("",bytes)));
        return pre;
    }

    /**
     * 根据 InputStream 创建上传预处理器，originalFilename 为空字符串
     */
    public UploadPretreatment of(InputStream in) {
        try {
            UploadPretreatment pre = of();
            pre.setFileWrapper(new MultipartFileWrapper(new MockMultipartFile("",in)));
            return pre;
        } catch (Exception e) {
            throw new FileStorageRuntimeException("根据 InputStream 创建上传预处理器失败！",e);
        }
    }

    /**
     * 根据 File 创建上传预处理器，originalFilename 为 file 的 name
     */
    public UploadPretreatment of(File file) {
        try {
            UploadPretreatment pre = of();
            pre.setFileWrapper(new MultipartFileWrapper(new MockMultipartFile(file.getName(),file.getName(),URLConnection.guessContentTypeFromName(file.getName()),Files.newInputStream(file.toPath()))));
            return pre;
        } catch (Exception e) {
            throw new FileStorageRuntimeException("根据 File 创建上传预处理器失败！",e);
        }
    }

    /**
     * 根据 URL 创建上传预处理器，originalFilename 将尝试自动识别，识别不到则为空字符串
     */
    public UploadPretreatment of(URL url) {
        try {
            UploadPretreatment pre = of();

            URLConnection conn = url.openConnection();

            //尝试获取文件名
            String name = "";
            String disposition = conn.getHeaderField("Content-Disposition");
            if (StrUtil.isNotBlank(disposition)) {
                name = ReUtil.get("filename=\"(.*?)\"",disposition,1);
                if (StrUtil.isBlank(name)) {
                    name = StrUtil.subAfter(disposition,"filename=",true);
                }
            }
            if (StrUtil.isBlank(name)) {
                final String path = url.getPath();
                name = StrUtil.subSuf(path,path.lastIndexOf('/') + 1);
                if (StrUtil.isNotBlank(name)) {
                    name = URLUtil.decode(name,StandardCharsets.UTF_8);
                }
            }

            pre.setFileWrapper(new MultipartFileWrapper(new MockMultipartFile(url.toString(),name,conn.getContentType(),conn.getInputStream())));
            return pre;
        } catch (Exception e) {
            throw new FileStorageRuntimeException("根据 URL 创建上传预处理器失败！",e);
        }
    }

    /**
     * 根据 URI 创建上传预处理器，originalFilename 将尝试自动识别，识别不到则为空字符串
     */
    public UploadPretreatment of(URI uri) {
        try {
            return of(uri.toURL());
        } catch (Exception e) {
            throw new FileStorageRuntimeException("根据 URI 创建上传预处理器失败！",e);
        }
    }

    /**
     * 根据 url 字符串创建上传预处理器，兼容Spring的ClassPath路径、文件路径、HTTP路径等，originalFilename 将尝试自动识别，识别不到则为空字符串
     */
    public UploadPretreatment of(String url) {
        try {
            return of(URLUtil.url(url));
        } catch (Exception e) {
            throw new FileStorageRuntimeException("根据 url：" + url + " 创建上传预处理器失败！",e);
        }
    }

    @Override
    public void destroy() {
        for (FileStorage fileStorage : fileStorageList) {
            try {
                fileStorage.close();
                log.error("销毁存储平台 {} 成功",fileStorage.getPlatform());
            } catch (Exception e) {
                log.error("销毁存储平台 {} 失败，{}",fileStorage.getPlatform(),e.getMessage(),e);
            }
        }
    }
}
