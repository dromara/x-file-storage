package org.dromara.x.file.storage.core.upload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 手动分片上传支持信息
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class MultipartUploadSupportInfo {
    /**
     * 是否支持手动分片上传，正常情况下判断此参数就行了
     */
    private Boolean isSupport;

    /**
     * 是否支持列举已上传的分片，又拍云 USS 不支持，建议将上传完成的分片信息通过 FileRecorder 接口保存到数据库，
     * 详情：https://x-file-storage.xuyanwu.cn/2.3.0/#/%E5%9F%BA%E7%A1%80%E5%8A%9F%E8%83%BD?id=%E4%BF%9D%E5%AD%98%E4%B8%8A%E4%BC%A0%E8%AE%B0%E5%BD%95
     */
    private Boolean isSupportListParts;

    /**
     * 是否支持取消上传，
     * 又拍云 USS 不支持手动取消，未完成上传的文件信息及分片默认 24 小时后自动删除
     */
    private Boolean isSupportAbort;

    /**
     * 手动分片上传-列举已上传的分片-每次获取的最大分片数，对象存储一般是 1000
     */
    private Integer listPartsSupportMaxParts;

    /**
     * 不支持手动分片上传
     */
    public static MultipartUploadSupportInfo notSupport() {
        return new MultipartUploadSupportInfo(false, false, false, null);
    }

    /**
     * 支持全部的手动分片上传功能
     */
    public static MultipartUploadSupportInfo supportAll() {
        return new MultipartUploadSupportInfo(true, true, true, 1000);
    }
}
