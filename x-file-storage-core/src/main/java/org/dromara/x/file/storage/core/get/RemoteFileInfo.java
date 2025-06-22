package org.dromara.x.file.storage.core.get;

import cn.hutool.core.map.MapProxy;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.azure.storage.blob.implementation.models.BlobItemInternal;
import com.azure.storage.blob.models.BlobProperties;
import com.baidubce.services.bos.model.BosObject;
import com.baidubce.services.bos.model.BosObjectSummary;
import com.github.sardine.DavResource;
import com.google.cloud.storage.Blob;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.obs.services.model.ObsObject;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectSummary;
import com.volcengine.tos.model.object.GetObjectV2Output;
import com.volcengine.tos.model.object.ListedCommonPrefix;
import com.volcengine.tos.model.object.ListedObjectV2;
import io.minio.StatObjectResponse;
import io.minio.messages.Contents;
import java.io.File;
import java.util.Date;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import okhttp3.Response;
import org.apache.commons.net.ftp.FTPFile;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.platform.FastDfsFileStorage.FastDfsFileInfo;
import org.dromara.x.file.storage.core.platform.GoFastDfsFileStorageClientFactory.GoFastDfsClient.GetFileInfo.GetFileInfoData;
import org.dromara.x.file.storage.core.platform.GoFastDfsFileStorageClientFactory.GoFastDfsClient.ListFileInfo.ListFileInfoDataItem;
import org.dromara.x.file.storage.core.util.KebabCaseInsensitiveMap;
import org.dromara.x.file.storage.core.util.Tools;
import org.json.JSONObject;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

