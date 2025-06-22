package org.dromara.x.file.storage.core;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.aspect.*;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.copy.CopyPretreatment;
import org.dromara.x.file.storage.core.exception.FileStorageRuntimeException;
import org.dromara.x.file.storage.core.file.FileWrapper;
import org.dromara.x.file.storage.core.file.FileWrapperAdapter;
import org.dromara.x.file.storage.core.file.HttpServletRequestFileWrapper;
import org.dromara.x.file.storage.core.file.MultipartFormDataReader;
import org.dromara.x.file.storage.core.get.GetFilePretreatment;
import org.dromara.x.file.storage.core.get.ListFilesPretreatment;
import org.dromara.x.file.storage.core.get.ListFilesSupportInfo;
import org.dromara.x.file.storage.core.get.RemoteFileInfo;
import org.dromara.x.file.storage.core.move.MovePretreatment;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlPretreatment;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.tika.ContentTypeDetect;
import org.dromara.x.file.storage.core.upload.*;
import org.dromara.x.file.storage.core.upload.MultipartUploadSupportInfo;
import org.dromara.x.file.storage.core.upload.UploadPretreatment;
import org.dromara.x.file.storage.core.util.Tools;

/**
 * 用来处理文件存储，对接多个平台
 */
@Slf4j
@Getter
@Setter
public class FileStorageService {

    private FileStorageService self;
    private FileStorageProperties properties;
    private FileRecorder fileRecorder;
    private CopyOnWriteArrayList<FileStorage> fileStorageList;
    private CopyOnWriteArrayList<FileStorageAspect> aspectList;
    private CopyOnWriteArrayList<FileWrapperAdapter> fileWrapperAdapterList;
    private ContentTypeDetect contentTypeDetect;

    /**
     * 获取默认的存储平台，请使用 getProperties().getDefaultPlatform() 代替
     */
    @Deprecated
    public String getDefaultPlatform() {
        return properties.getDefaultPlatform();
    }

    /**
     * 缩略图后缀，例如【.min.jpg】【.png】，请使用 getProperties().getThumbnailSuffix() 代替
     */
    @Deprecated
    public String getThumbnailSuffix() {
        return properties.getThumbnailSuffix();
    }

    /**
     * 获取默认的存储平台
     */
    public <T extends FileStorage> T getFileStorage() {
        return self.getFileStorage(properties.getDefaultPlatform());
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
        if (fileStorage == null)
            throw new FileStorageRuntimeException(StrUtil.format("没有找到对应的存储平台！platform:{}", platform));
        return fileStorage;
    }

