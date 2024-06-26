package org.dromara.x.file.storage.test.controller;

import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.file.HttpServletRequestFileWrapper;
import org.dromara.x.file.storage.core.file.MultipartFormDataReader;
import org.dromara.x.file.storage.core.hash.MessageDigestHashCalculator;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        FileInfo fileInfo = fileStorageService
                .of(file)
                .setPath("upload/") // 保存到相对路径下，为了方便管理，不需要可以不写
                .setObjectId("0") // 关联对象id，为了方便管理，不需要可以不写
                .setObjectType("0") // 关联对象类型，为了方便管理，不需要可以不写
                .upload(); // 将文件上传到对应地方
        return fileInfo == null ? "上传失败！" : fileInfo.getUrl();
    }

    /**
     * 上传图片，成功返回文件信息
     * 图片处理使用的是 https://github.com/coobird/thumbnailator
     */
    @PostMapping("/upload-image")
    public FileInfo uploadImage(MultipartFile file) {
        return fileStorageService
                .of(file)
                .image(img -> img.size(1000, 1000)) // 将图片大小调整到 1000*1000
                .thumbnail(th -> th.size(200, 200)) // 再生成一张 200*200 的缩略图
                .upload();
    }

    /**
     * 上传文件到指定存储平台，成功返回文件信息
     */
    @PostMapping("/upload-platform")
    public FileInfo uploadPlatform(MultipartFile file) {
        return fileStorageService
                .of(file)
                .setPlatform("aliyun-oss-1") // 使用指定的存储平台
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
        log.info("parameterMap：{}", parameterMap);
        return fileStorageService.of(wrapper).upload();
    }

    /**
     * partUpload hash值异常问题验证
     */
    @PostMapping("/upload-part")
    public String uploadPart(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return "empty file";
        }
        int parts = 3;
        List<byte[]> filePartList = splitFile(file, parts);

        for (int i = 0; i < filePartList.size(); i++) {
            // init
            FileInfo fileInfo = new FileInfo();
            fileInfo.setPlatform("local-plus-1");
            fileInfo.setBasePath("/123");
            fileInfo.setPath("/456");
            fileInfo.setFilename("789");
            fileInfo.setUploadId(String.valueOf(i));

            // hash验证
            byte[] sourceArr = filePartList.get(i);
            MessageDigestHashCalculator md5 = new MessageDigestHashCalculator("MD5");
            MessageDigestHashCalculator sha256 = new MessageDigestHashCalculator("SHA-256");
            md5.update(sourceArr);
            sha256.update(sourceArr);
            log.info("[{}]===> before load, md5: {},sha256: {}", i, md5.getValue(), sha256.getValue());

            // upload
            FilePartInfo upload = fileStorageService.uploadPart(fileInfo, i, sourceArr)
                    .setHashCalculatorSha256()
                    .upload();
            log.info("[{}]<=== after load, hash: {}", i, upload.getHashInfo());
        }
        return "ok";
    }

    /**
     * 将 MultipartFile 分成 n 份
     *
     * @param file 要分割的 MultipartFile
     * @param n    分割的份数
     * @return 分割后的文件内容列表
     * @throws IOException .
     */
    private List<byte[]> splitFile(MultipartFile file, int n) throws IOException {
        long totalBytes = file.getSize();
        long bytesPerSplit = totalBytes / n;
        long remainingBytes = totalBytes % n;

        byte[] fileData = file.getBytes();
        List<byte[]> parts = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            int start = (int) (i * bytesPerSplit);
            int end = (int) (start + bytesPerSplit + (i == n - 1 ? remainingBytes : 0));

            byte[] partData = new byte[end - start];
            System.arraycopy(fileData, start, partData, 0, end - start);
            parts.add(partData);
        }

        return parts;
    }
}
