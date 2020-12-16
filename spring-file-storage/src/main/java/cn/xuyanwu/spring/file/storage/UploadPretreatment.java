package cn.xuyanwu.spring.file.storage;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Assert;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.function.Consumer;

/**
 * 文件上传预处理对象
 */
@Slf4j
@Getter
@Setter
@Accessors(chain = true)
public class UploadPretreatment {
    private FileStorageService fileStorageService;
    /**
     * 要上传到的平台
     */
    private String platform;
    /**
     * 要上传的文件包装类
     */
    private MultipartFileWrapper fileWrapper;
    /**
     * 要上传文件的缩略图
     */
    private byte[] thumbnailBytes;
    /**
     * 缩略图后缀，不是扩展名但包含扩展名，例如【.min.jpg】【.png】。
     * 只能在缩略图生成前进行修改后缀中的扩展名部分。
     * 例如当前是【.min.jpg】那么扩展名就是【jpg】，当缩略图未生成的情况下可以随意修改（扩展名必须是 thumbnailator 支持的图片格式），
     * 一旦缩略图生成后，扩展名之外的部分可以随意改变 ，扩展名部分不能改变，除非你在 {@link this#thumbnail} 方法中修改了输出格式。
     */
    private String thumbnailSuffix;
    /**
     * 文件所属对象id
     */
    private String objectId;
    /**
     * 文件所属对象类型
     */
    private String objectType;
    /**
     * 文件存储路径
     */
    private String path = "";


    /**
     * 获取文件名
     */
    public String getName() {
        return fileWrapper.getName();
    }

    /**
     * 设置文件名
     */
    public UploadPretreatment setName(String name) {
        fileWrapper.setName(name);
        return this;
    }

    /**
     * 获取原始文件名
     */
    public String getOriginalFilename() {
        return fileWrapper.getOriginalFilename();
    }

    /**
     * 设置原始文件名
     */
    public UploadPretreatment setOriginalFilename(String originalFilename) {
        fileWrapper.setOriginalFilename(originalFilename);
        return this;
    }


    /**
     * 进行图片处理，可以进行裁剪、旋转、缩放、水印等操作
     */
    public UploadPretreatment image(Consumer<Thumbnails.Builder<? extends InputStream>> consumer) {
        try (InputStream in = fileWrapper.getInputStream()) {
            Thumbnails.Builder<? extends InputStream> builder = Thumbnails.of(in);
            consumer.accept(builder);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            builder.toOutputStream(out);
            MultipartFile mf = fileWrapper.getMultipartFile();
            fileWrapper.setMultipartFile(new MockMultipartFile(mf.getName(),mf.getOriginalFilename(),null,out.toByteArray()));
        } catch (IOException e) {
            log.error(e.getMessage(),e);
        }
        return this;
    }

    /**
     * 缩放到指定大小
     */
    public UploadPretreatment image(int width,int height) {
        return image(th -> th.size(width,height));
    }

    /**
     * 缩放到 200*200 大小
     */
    public UploadPretreatment image() {
        return image(th -> th.size(200,200));
    }

    /**
     * 生成缩略图并进行图片处理，可以进行裁剪、旋转、缩放、水印等操作，默认输出图片格式通过 thumbnailSuffix 获取
     */
    public UploadPretreatment thumbnail(Consumer<Thumbnails.Builder<? extends InputStream>> consumer) {
        try (InputStream in = fileWrapper.getInputStream()) {
            Thumbnails.Builder<? extends InputStream> builder = Thumbnails.of(in);
            builder.outputFormat(FileNameUtil.extName(thumbnailSuffix));
            consumer.accept(builder);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            builder.toOutputStream(out);
            thumbnailBytes = out.toByteArray();
        } catch (IOException e) {
            log.error(e.getMessage(),e);
        }
        return this;
    }

    /**
     * 生成缩略图并缩放到指定大小，默认输出图片格式通过 thumbnailSuffix 获取
     */
    public UploadPretreatment thumbnail(int width,int height) {
        return thumbnail(th -> th.size(width,height));
    }

    /**
     * 生成缩略图并缩放到 200*200 大小，默认输出图片格式通过 thumbnailSuffix 获取
     */
    public UploadPretreatment thumbnail() {
        return thumbnail(200,200);
    }

    /**
     * 上传文件，成功返回文件信息，失败返回null
     */
    public FileInfo upload() {
        return fileStorageService.upload(this);
    }
}
