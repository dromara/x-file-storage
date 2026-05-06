package org.dromara.x.file.storage.test.boot4.controller;

import cn.hutool.core.lang.Assert;
import cn.hutool.crypto.SecureUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.file.HttpServletRequestFileWrapper;
import org.dromara.x.file.storage.core.file.MultipartFormDataReader;
import org.dromara.x.file.storage.core.hash.HashInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
public class FileDetailController {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 上传文件，成功返回文件 url
     */
    @PostMapping("/upload")
    public String upload(MultipartFile file) {
        FileInfo fileInfo = fileStorageService
                .of(file)
                .setPath("upload/")
                .setObjectId("0")
                .setObjectType("0")
                .upload();
        return fileInfo == null ? "上传失败！" : fileInfo.getUrl();
    }

    /**
     * 上传图片，成功返回文件信息
     */
    @PostMapping("/upload-image")
    public FileInfo uploadImage(MultipartFile file) {
        return fileStorageService
                .of(file)
                .image(img -> img.size(1000, 1000))
                .thumbnail(th -> th.size(200, 200))
                .upload();
    }

    /**
     * 直接读取 HttpServletRequest 中的文件进行上传，验证 Jakarta 适配器路径
     */
    @PostMapping("/upload-request")
    public FileInfo uploadFromRequest(HttpServletRequest request) {
        HttpServletRequestFileWrapper wrapper = (HttpServletRequestFileWrapper) fileStorageService.wrapper(request);
        MultipartFormDataReader.MultipartFormData formData = wrapper.getMultipartFormData();
        Map<String, String[]> parameterMap = formData.getParameterMap();
        log.info("parameterMap：{}", parameterMap);
        return fileStorageService.of(wrapper).upload();
    }

    /**
     * 上传时计算 MD5 校验
     */
    @PostMapping("/upload-hash")
    public FileInfo uploadHash(MultipartFile file) throws IOException {
        FileInfo fileInfo = fileStorageService.of(file).setHashCalculatorMd5().upload();
        HashInfo hashInfo = fileInfo.getHashInfo();
        Assert.isTrue(SecureUtil.md5().digestHex(file.getBytes()).equals(hashInfo.getMd5()), "文件 MD5 对比不一致！");
        log.info("文件 MD5 对比通过");
        return fileInfo;
    }
}
