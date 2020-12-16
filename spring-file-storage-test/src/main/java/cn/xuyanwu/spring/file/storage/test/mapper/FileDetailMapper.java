package cn.xuyanwu.spring.file.storage.test.mapper;

import cn.xuyanwu.spring.file.storage.test.model.FileDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileDetailMapper extends BaseMapper<FileDetail> {
}