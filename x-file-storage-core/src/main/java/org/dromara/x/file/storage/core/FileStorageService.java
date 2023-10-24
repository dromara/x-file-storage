package org.dromara.x.file.storage.core;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.aspect.*;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.file.FileWrapperAdapter;
import org.dromara.x.file.storage.core.file.HttpServletRequestFileWrapper;
import org.dromara.x.file.storage.core.file.MultipartFormDataReader;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.tika.ContentTypeDetect;
import org.dromara.x.file.storage.core.util.Tools;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;


/**
 * 用来处理文件存储，对接多个平台
 */
@Slf4j
@Getter
@Setter
public class FileStorageService {

    private FileStorageService self;
    private FileRecorder fileRecorder;
    private CopyOnWriteArrayList<FileStorage> fileStorageList;
    private String defaultPlatform;
    private String thumbnailSuffix;
    private Boolean uploadNotSupportMetadataThrowException;
    private Boolean uploadNotSupportAclThrowException;
    private CopyOnWriteArrayList<FileStorageAspect> aspectList;
    private CopyOnWriteArrayList<FileWrapperAdapter> fileWrapperAdapterList;
    private ContentTypeDetect contentTypeDetect;


    /**
     * 获取默认的存储平台
     */
    public <T extends FileStorage> T getFileStorage() {
        return self.getFileStorage(defaultPlatform);
    }

    /**
     * 获取对应的存储平台
     */
    public <T extends FileStorage> T getFileStorage(String platform) {
        for (FileStorage fileStorage : fileStorageList) {
            if (fileStorage.getPlatform().equals(platform)) {
                return Tools.cast(fileStorage);
            }
        }
        return null;
    }

    /**
     * 获取对应的存储平台，如果存储平台不存在则抛出异常
     */
    public <T extends FileStorage> T getFileStorageVerify(FileInfo fileInfo) {
        return self.getFileStorageVerify(fileInfo.getPlatform());
    }

    /**
     * 获取对应的存储平台，如果存储平台不存在则抛出异常
     */
    public <T extends FileStorage> T getFileStorageVerify(String platform) {
        T fileStorage = self.getFileStorage(platform);
        if (fileStorage == null) throw new FileStorageRuntimeException("没有找到对应的存储平台！");
        return fileStorage;
    }

