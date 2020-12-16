package cn.xuyanwu.spring.file.storage.test.controller;

import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
public class FileDetailController {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 可以用 postman 请求这个接口进行手动测试
     */
    @PostMapping("/upload")
    public String upload(MultipartFile file) {
        FileInfo fileInfo = fileStorageService.of(file).setPath("upload/").setObjectId("0").setObjectType("0").upload();
        return fileInfo == null ? "上传失败！" : fileInfo.getUrl();
    }
}
