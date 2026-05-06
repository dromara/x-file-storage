package org.dromara.x.file.storage.test.boot4.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Map;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.hash.HashInfo;
import org.dromara.x.file.storage.core.recorder.FileRecorder;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.dromara.x.file.storage.test.boot4.mapper.FileDetailMapper;
import org.dromara.x.file.storage.test.boot4.model.FileDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 用来将文件上传记录保存到数据库
 */
@Service
public class FileDetailService extends ServiceImpl<FileDetailMapper, FileDetail> implements FileRecorder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private FilePartDetailService filePartDetailService;

    @Override
    public boolean save(FileInfo info) {
        FileDetail detail = toFileDetail(info);
        boolean b = save(detail);
        if (b) {
            info.setId(detail.getId());
        }
        return b;
    }

    @Override
    public void update(FileInfo info) {
        FileDetail detail = toFileDetail(info);
        QueryWrapper<FileDetail> qw = new QueryWrapper<FileDetail>()
                .eq(detail.getUrl() != null, FileDetail.COL_URL, detail.getUrl())
                .eq(detail.getId() != null, FileDetail.COL_ID, detail.getId());
        update(detail, qw);
    }

    @Override
    public FileInfo getByUrl(String url) {
        return toFileInfo(getOne(new QueryWrapper<FileDetail>().eq(FileDetail.COL_URL, url)));
    }

    @Override
    public boolean delete(String url) {
        remove(new QueryWrapper<FileDetail>().eq(FileDetail.COL_URL, url));
        return true;
    }

    @Override
    public void saveFilePart(FilePartInfo filePartInfo) {
        filePartDetailService.saveFilePart(filePartInfo);
    }

    @Override
    public void deleteFilePartByUploadId(String uploadId) {
        filePartDetailService.deleteFilePartByUploadId(uploadId);
    }

    public FileDetail toFileDetail(FileInfo info) {
        FileDetail detail = BeanUtil.copyProperties(
                info, FileDetail.class, "metadata", "userMetadata", "thMetadata", "thUserMetadata", "attr", "hashInfo");
        detail.setMetadata(valueToJson(info.getMetadata()));
        detail.setUserMetadata(valueToJson(info.getUserMetadata()));
        detail.setThMetadata(valueToJson(info.getThMetadata()));
        detail.setThUserMetadata(valueToJson(info.getThUserMetadata()));
        detail.setAttr(valueToJson(info.getAttr()));
        detail.setHashInfo(valueToJson(info.getHashInfo()));
        return detail;
    }

    public FileInfo toFileInfo(FileDetail detail) {
        FileInfo info = BeanUtil.copyProperties(
                detail, FileInfo.class, "metadata", "userMetadata", "thMetadata", "thUserMetadata", "attr", "hashInfo");
        info.setMetadata(jsonToMetadata(detail.getMetadata()));
        info.setUserMetadata(jsonToMetadata(detail.getUserMetadata()));
        info.setThMetadata(jsonToMetadata(detail.getThMetadata()));
        info.setThUserMetadata(jsonToMetadata(detail.getThUserMetadata()));
        info.setAttr(jsonToDict(detail.getAttr()));
        info.setHashInfo(jsonToHashInfo(detail.getHashInfo()));
        return info;
    }

    public String valueToJson(Object value) {
        if (value == null) return null;
        return objectMapper.writeValueAsString(value);
    }

    public Map<String, String> jsonToMetadata(String json) {
        if (StrUtil.isBlank(json)) return null;
        return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
    }

    public Dict jsonToDict(String json) {
        if (StrUtil.isBlank(json)) return null;
        return objectMapper.readValue(json, Dict.class);
    }

    public HashInfo jsonToHashInfo(String json) {
        if (StrUtil.isBlank(json)) return null;
        return objectMapper.readValue(json, HashInfo.class);
    }
}
