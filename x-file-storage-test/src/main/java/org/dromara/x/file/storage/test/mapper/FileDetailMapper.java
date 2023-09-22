package org.dromara.x.file.storage.test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dromara.x.file.storage.test.model.FileDetail;

@Mapper
public interface FileDetailMapper extends BaseMapper<FileDetail> {
}