    /**
     * 上传文件，成功返回文件信息，失败返回 null
     */
    public FileInfo upload(UploadPretreatment pre) {
        FileWrapper file = pre.getFileWrapper();
        if (file == null) throw new FileStorageRuntimeException("文件不允许为 null ！");
        if (pre.getPlatform() == null) throw new FileStorageRuntimeException("platform 不允许为 null ！");

        FileInfo fileInfo = new FileInfo();
        fileInfo.setCreateTime(new Date());
        fileInfo.setSize(file.getSize());
        fileInfo.setOriginalFilename(file.getName());
        fileInfo.setExt(FileNameUtil.getSuffix(file.getName()));
        fileInfo.setObjectId(pre.getObjectId());
        fileInfo.setObjectType(pre.getObjectType());
        fileInfo.setPath(pre.getPath());
        fileInfo.setPlatform(pre.getPlatform());
        fileInfo.setMetadata(pre.getMetadata());
        fileInfo.setUserMetadata(pre.getUserMetadata());
        fileInfo.setThMetadata(pre.getThMetadata());
        fileInfo.setThUserMetadata(pre.getThUserMetadata());
        fileInfo.setAttr(pre.getAttr());
        fileInfo.setFileAcl(pre.getFileAcl());
        fileInfo.setThFileAcl(pre.getThFileAcl());
        if (StrUtil.isNotBlank(pre.getSaveFilename())) {
            fileInfo.setFilename(pre.getSaveFilename());
        } else {
            fileInfo.setFilename(IdUtil.objectId() + (StrUtil.isEmpty(fileInfo.getExt()) ? StrUtil.EMPTY : "." + fileInfo.getExt()));
        }
        fileInfo.setContentType(file.getContentType());

        byte[] thumbnailBytes = pre.getThumbnailBytes();
        if (thumbnailBytes != null) {
            fileInfo.setThSize((long) thumbnailBytes.length);
            if (StrUtil.isNotBlank(pre.getSaveThFilename())) {
                fileInfo.setThFilename(pre.getSaveThFilename() + pre.getThumbnailSuffix());
            } else {
                fileInfo.setThFilename(fileInfo.getFilename() + pre.getThumbnailSuffix());
            }
            if (StrUtil.isNotBlank(pre.getThContentType())) {
                fileInfo.setThContentType(pre.getThContentType());
            } else {
                fileInfo.setThContentType(contentTypeDetect.detect(thumbnailBytes,fileInfo.getThFilename()));
            }
        }

        FileStorage fileStorage = self.getFileStorage(pre.getPlatform());
        if (fileStorage == null) throw new FileStorageRuntimeException(StrUtil.format("没有找到对应的存储平台！platform:{}", pre.getPlatform()));

        //处理切面
        return new UploadAspectChain(aspectList,(_fileInfo,_pre,_fileStorage,_fileRecorder) -> {
            //真正开始保存
            if (_fileStorage.save(_fileInfo,_pre)) {
                if (_fileRecorder.save(_fileInfo)) {
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
        return self.delete(getFileInfoByUrl(url));
    }

    /**
     * 根据 url 删除文件
     */
    public boolean delete(String url,Predicate<FileInfo> predicate) {
        return self.delete(getFileInfoByUrl(url),predicate);
    }

    /**
     * 根据条件
     */
    public boolean delete(FileInfo fileInfo) {
        return self.delete(fileInfo,null);
    }

    /**
     * 根据条件删除文件
     */
    public boolean delete(FileInfo fileInfo,Predicate<FileInfo> predicate) {
        if (fileInfo == null) return true;
        if (predicate != null && !predicate.test(fileInfo)) return false;
        FileStorage fileStorage = self.getFileStorage(fileInfo.getPlatform());
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
        return self.exists(getFileInfoByUrl(url));
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
        return self.download(getFileInfoByUrl(url));
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
        return self.downloadTh(getFileInfoByUrl(url));
    }

    /**
     * 是否支持对文件生成可以签名访问的 URL
     */
    public boolean isSupportPresignedUrl(String platform) {
        FileStorage storage = self.getFileStorageVerify(platform);
        return self.isSupportPresignedUrl(storage);
    }

    /**
     * 是否支持对文件生成可以签名访问的 URL
     */
    public boolean isSupportPresignedUrl(FileStorage fileStorage) {
        if (fileStorage == null) return false;
        return new IsSupportPresignedUrlAspectChain(aspectList,FileStorage::isSupportPresignedUrl).next(fileStorage);
    }

    /**
     * 对文件生成可以签名访问的 URL，无法生成则返回 null
     *
     * @param expiration 到期时间
     */
    public String generatePresignedUrl(FileInfo fileInfo,Date expiration) {
        if (fileInfo == null) return null;
        return new GeneratePresignedUrlAspectChain(aspectList,(_fileInfo,_expiration,_fileStorage) ->
                _fileStorage.generatePresignedUrl(_fileInfo,_expiration)
        ).next(fileInfo,expiration,self.getFileStorageVerify(fileInfo));
    }

    /**
     * 对缩略图文件生成可以签名访问的 URL，无法生成则返回 null
     *
     * @param expiration 到期时间
     */
    public String generateThPresignedUrl(FileInfo fileInfo,Date expiration) {
        if (fileInfo == null) return null;
        return new GenerateThPresignedUrlAspectChain(aspectList,(_fileInfo,_expiration,_fileStorage) ->
                _fileStorage.generateThPresignedUrl(_fileInfo,_expiration)
        ).next(fileInfo,expiration,self.getFileStorageVerify(fileInfo));
    }

    /**
     * 是否支持对文件的访问控制列表
     */
    public boolean isSupportAcl(String platform) {
        FileStorage storage = self.getFileStorageVerify(platform);
        return self.isSupportAcl(storage);
    }

    /**
     * 是否支持对文件的访问控制列表
     */
    public boolean isSupportAcl(FileStorage fileStorage) {
        if (fileStorage == null) return false;
        return new IsSupportAclAspectChain(aspectList,FileStorage::isSupportAcl).next(fileStorage);
    }

    /**
     * 设置文件的访问控制列表，一般情况下只有对象存储支持该功能
     * 详情见{@link FileInfo#setFileAcl}
     */
    public boolean setFileAcl(FileInfo fileInfo,Object acl) {
        if (fileInfo == null) return false;
        return new SetFileAclAspectChain(aspectList,(_fileInfo,_acl,_fileStorage) ->
                _fileStorage.setFileAcl(_fileInfo,_acl)
        ).next(fileInfo,acl,self.getFileStorageVerify(fileInfo));
    }

    /**
     * 设置缩略图文件的访问控制列表，一般情况下只有对象存储支持该功能
     * 详情见{@link FileInfo#setFileAcl}
     */
    public boolean setThFileAcl(FileInfo fileInfo,Object acl) {
        if (fileInfo == null) return false;
        return new SetThFileAclAspectChain(aspectList,(_fileInfo,_acl,_fileStorage) ->
                _fileStorage.setThFileAcl(_fileInfo,_acl)
        ).next(fileInfo,acl,self.getFileStorageVerify(fileInfo));
    }

    /**
     * 是否支持 Metadata
     */
    public boolean isSupportMetadata(String platform) {
        FileStorage storage = self.getFileStorageVerify(platform);
        return self.isSupportMetadata(storage);
    }

    /**
     * 是否支持 Metadata
     */
    public boolean isSupportMetadata(FileStorage fileStorage) {
        if (fileStorage == null) return false;
        return new IsSupportMetadataAspectChain(aspectList,FileStorage::isSupportMetadata).next(fileStorage);
    }

    /**
     * 创建上传预处理器
     */
    public UploadPretreatment of() {
        UploadPretreatment pre = new UploadPretreatment();
        pre.setFileStorageService(self);
        pre.setPlatform(defaultPlatform);
        pre.setThumbnailSuffix(thumbnailSuffix);
        pre.setNotSupportMetadataThrowException(uploadNotSupportMetadataThrowException);
        pre.setNotSupportAclThrowException(uploadNotSupportAclThrowException);
        return pre;
    }

    /**
     * 创建上传预处理器
     *
     * @param source 源
     */
    public UploadPretreatment of(Object source) {
        return self.of(source,null,null);
    }

    /**
     * 创建上传预处理器
     *
     * @param source 源
     * @param name   文件名
     */
    public UploadPretreatment of(Object source,String name) {
        return self.of(source,name,null);
    }

    /**
     * 创建上传预处理器
     *
     * @param source      源
     * @param name        文件名
     * @param contentType 文件的 MIME 类型
     */
    public UploadPretreatment of(Object source,String name,String contentType) {
        return self.of(source,name,contentType,null);
    }

    /**
     * 创建上传预处理器
     *
     * @param source      源
     * @param name        文件名
     * @param contentType 文件的 MIME 类型
     * @param size        文件大小
     */
    public UploadPretreatment of(Object source,String name,String contentType,Long size) {
        FileWrapper wrapper = self.wrapper(source,name,contentType,size);
        UploadPretreatment up = self.of().setFileWrapper(wrapper);
        //这里针对 HttpServletRequestFileWrapper 特殊处理，加载读取到的缩略图文件
        if (wrapper instanceof HttpServletRequestFileWrapper) {
            MultipartFormDataReader.MultipartFormData data = ((HttpServletRequestFileWrapper) wrapper).getMultipartFormData();
            if (data.getThFileBytes() != null) {
                FileWrapper thWrapper = self.wrapper(data.getThFileBytes(),data.getThFileOriginalFilename(),data.getThFileContentType());
                up.thumbnailOf(thWrapper);
            }
        }
        return up;
    }

    /**
     * 对要上传的文件进行包装
     *
     * @param source 源
     */
    public FileWrapper wrapper(Object source) {
        return self.wrapper(source,null);
    }

    /**
     * 对要上传的文件进行包装
     *
     * @param source 源
     * @param name   文件名
     */
    public FileWrapper wrapper(Object source,String name) {
        return self.wrapper(source,name,null);
    }

    /**
     * 对要上传的文件进行包装
     *
     * @param source 源
     * @param name   文件名
     */
    public FileWrapper wrapper(Object source,String name,String contentType) {
        return self.wrapper(source,name,contentType,null);
    }

    /**
     * 对要上传的文件进行包装
     *
     * @param source      源
     * @param name        文件名
     * @param contentType 文件的 MIME 类型
     * @param size        文件大小
     */
    public FileWrapper wrapper(Object source,String name,String contentType,Long size) {
        if (source == null) {
            throw new FileStorageRuntimeException("要包装的文件不能是 null");
        }
        try {
            for (FileWrapperAdapter adapter : fileWrapperAdapterList) {
                if (adapter.isSupport(source)) {
                    return adapter.getFileWrapper(source,name,contentType,size);
                }
            }
        } catch (IOException e) {
            throw new FileStorageRuntimeException("文件包装失败",e);
        }
        throw new FileStorageRuntimeException("不支持此文件");
    }


    /**
     * 通过反射调用指定存储平台的方法
     * 详情见{@link ReflectUtil#invoke(Object,String,Object...)}
     */
    public <T> T invoke(String platform,String method,Object... args) {
        return self.invoke((FileStorage) self.getFileStorageVerify(platform),method,args);
    }

    /**
     * 通过反射调用指定存储平台的方法
     * 详情见{@link ReflectUtil#invoke(Object,String,Object...)}
     */
    public <T> T invoke(FileStorage platform,String method,Object... args) {
        return new InvokeAspectChain(aspectList,ReflectUtil::invoke)
                .next(platform,method,args);
    }


    public void destroy() {
        for (FileStorage fileStorage : fileStorageList) {
            try {
                fileStorage.close();
                log.info("销毁存储平台 {} 成功",fileStorage.getPlatform());
            } catch (Exception e) {
                log.error("销毁存储平台 {} 失败，{}",fileStorage.getPlatform(),e.getMessage(),e);
            }
        }
    }
}