/**
 * 远程文件信息
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class RemoteFileInfo {
    /**
     * 存储平台
     */
    private String platform;
    /**
     * 基础存储路径
     */
    private String basePath;
    /**
     * 存储路径
     */
    private String path;
    /**
     * 文件名称
     */
    private String filename;
    /**
     * 文件访问地址
     */
    private String url;
    /**
     * 文件大小，单位字节
     */
    private Long size;
    /**
     * 文件扩展名
     */
    private String ext;
    /**
     * 文件 ETag，仅部分存储平台支持
     */
    private String eTag;
    /**
     * Content-Disposition，仅部分存储平台支持
     */
    private String contentDisposition;
    /**
     * MIME 类型，仅部分存储平台支持
     */
    private String contentType;
    /**
     * MD5，仅部分存储平台支持
     */
    private String contentMd5;
    /**
     * 最后修改时间
     */
    private Date lastModified;
    /**
     * 文件元数据，仅部分存储平台支持
     */
    private Map<String, Object> metadata;
    /**
     * 文件用户元数据，仅部分存储平台支持
     */
    private Map<String, Object> userMetadata;
    /**
     * 原始数据
     */
    private Object original;

    /**
     * 获取短横命名风格且不区分大小写的文件元数据，以下方式都获得的值相同，put进入的值也会被覆盖<br>
     * get("ContentType")<br>
     * get("Content_Type")<br>
     * get("Content-Type")<br>
     * get("contentType")<br>
     */
    public MapProxy getKebabCaseInsensitiveMetadata() {
        if (metadata == null) return null;
        return new MapProxy(new KebabCaseInsensitiveMap<>(metadata));
    }

    /**
     * 获取短横命名风格的文件用户元数据，以下方式都获得的值相同，put进入的值也会被覆盖<br>
     * get("ContentType")<br>
     * get("Content_Type")<br>
     * get("Content-Type")<br>
     * get("contentType")<br>
     */
    public MapProxy getKebabCaseInsensitiveUserMetadata() {
        if (userMetadata == null) return null;
        return new MapProxy(new KebabCaseInsensitiveMap<>(userMetadata));
    }

    /**
     * 获取原始数据并转换为指定类型
     */
    public <T> T getOriginal(Class<T> clazz) {
        if (original == null) return null;
        if (clazz.isInstance(original)) {
            return Tools.cast(original);
        }
        return null;
    }

    /**
     * 获取阿里云 OSS 存储平台的文件原始数据，失败返回 null，
     * 仅在获取文件的返回值中使用
     */
    public OSSObject getOriginalAliyunOssObject() {
        return getOriginal(OSSObject.class);
    }

    /**
     * 获取阿里云 OSS 存储平台的文件原始数据，失败返回 null，
     * 仅在列举文件的返回值中使用
     */
    public OSSObjectSummary getOriginalAliyunOssObjectSummary() {
        return getOriginal(OSSObjectSummary.class);
    }

    /**
     * 获取 Amazon S3 存储平台的文件原始数据，失败返回 null，
     * 仅在获取文件的返回值中使用
     */
    public S3Object getOriginalAmazonS3Object() {
        return getOriginal(S3Object.class);
    }

    /**
     * 获取 Amazon S3 存储平台的文件原始数据，失败返回 null，
     * 仅在列举文件的返回值中使用
     */
    public S3ObjectSummary getOriginalAmazonS3ObjectSummary() {
        return getOriginal(S3ObjectSummary.class);
    }

    /**
     * 获取 Amazon S3 V2 存储平台的文件原始数据，失败返回 null，
     * 仅在获取文件的返回值中使用
     */
    public HeadObjectResponse getOriginalAmazonS3V2HeadObjectResponse() {
        return getOriginal(HeadObjectResponse.class);
    }
    /**
     * 获取 Amazon S3 V2 存储平台的文件原始数据，失败返回 null，
     * 仅在列举文件的返回值中使用
     */
    public S3Object getOriginalAmazonS3V2S3Object() {
        return getOriginal(S3Object.class);
    }

    /**
     * 获取 Amazon S3 V2 存储平台的文件原始数据，失败返回 null，
     * 仅在列举文件的返回值中使用
     */
    public CommonPrefix getOriginalAmazonS3V2CommonPrefix() {
        return getOriginal(CommonPrefix.class);
    }

    /**
     * 获取 Azure Blob Storage 存储平台的文件原始数据，失败返回 null，
     * 仅在获取文件的返回值中使用
     */
    public BlobProperties getOriginalAzureBlobStorageBlobProperties() {
        return getOriginal(BlobProperties.class);
    }

    /**
     * 获取 Azure Blob Storage 存储平台的文件原始数据，失败返回 null，
     * 仅在列举文件的返回值中使用
     */
    public BlobItemInternal getOriginalAzureBlobStorageBlobItemInternal() {
        return getOriginal(BlobItemInternal.class);
    }

    /**
     * 获取百度云 BOS 存储平台的文件原始数据，失败返回 null，
     * 仅在获取文件的返回值中使用
     */
    public BosObject getOriginalBaiduBosObject() {
        return getOriginal(BosObject.class);
    }

    /**
     * 获取百度云 BOS 存储平台的文件原始数据，失败返回 null，
     * 仅在列举文件的返回值中使用
     */
    public BosObjectSummary getOriginalBaiduBosObjectSummary() {
        return getOriginal(BosObjectSummary.class);
    }

    /**
     * 获取 FastDFS 存储平台的文件原始数据，失败返回 null
     */
    public FastDfsFileInfo getOriginalFastDfs() {
        return getOriginal(FastDfsFileInfo.class);
    }

    /**
     * 获取 FTP 存储平台的文件原始数据，失败返回 null
     */
    public FTPFile getOriginalFtp() {
        return getOriginal(FTPFile.class);
    }

    /**
     * 获取 Google Cloud Storage 存储平台的文件原始数据，失败返回 null
     */
    public Blob getOriginalGoogleCloudStorage() {
        return getOriginal(Blob.class);
    }

    /**
     * 获取华为云 OBS 存储平台的文件原始数据，失败返回 null
     */
    public ObsObject getOriginalHuaweiObs() {
        return getOriginal(ObsObject.class);
    }

    /**
     * 获取本地存储平台（升级版）的文件原始数据，失败返回 null
     */
    public File getOriginalLocal() {
        return getOriginal(File.class);
    }

    /**
     * 获取 Minio 存储平台的文件原始数据，失败返回 null，
     * 仅在获取文件的返回值中使用
     */
    public StatObjectResponse getOriginalMinioStatObjectResponse() {
        return getOriginal(StatObjectResponse.class);
    }

    /**
     * 获取 Minio 存储平台的文件原始数据，失败返回 null，
     * 仅在列举文件的返回值中使用
     */
    public Contents getOriginalMinioContents() {
        return getOriginal(Contents.class);
    }

    /**
     * 获取七牛云 Kodo 存储平台的文件原始数据，失败返回 null
     */
    public com.qiniu.storage.model.FileInfo getOriginalQiniuKodo() {
        return getOriginal(com.qiniu.storage.model.FileInfo.class);
    }

    /**
     * 获取 SFTP 存储平台的文件原始数据，失败返回 null
     */
    public LsEntry getOriginalSftp() {
        return getOriginal(LsEntry.class);
    }

    /**
     * 获取腾讯云 COS 存储平台的文件原始数据，失败返回 null，
     * 仅在获取文件的返回值中使用
     */
    public COSObject getOriginalTencentCosObject() {
        return getOriginal(COSObject.class);
    }

    /**
     * 获取腾讯云 COS 存储平台的文件原始数据，失败返回 null，
     * 仅在列举文件的返回值中使用
     */
    public COSObjectSummary getOriginalTencentCosObjectSummary() {
        return getOriginal(COSObjectSummary.class);
    }

    /**
     * 获取又拍云 USS 存储平台的文件原始数据，失败返回 null，
     * 仅在获取文件的返回值中使用
     */
    public Response getOriginalUpyunUssResponse() {
        return getOriginal(Response.class);
    }

    /**
     * 获取又拍云 USS 存储平台的文件原始数据，失败返回 null，
     * 仅在列举文件的返回值中使用
     */
    public JSONObject getOriginalUpyunUssJSONObject() {
        return getOriginal(JSONObject.class);
    }

    /**
     * 获取 WebDAV 存储平台的文件原始数据，失败返回 null
     */
    public DavResource getOriginalWebDav() {
        return getOriginal(DavResource.class);
    }

    /**
     * 获取 Mongo GridFS 存储平台的文件原始数据，失败返回 null
     */
    public GridFSFile getOriginalMongoGridFs() {
        return getOriginal(GridFSFile.class);
    }

    /**
     * 获取 go-fastdfs 存储平台的文件原始数据，失败返回 null,
     * 仅在获取文件的返回值中使用
     */
    public GetFileInfoData getOriginalGoFastDfsGetFileInfoData() {
        return getOriginal(GetFileInfoData.class);
    }

    /**
     * 获取 go-fastdfs 存储平台的文件原始数据，失败返回 null,
     * 仅在列举文件的返回值中使用
     */
    public ListFileInfoDataItem getOriginalGoFastDfsListFileInfoDataItem() {
        return getOriginal(ListFileInfoDataItem.class);
    }

    /**
     * 获取火山引擎 TOS 存储平台的文件原始数据，失败返回 null，
     * 仅在获取文件的返回值中使用
     */
    public ListedObjectV2 getOriginalVolcengineTosListedObjectV2() {
        return getOriginal(ListedObjectV2.class);
    }

    /**
     * 获取火山引擎 TOS 存储平台的文件原始数据，失败返回 null，
     * 仅在列举文件的返回值中使用
     */
    public GetObjectV2Output getOriginalVolcengineTosGetObjectV2Output() {
        return getOriginal(GetObjectV2Output.class);
    }

    /**
     * 获取火山引擎 TOS 存储平台的文件原始数据，失败返回 null，
     * 仅在列举文件的返回值中使用
     */
    public ListedCommonPrefix getOriginalVolcengineTosListedCommonPrefix() {
        return getOriginal(ListedCommonPrefix.class);
    }

    /**
     * 转换成 FileInfo，注意 createTime 、metadata 及 userMetadata 可能需要自行处理，详情查看下方源码注释
     * @return 文件信息
     */
    public FileInfo toFileInfo() {
        return toFileInfo(new FileInfo());
    }

    /**
     * 转换成 FileInfo，注意 createTime 、metadata 及 userMetadata 可能需要自行处理，详情查看下方源码注释
     * @param fileInfo 文件信息
     * @return 文件信息
     */
    public FileInfo toFileInfo(FileInfo fileInfo) {
        fileInfo.setPlatform(platform).setBasePath(basePath).setPath(path).setFilename(filename);
        fileInfo.setUrl(url).setSize(size).setExt(ext).setContentType(contentType);
        // 一般情况下 FileInfo 中的 createTime（创建时间）就是 RemoteFileInfo 中的 lastModified（最后修改时间），
        // 如果有误，可以自行设置
        fileInfo.setCreateTime(lastModified);
        // RemoteFileInfo 中的 metadata 元数据值为 Object 类型，FileInfo 中的元数据值为 String 类型，
        // 目前是将 Object 类型使用 toString() 方法转为 String 类型，应该可以满足大部分情况，如有需要可以自行转换
        fileInfo.setMetadata(Tools.toStringMap(metadata));
        fileInfo.setUserMetadata(Tools.toStringMap(userMetadata));
        return fileInfo;
    }

    /**
     * 转换成缩略图 FileInfo，注意 metadata 及 userMetadata 可能需要自行处理，详情查看下方源码注释
     * @return 文件信息
     */
    public FileInfo toFileInfoTh() {
        return toFileInfoTh(new FileInfo());
    }

    /**
     * 转换成缩略图 FileInfo，注意不含 platform 、 basePath 及 path，如果需要，请使用 toFileInfoThAll() 方法，
     * 注意 metadata 及 userMetadata 可能需要自行处理，详情查看下方源码注释
     * @param fileInfo 文件信息
     * @return 文件信息
     */
    public FileInfo toFileInfoTh(FileInfo fileInfo) {
        fileInfo.setThFilename(filename).setThUrl(url).setThSize(size).setThContentType(contentType);
        // RemoteFileInfo 中的 metadata 元数据值为 Object 类型，FileInfo 中的元数据值为 String 类型，
        // 目前是将 Object 类型使用 toString() 方法转为 String 类型，应该可以满足大部分情况，如有需要可以自行转换
        fileInfo.setThMetadata(Tools.toStringMap(metadata));
        fileInfo.setThUserMetadata(Tools.toStringMap(userMetadata));
        return fileInfo;
    }

    /**
     * 转换成缩略图 FileInfo，注意 metadata 及 userMetadata 可能需要自行处理，详情查看下方源码注释
     * @return 文件信息
     */
    public FileInfo toFileInfoThAll() {
        return toFileInfoThAll(new FileInfo());
    }

    /**
     * 转换成缩略图 FileInfo，注意 metadata 及 userMetadata 可能需要自行处理，详情查看下方源码注释
     * @param fileInfo 文件信息
     * @return 文件信息
     */
    public FileInfo toFileInfoThAll(FileInfo fileInfo) {
        return toFileInfoTh(fileInfo)
                .setPlatform(platform)
                .setBasePath(basePath)
                .setPath(path);
    }
}
