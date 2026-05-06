package org.dromara.x.file.storage.test.boot4.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.dromara.x.file.storage.test.boot4.mapper.FilePartDetailMapper;
import org.dromara.x.file.storage.test.boot4.model.FilePartDetail;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * 用来将文件分片上传记录保存到数据库，仅在手动分片上传时使用
 */
@Service
public class FilePartDetailService extends ServiceImpl<FilePartDetailMapper, FilePartDetail> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void saveFilePart(FilePartInfo info) {
        FilePartDetail detail = toFilePartDetail(info);
        if (save(detail)) {
            info.setId(detail.getId());
        }
    }

    public void deleteFilePartByUploadId(String uploadId) {
        remove(new QueryWrapper<FilePartDetail>().eq(FilePartDetail.COL_UPLOAD_ID, uploadId));
    }

    public FilePartDetail toFilePartDetail(FilePartInfo info) {
        FilePartDetail detail = new FilePartDetail();
        detail.setPlatform(info.getPlatform());
        detail.setUploadId(info.getUploadId());
        detail.setETag(info.getETag());
        detail.setPartNumber(info.getPartNumber());
        detail.setPartSize(info.getPartSize());
        detail.setHashInfo(valueToJson(info.getHashInfo()));
        detail.setCreateTime(info.getCreateTime());
        return detail;
    }

    public String valueToJson(Object value) {
        if (value == null) return null;
        return objectMapper.writeValueAsString(value);
    }
}
