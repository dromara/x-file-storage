package cn.xuyanwu.spring.file.storage;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.xuyanwu.spring.file.storage.platform.FileStorage;
import cn.xuyanwu.spring.file.storage.recorder.FileRecorder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.List;


/**
 * 用来处理文件存储，对接多个平台
 */
@Getter
@Setter
@Slf4j
public class FileStorageService {

    @Lazy
    @Autowired
    private FileStorageService self;
    @Autowired
    private FileRecorder fileRecorder;
    @Autowired
    private List<List<? extends FileStorage>> fileStorageList;
    @Autowired
    FileStorageProperties properties;


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
        for (List<? extends FileStorage> subFileStorageList : fileStorageList) {
            for (FileStorage fileStorage : subFileStorageList) {
                if (fileStorage.getPlatform().equals(platform)) {
                    return fileStorage;
                }
            }
        }
        return null;
    }

    /**
     * 上传文件，成功返回文件信息，失败返回 null
     */
    public FileInfo upload(UploadPretreatment pre) {
        MultipartFile file = pre.getFileWrapper();
        Assert.notNull(file,"文件不允许为 null");
        Assert.notNull(pre.getPlatform(),"platform 不允许为 null");

        FileInfo fileInfo = new FileInfo();
        fileInfo.setCreateTime(new Date());
        fileInfo.setSize(file.getSize());
        fileInfo.setOriginalFilename(file.getOriginalFilename());
        fileInfo.setExt(FileNameUtil.getSuffix(file.getOriginalFilename()));
        fileInfo.setObjectId(pre.getObjectId());
        fileInfo.setObjectType(pre.getObjectType());
        fileInfo.setPath(pre.getPath());
        fileInfo.setPlatform(pre.getPlatform());
        fileInfo.setFilename(IdUtil.objectId() + (StrUtil.isEmpty(fileInfo.getExt()) ? StrUtil.EMPTY : "." + fileInfo.getExt()));

        byte[] thumbnailBytes = pre.getThumbnailBytes();
        if (thumbnailBytes != null) {
            fileInfo.setThSize((long) thumbnailBytes.length);
            fileInfo.setThFilename(fileInfo.getFilename() + pre.getThumbnailSuffix());
        }

        FileStorage fileStorage = getFileStorage(pre.getPlatform());
        Assert.notNull(fileStorage,"没有找到对应的存储平台！");

        if (fileStorage.save(fileInfo,pre)) {
            if (fileRecorder.record(fileInfo)) {
                return fileInfo;
            }
        }
        return null;
    }

    /**
     * 根据 url 删除文件
     */
    public boolean delete(String url) {
        //判断是否为缩略图路径的先写死
        url = StrUtil.removeSuffix(url,".min.jpg");
        FileInfo fileInfo = fileRecorder.getByUrl(url);
        return delete(fileInfo);
    }

    public boolean delete(FileInfo fileInfo) {
        if (fileInfo == null) return true;
        FileStorage fileStorage = getFileStorage(fileInfo.getPlatform());
        if (fileStorage == null) {
            log.warn("没有找到对应的存储平台！");
        } else if (fileStorage.delete(fileInfo)) {   //删除文件
            return fileRecorder.delete(fileInfo.getUrl());  //删除文件记录
        }
        return false;
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
     * 根据 InputStream 创建上传预处理器，name 为空字符串
     */
    public UploadPretreatment of(InputStream in) {
        try {
            UploadPretreatment pre = of();
            pre.setFileWrapper(new MultipartFileWrapper(new MockMultipartFile("",in)));
            return pre;
        } catch (Exception e) {
            log.error("根据 InputStream 创建上传预处理器失败！",e);
            return null;
        }
    }

    /**
     * 根据 File 创建上传预处理器，name 为 file 的 name
     */
    public UploadPretreatment of(File file) {
        try {
            UploadPretreatment pre = of();
            pre.setFileWrapper(new MultipartFileWrapper(new MockMultipartFile(file.getName(),file.getName(),null,new FileInputStream(file))));
            return pre;
        } catch (Exception e) {
            log.error("根据 File 创建上传预处理器失败！",e);
            return null;
        }
    }

    /**
     * 根据 URL 创建上传预处理器，name 为空字符串
     */
    public UploadPretreatment of(URL url) {
        try {
            UploadPretreatment pre = of();
            pre.setFileWrapper(new MultipartFileWrapper(new MockMultipartFile("",url.openStream())));
            return pre;
        } catch (Exception e) {
            log.error("根据 URL 创建上传预处理器失败！",e);
            return null;
        }
    }

}