    /**
     * 上传文件，成功返回文件信息，失败返回 null，请使用 pre.upload() 代替
     */
    @Deprecated
    public FileInfo upload(UploadPretreatment pre) {
        return pre.upload();
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
    public boolean delete(String url, Predicate<FileInfo> predicate) {
        return self.delete(getFileInfoByUrl(url), predicate);
    }

    /**
     * 根据条件
     */
    public boolean delete(FileInfo fileInfo) {
        return self.delete(fileInfo, null);
    }

    /**
     * 根据条件删除文件
     */
    public boolean delete(FileInfo fileInfo, Predicate<FileInfo> predicate) {
        if (fileInfo == null) return true;
        if (predicate != null && !predicate.test(fileInfo)) return false;
        FileStorage fileStorage = self.getFileStorage(fileInfo.getPlatform());
        if (fileStorage == null) throw new FileStorageRuntimeException("没有找到对应的存储平台！");
        return self.delete(fileInfo, fileStorage, fileRecorder, aspectList);
    }

    /**
     * 删除文件，仅限内部使用
     */
    public boolean delete(
            FileInfo fileInfo, FileStorage fileStorage, FileRecorder fileRecorder, List<FileStorageAspect> aspectList) {
        return new DeleteAspectChain(aspectList, (_fileInfo, _fileStorage, _fileRecorder) -> {
                    if (_fileStorage.delete(_fileInfo)) { // 删除文件
                        return _fileRecorder.delete(_fileInfo.getUrl()); // 删除文件记录
                    }
                    return false;
                })
                .next(fileInfo, fileStorage, fileRecorder);
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
        return new ExistsAspectChain(aspectList, (_fileInfo, _fileStorage) -> _fileStorage.exists(_fileInfo))
                .next(fileInfo, getFileStorageVerify(fileInfo));
    }

    /**
     * 获取文件下载器
     */
    public Downloader download(FileInfo fileInfo) {
        return new Downloader(fileInfo, aspectList, getFileStorageVerify(fileInfo), Downloader.TARGET_FILE);
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
        return new Downloader(fileInfo, aspectList, getFileStorageVerify(fileInfo), Downloader.TARGET_TH_FILE);
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
    public boolean isSupportPresignedUrl() {
        return self.isSupportPresignedUrl(properties.getDefaultPlatform());
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
        return new IsSupportPresignedUrlAspectChain(aspectList, FileStorage::isSupportPresignedUrl).next(fileStorage);
    }

    /**
     * 生成预签名 URL
     * @return 生成预签名 URL 预处理器
     */
    public GeneratePresignedUrlPretreatment generatePresignedUrl() {
        return new GeneratePresignedUrlPretreatment()
                .setFileStorageService(self)
                .setPlatform(properties.getDefaultPlatform());
    }

    /**
     * 生成预签名 URL
     * @return 生成预签名 URL 预处理器
     */
    public GeneratePresignedUrlPretreatment generatePresignedUrl(String platform) {
        return new GeneratePresignedUrlPretreatment()
                .setFileStorageService(self)
                .setPlatform(platform);
    }

    /**
     * 对文件生成可以签名访问的 URL，无法生成则返回 null
     *
     * @param expiration 到期时间
     */
    public String generatePresignedUrl(FileInfo fileInfo, Date expiration) {
        if (fileInfo == null) return null;
        GeneratePresignedUrlResult result = generatePresignedUrl()
                .setExpiration(expiration)
                .setPlatform(fileInfo.getPlatform())
                .setPath(fileInfo.getPath())
                .setFilename(fileInfo.getFilename())
                .setMethod(Constant.GeneratePresignedUrl.Method.GET)
                .generatePresignedUrl();
        return result == null ? null : result.getUrl();
    }

    /**
     * 对缩略图文件生成可以签名访问的 URL，无法生成则返回 null
     *
     * @param expiration 到期时间
     */
    public String generateThPresignedUrl(FileInfo fileInfo, Date expiration) {
        if (fileInfo == null) return null;
        GeneratePresignedUrlResult result = generatePresignedUrl()
                .setExpiration(expiration)
                .setPlatform(fileInfo.getPlatform())
                .setPath(fileInfo.getPath())
                .setFilename(fileInfo.getThFilename())
                .setMethod(Constant.GeneratePresignedUrl.Method.GET)
                .generatePresignedUrl();
        return result == null ? null : result.getUrl();
    }

    /**
     * 是否支持对文件的访问控制列表
     */
    public boolean isSupportAcl() {
        return self.isSupportAcl(properties.getDefaultPlatform());
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
        return new IsSupportAclAspectChain(aspectList, FileStorage::isSupportAcl).next(fileStorage);
    }

    /**
     * 设置文件的访问控制列表，一般情况下只有对象存储支持该功能
     * 详情见{@link FileInfo#setFileAcl}
     */
    public boolean setFileAcl(FileInfo fileInfo, Object acl) {
        if (fileInfo == null) return false;
        return new SetFileAclAspectChain(
                        aspectList, (_fileInfo, _acl, _fileStorage) -> _fileStorage.setFileAcl(_fileInfo, _acl))
                .next(fileInfo, acl, self.getFileStorageVerify(fileInfo));
    }

    /**
     * 设置缩略图文件的访问控制列表，一般情况下只有对象存储支持该功能
     * 详情见{@link FileInfo#setFileAcl}
     */
    public boolean setThFileAcl(FileInfo fileInfo, Object acl) {
        if (fileInfo == null) return false;
        return new SetThFileAclAspectChain(
                        aspectList, (_fileInfo, _acl, _fileStorage) -> _fileStorage.setThFileAcl(_fileInfo, _acl))
                .next(fileInfo, acl, self.getFileStorageVerify(fileInfo));
    }

    /**
     * 是否支持 Metadata
     */
    public boolean isSupportMetadata() {
        return self.isSupportMetadata(properties.getDefaultPlatform());
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
        return new IsSupportMetadataAspectChain(aspectList, FileStorage::isSupportMetadata).next(fileStorage);
    }

    /**
     * 创建上传预处理器
     */
    public UploadPretreatment of() {
        UploadPretreatment pre = new UploadPretreatment();
        pre.setFileStorageService(self);
        pre.setPlatform(properties.getDefaultPlatform());
        pre.setThumbnailSuffix(properties.getThumbnailSuffix());
        pre.setNotSupportMetadataThrowException(properties.getUploadNotSupportMetadataThrowException());
        pre.setNotSupportAclThrowException(properties.getUploadNotSupportAclThrowException());
        return pre;
    }

    /**
     * 创建上传预处理器
     *
     * @param source 源
     */
    public UploadPretreatment of(Object source) {
        return self.of(source, null, null);
    }

    /**
     * 创建上传预处理器
     *
     * @param source 源
     * @param name   文件名
     */
    public UploadPretreatment of(Object source, String name) {
        return self.of(source, name, null);
    }

    /**
     * 创建上传预处理器
     *
     * @param source      源
     * @param name        文件名
     * @param contentType 文件的 MIME 类型
     */
    public UploadPretreatment of(Object source, String name, String contentType) {
        return self.of(source, name, contentType, null);
    }

    /**
     * 默认使用的存储平台是否支持手动分片上传
     */
    public MultipartUploadSupportInfo isSupportMultipartUpload() {
        return self.isSupportMultipartUpload(properties.getDefaultPlatform());
    }

    /**
     * 是否支持手动分片上传
     */
    public MultipartUploadSupportInfo isSupportMultipartUpload(String platform) {
        FileStorage storage = self.getFileStorageVerify(platform);
        return self.isSupportMultipartUpload(storage);
    }

    /**
     * 是否支持手动分片上传
     */
    public MultipartUploadSupportInfo isSupportMultipartUpload(FileStorage fileStorage) {
        if (fileStorage == null) return MultipartUploadSupportInfo.notSupport();
        return new IsSupportMultipartUploadAspectChain(aspectList, FileStorage::isSupportMultipartUpload)
                .next(fileStorage);
    }

    /**
     * 手动分片上传-初始化
     */
    public InitiateMultipartUploadPretreatment initiateMultipartUpload() {
        InitiateMultipartUploadPretreatment pre = new InitiateMultipartUploadPretreatment();
        pre.setFileStorageService(self);
        pre.setPlatform(properties.getDefaultPlatform());
        pre.setNotSupportMetadataThrowException(properties.getUploadNotSupportMetadataThrowException());
        pre.setNotSupportAclThrowException(properties.getUploadNotSupportAclThrowException());
        return pre;
    }

    /**
     * 手动分片上传-上传分片
     * @param fileInfo 文件信息
     * @param partNumber 分片号。每一个上传的分片都有一个分片号，一般情况下取值范围是1~10000
     * @param source      源
     * @return 手动分片上传-上传分片预处理器
     */
    public UploadPartPretreatment uploadPart(FileInfo fileInfo, int partNumber, Object source) {
        return self.uploadPart(fileInfo, partNumber, source, null);
    }

    /**
     * 手动分片上传-上传分片
     * @param fileInfo 文件信息
     * @param partNumber 分片号。每一个上传的分片都有一个分片号，一般情况下取值范围是1~10000
     * @param source      源
     * @param size        源的文件大小
     * @return 手动分片上传-上传分片预处理器
     */
    public UploadPartPretreatment uploadPart(FileInfo fileInfo, int partNumber, Object source, Long size) {
        UploadPartPretreatment pre = new UploadPartPretreatment();
        pre.setFileStorageService(self);
        pre.setFileInfo(fileInfo);
        pre.setPartNumber(partNumber);
        // 这是是个优化，如果是 FileWrapper 对象就直接使用，否则就创建 FileWrapper 并指定 contentType 避免自动识别造成性能浪费
        if (source instanceof FileWrapper) {
            pre.setPartFileWrapper((FileWrapper) source);
        } else {
            pre.setPartFileWrapper(self.wrapper(source, null, "application/octet-stream", size));
        }
        return pre;
    }

    /**
     * 手动分片上传-完成
     * @param fileInfo 文件信息，如果在初始化时传入了 ACL 访问控制列表、Metadata 元数据等信息，
     *                 一定要保证这里的 fileInfo 也有相同的信息，否则有些存储平台会不生效，
     *                 这是因为每个存储平台的逻辑不一样，有些是初始化时传入的，有些是完成时传入的，
     *                 建议将 FileInfo 保存到数据库中，这样就可以使用 fileStorageService.getFileInfoByUrl("https://abc.def.com/xxx.png")
     *                 来获取 FileInfo 方便操作，详情请阅读 https://x-file-storage.xuyanwu.cn/2.3.0/#/%E5%9F%BA%E7%A1%80%E5%8A%9F%E8%83%BD?id=%E4%BF%9D%E5%AD%98%E4%B8%8A%E4%BC%A0%E8%AE%B0%E5%BD%95
     */
    public CompleteMultipartUploadPretreatment completeMultipartUpload(FileInfo fileInfo) {
        CompleteMultipartUploadPretreatment pre = new CompleteMultipartUploadPretreatment();
        pre.setFileStorageService(self);
        pre.setFileInfo(fileInfo);
        return pre;
    }

    /**
     * 手动分片上传-取消
     */
    public AbortMultipartUploadPretreatment abortMultipartUpload(FileInfo fileInfo) {
        AbortMultipartUploadPretreatment pre = new AbortMultipartUploadPretreatment();
        pre.setFileStorageService(self);
        pre.setFileInfo(fileInfo);
        return pre;
    }

    /**
     * 手动分片上传-列举已上传的分片
     */
    public ListPartsPretreatment listParts(FileInfo fileInfo) {
        ListPartsPretreatment pre = new ListPartsPretreatment();
        pre.setFileStorageService(self);
        pre.setFileInfo(fileInfo);
        return pre;
    }

    /**
     * 默认使用的存储平台是否支持列举文件
     */
    public ListFilesSupportInfo isSupportListFiles() {
        return self.isSupportListFiles(properties.getDefaultPlatform());
    }

    /**
     * 是否支持列举文件
     */
    public ListFilesSupportInfo isSupportListFiles(String platform) {
        FileStorage storage = self.getFileStorageVerify(platform);
        return self.isSupportListFiles(storage);
    }

    /**
     * 是否支持列举文件
     */
    public ListFilesSupportInfo isSupportListFiles(FileStorage fileStorage) {
        if (fileStorage == null) return ListFilesSupportInfo.notSupport();
        return new IsSupportListFilesAspectChain(aspectList, FileStorage::isSupportListFiles).next(fileStorage);
    }

    /**
     * 列举文件
     */
    public ListFilesPretreatment listFiles() {
        ListFilesPretreatment pre = new ListFilesPretreatment();
        pre.setPlatform(properties.getDefaultPlatform());
        pre.setFileStorageService(self);
        return pre;
    }

    /**
     * 获取文件
     */
    public GetFilePretreatment getFile() {
        return new GetFilePretreatment()
                .setPlatform(properties.getDefaultPlatform())
                .setFileStorageService(self);
    }

    /**
     * 获取文件
     * @param fileInfo 文件信息
     * @return 远程文件信息
     */
    public RemoteFileInfo getFile(FileInfo fileInfo) {
        return getFile()
                .setPlatform(fileInfo.getPlatform())
                .setPath(fileInfo.getPath() != null, fileInfo.getPath())
                .setFilename(fileInfo.getFilename() != null, fileInfo.getFilename())
                .setUrl(fileInfo.getUrl() != null, fileInfo.getUrl())
                .getFile();
    }

    /**
     * 获取缩略图文件
     * @param fileInfo 文件信息
     * @return 远程文件信息
     */
    public RemoteFileInfo getThFile(FileInfo fileInfo) {
        return getFile()
                .setPlatform(fileInfo.getPlatform())
                .setPath(fileInfo.getPath() != null, fileInfo.getPath())
                .setFilename(fileInfo.getThFilename() != null, fileInfo.getThFilename())
                .setUrl(fileInfo.getThUrl() != null, fileInfo.getThUrl())
                .getFile();
    }

    /**
     * 创建上传预处理器
     *
     * @param source      源
     * @param name        文件名
     * @param contentType 文件的 MIME 类型
     * @param size        文件大小
     */
    public UploadPretreatment of(Object source, String name, String contentType, Long size) {
        FileWrapper wrapper = self.wrapper(source, name, contentType, size);
        UploadPretreatment up = self.of().setFileWrapper(wrapper);
        // 这里针对 HttpServletRequestFileWrapper 特殊处理，加载读取到的缩略图文件
        if (wrapper instanceof HttpServletRequestFileWrapper) {
            MultipartFormDataReader.MultipartFormData data =
                    ((HttpServletRequestFileWrapper) wrapper).getMultipartFormData();
            if (data.getThFileBytes() != null) {
                FileWrapper thWrapper = self.wrapper(
                        data.getThFileBytes(), data.getThFileOriginalFilename(), data.getThFileContentType());
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
        return self.wrapper(source, null);
    }

    /**
     * 对要上传的文件进行包装
     *
     * @param source 源
     * @param name   文件名
     */
    public FileWrapper wrapper(Object source, String name) {
        return self.wrapper(source, name, null);
    }

    /**
     * 对要上传的文件进行包装
     *
     * @param source 源
     * @param name   文件名
     */
    public FileWrapper wrapper(Object source, String name, String contentType) {
        return self.wrapper(source, name, contentType, null);
    }

    /**
     * 对要上传的文件进行包装
     *
     * @param source      源
     * @param name        文件名
     * @param contentType 文件的 MIME 类型
     * @param size        文件大小
     */
    public FileWrapper wrapper(Object source, String name, String contentType, Long size) {
        if (source == null) {
            throw new FileStorageRuntimeException("要包装的文件不能是 null");
        }
        try {
            for (FileWrapperAdapter adapter : fileWrapperAdapterList) {
                if (adapter.isSupport(source)) {
                    return adapter.getFileWrapper(source, name, contentType, size);
                }
            }
        } catch (IOException e) {
            throw new FileStorageRuntimeException("文件包装失败", e);
        }
        throw new FileStorageRuntimeException("不支持此文件");
    }

    /**
     * 是否支持同存储平台复制文件
     */
    public boolean isSupportSameCopy() {
        return self.isSupportSameCopy(properties.getDefaultPlatform());
    }

    /**
     * 是否支持同存储平台复制文件
     */
    public boolean isSupportSameCopy(String platform) {
        FileStorage storage = self.getFileStorageVerify(platform);
        return self.isSupportSameCopy(storage);
    }

    /**
     * 是否支持同存储平台复制文件
     */
    public boolean isSupportSameCopy(FileStorage fileStorage) {
        if (fileStorage == null) return false;
        return new IsSupportSameCopyAspectChain(aspectList, FileStorage::isSupportSameCopy).next(fileStorage);
    }

    /**
     * 复制文件
     */
    public CopyPretreatment copy(FileInfo fileInfo) {
        return new CopyPretreatment(fileInfo, self)
                .setNotSupportMetadataThrowException(properties.getCopyNotSupportMetadataThrowException())
                .setNotSupportAclThrowException(properties.getCopyNotSupportAclThrowException());
    }

    /**
     * 复制文件
     */
    public CopyPretreatment copy(String url) {
        return self.copy(self.getFileInfoByUrl(url));
    }

    /**
     * 是否支持同存储平台移动文件
     */
    public boolean isSupportSameMove() {
        return self.isSupportSameMove(properties.getDefaultPlatform());
    }

    /**
     * 是否支持同存储平台移动文件
     */
    public boolean isSupportSameMove(String platform) {
        FileStorage storage = self.getFileStorageVerify(platform);
        return self.isSupportSameMove(storage);
    }

    /**
     * 是否支持同存储平台移动文件
     */
    public boolean isSupportSameMove(FileStorage fileStorage) {
        if (fileStorage == null) return false;
        return new IsSupportSameMoveAspectChain(aspectList, FileStorage::isSupportSameMove).next(fileStorage);
    }

    /**
     * 移动文件
     */
    public MovePretreatment move(FileInfo fileInfo) {
        return new MovePretreatment(fileInfo, self)
                .setNotSupportMetadataThrowException(properties.getMoveNotSupportMetadataThrowException())
                .setNotSupportAclThrowException(properties.getMoveNotSupportAclThrowException());
    }

    /**
     * 移动文件
     */
    public MovePretreatment move(String url) {
        return self.move(self.getFileInfoByUrl(url));
    }

    /**
     * 通过反射调用指定存储平台的方法
     * 详情见{@link ReflectUtil#invoke(Object,String,Object...)}
     */
    public <T> T invoke(String platform, String method, Object... args) {
        return self.invoke((FileStorage) self.getFileStorageVerify(platform), method, args);
    }

    /**
     * 通过反射调用指定存储平台的方法
     * 详情见{@link ReflectUtil#invoke(Object,String,Object...)}
     */
    public <T> T invoke(FileStorage platform, String method, Object... args) {
        return new InvokeAspectChain(aspectList, ReflectUtil::invoke).next(platform, method, args);
    }

    public void destroy() {
        for (FileStorage fileStorage : fileStorageList) {
            try {
                fileStorage.close();
                log.info("销毁存储平台 {} 成功", fileStorage.getPlatform());
            } catch (Exception e) {
                log.error("销毁存储平台 {} 失败，{}", fileStorage.getPlatform(), e.getMessage(), e);
            }
        }
    }
}
