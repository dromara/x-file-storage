package org.dromara.x.file.storage.core.constant;


public interface Constant {

    /**
     * 文件的访问控制列表，一般情况下只有对象存储支持该功能
     */
    interface ACL {
        /**
         * 私有
         */
        String PRIVATE = "private";
        /**
         * 公共读
         */
        String PUBLIC_READ = "public-read";
        /**
         * 公共读写
         */
        String PUBLIC_READ_WRITE = "public-read-write";
    }

    /**
     * 阿里云 OSS 的 ACL
     * {@link com.aliyun.oss.model.CannedAccessControlList}
     */
    interface AliyunOssACL extends ACL {
        String DEFAULT = "default";
    }


    /**
     * Aws S3 的 ACL
     * {@link com.amazonaws.services.s3.model.CannedAccessControlList}
     */
    interface AwsS3ACL extends ACL {
        String AUTHENTICATED_READ = "authenticated-read";
        String LOG_DELIVERY_WRITE = "log-delivery-write";
        String BUCKET_OWNER_READ = "bucket-owner-read";
        String BUCKET_OWNER_FULL_CONTROL = "bucket-owner-full-control";
        String AWS_EXEC_READ = "aws-exec-read";
    }

    /**
     * 华为云 OBS 的 ACL
     * {@link com.obs.services.model.AccessControlList}
     * {@link com.obs.services.internal.IConvertor#transCannedAcl(String)}
     */
    interface HuaweiObsACL extends ACL {
        String PUBLIC_READ_DELIVERED = "public-read-delivered";
        String PUBLIC_READ_WRITE_DELIVERED = "public-read-write-delivered";
        String AUTHENTICATED_READ = "authenticated-read";
        String BUCKET_OWNER_READ = "bucket-owner-read";
        String BUCKET_OWNER_FULL_CONTROL = "bucket-owner-full-control";
        String LOG_DELIVERY_WRITE = "log-delivery-write";
    }

    /**
     * 百度云 BOS 的 ACL
     * {@link com.baidubce.services.bos.model.CannedAccessControlList}
     */
    interface BaiduBosACL extends ACL {

    }


    /**
     * 腾讯云 COS 的 ACL
     * {@link com.qcloud.cos.model.CannedAccessControlList}
     */
    interface TencentCosACL extends ACL {
        String DEFAULT = "default";
    }

    /**
     * GoogleCloud Storage 的 ACL（已经做了命名规则转换）
     * {@link com.google.cloud.storage.Storage.PredefinedAcl}
     * 如果这里的预定义ACL满足不了要求，也可以使用 {@link com.google.cloud.storage.Acl}
     * 文档：https://cloud.google.com/storage/docs/access-control/lists?hl=zh-cn
     */
    interface GoogleCloudStorageACL extends ACL {
        String AUTHENTICATED_READ = "authenticated-read";
        String ALL_AUTHENTICATED_USERS = "all-authenticated-users";
        String PROJECT_PRIVATE = "project-private";
        String BUCKET_OWNER_READ = "bucket-owner-read";
        String BUCKET_OWNER_FULL_CONTROL = "bucket-owner-full-control";
    }
}
