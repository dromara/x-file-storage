package cn.xuyanwu.spring.file.storage.test.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.xuyanwu.spring.file.storage.test.mapper.FileDetailMapper;
import cn.xuyanwu.spring.file.storage.test.model.FileDetail;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.recorder.FileRecorder;

/**
 * 用来将文件上传记录保存到数据库
 */
@Service
public class FileDetailService extends ServiceImpl<FileDetailMapper, FileDetail> implements FileRecorder {


    @Override
    public boolean record(FileInfo info) {
        FileDetail detail = BeanUtil.copyProperties(info,FileDetail.class);
        boolean b = save(detail);
        if (b) {
            info.setId(detail.getId());
        }
        return b;
    }

    @Override
    public FileInfo getByUrl(String url) {
        return BeanUtil.copyProperties(getOne(new QueryWrapper<FileDetail>().eq(FileDetail.COL_URL,url)),FileInfo.class);
    }

    @Override
    public boolean delete(String url) {
        return remove(new QueryWrapper<FileDetail>().eq(FileDetail.COL_URL,url));
    }
}


