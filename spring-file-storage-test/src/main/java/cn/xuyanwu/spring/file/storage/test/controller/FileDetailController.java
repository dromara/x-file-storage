package cn.xuyanwu.spring.file.storage.test.controller;

import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.FileStorageService;
import cn.xuyanwu.spring.file.storage.file.HttpServletRequestFileWrapper;
import cn.xuyanwu.spring.file.storage.file.MultipartFormDataReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;


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
        FileInfo fileInfo = fileStorageService.of(file)
                .setPath("upload/") //保存到相对路径下，为了方便管理，不需要可以不写
                .setObjectId("0")   //关联对象id，为了方便管理，不需要可以不写
                .setObjectType("0") //关联对象类型，为了方便管理，不需要可以不写
                .upload();  //将文件上传到对应地方
        return fileInfo == null ? "上传失败！" : fileInfo.getUrl();
    }

    /**
     * 上传图片，成功返回文件信息
     * 图片处理使用的是 https://github.com/coobird/thumbnailator
     */
    @PostMapping("/upload-image")
    public FileInfo uploadImage(MultipartFile file) {
        return fileStorageService.of(file)
                .image(img -> img.size(1000,1000))  //将图片大小调整到 1000*1000
                .thumbnail(th -> th.size(200,200))  //再生成一张 200*200 的缩略图
                .upload();
    }

    /**
     * 上传文件到指定存储平台，成功返回文件信息
     */
    @PostMapping("/upload-platform")
    public FileInfo uploadPlatform(MultipartFile file) {
        return fileStorageService.of(file)
                .setPlatform("aliyun-oss-1")    //使用指定的存储平台
                .upload();
    }

    /**
     * 直接读取 HttpServletRequest 中的文件进行上传，成功返回文件信息
     */
    @PostMapping("/upload-request")
    public FileInfo uploadPlatform(HttpServletRequest request) {
        HttpServletRequestFileWrapper wrapper = (HttpServletRequestFileWrapper) fileStorageService.wrapper(request);
        MultipartFormDataReader.MultipartFormData formData = wrapper.getMultipartFormData();
        Map<String, String[]> parameterMap = formData.getParameterMap();
        log.info("parameterMap：{}",parameterMap);
        return fileStorageService.of(wrapper).upload();
    }
}
