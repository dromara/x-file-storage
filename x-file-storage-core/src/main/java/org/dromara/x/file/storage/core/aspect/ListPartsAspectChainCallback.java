package org.dromara.x.file.storage.core.aspect;

import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.upload.FilePartInfoList;
import org.dromara.x.file.storage.core.upload.ListPartsPretreatment;

/**
 * 手动分片上传-列举已上传的分片切面调用链结束回调
 */
public interface ListPartsAspectChainCallback {
    FilePartInfoList run(ListPartsPretreatment pre, FileStorage fileStorage);
}
