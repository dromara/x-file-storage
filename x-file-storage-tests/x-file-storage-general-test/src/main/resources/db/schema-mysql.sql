/**
  这里存放着测试用到的表结构
 */

-- ----------------------------
-- Table structure for file_detail
-- ----------------------------
DROP TABLE IF EXISTS `file_detail`;
CREATE TABLE `file_detail`
(
    `id`                varchar(32)  NOT NULL COMMENT '文件id',
    `url`               varchar(512) NOT NULL COMMENT '文件访问地址',
    `size`              bigint(20)   DEFAULT NULL COMMENT '文件大小，单位字节',
    `filename`          varchar(256) DEFAULT NULL COMMENT '文件名称',
    `original_filename` varchar(256) DEFAULT NULL COMMENT '原始文件名',
    `base_path`         varchar(256) DEFAULT NULL COMMENT '基础存储路径',
    `path`              varchar(256) DEFAULT NULL COMMENT '存储路径',
    `ext`               varchar(32)  DEFAULT NULL COMMENT '文件扩展名',
    `content_type`      varchar(128) DEFAULT NULL COMMENT 'MIME类型',
    `platform`          varchar(32)  DEFAULT NULL COMMENT '存储平台',
    `th_url`            varchar(512) DEFAULT NULL COMMENT '缩略图访问路径',
    `th_filename`       varchar(256) DEFAULT NULL COMMENT '缩略图名称',
    `th_size`           bigint(20)   DEFAULT NULL COMMENT '缩略图大小，单位字节',
    `th_content_type`   varchar(128) DEFAULT NULL COMMENT '缩略图MIME类型',
    `object_id`         varchar(32)  DEFAULT NULL COMMENT '文件所属对象id',
    `object_type`       varchar(32)  DEFAULT NULL COMMENT '文件所属对象类型，例如用户头像，评价图片',
    `metadata`          text COMMENT '文件元数据',
    `user_metadata`     text COMMENT '文件用户元数据',
    `th_metadata`       text COMMENT '缩略图元数据',
    `th_user_metadata`  text COMMENT '缩略图用户元数据',
    `attr`              text COMMENT '附加属性',
    `file_acl`          varchar(32)  DEFAULT NULL COMMENT '文件ACL',
    `th_file_acl`       varchar(32)  DEFAULT NULL COMMENT '缩略图文件ACL',
    `hash_info`         text COMMENT '哈希信息',
    `upload_id`         varchar(128) DEFAULT NULL COMMENT '上传ID，仅在手动分片上传时使用',
    `upload_status`     int(11)      DEFAULT NULL COMMENT '上传状态，仅在手动分片上传时使用，1：初始化完成，2：上传完成',
    `create_time`       datetime     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8 ROW_FORMAT = DYNAMIC COMMENT ='文件记录表';

-- ----------------------------
-- Table structure for file_part_detail
-- ----------------------------
DROP TABLE IF EXISTS `file_part_detail`;
CREATE TABLE `file_part_detail`
(
    `id`          varchar(32) NOT NULL COMMENT '分片id',
    `platform`    varchar(32)  DEFAULT NULL COMMENT '存储平台',
    `upload_id`   varchar(128) DEFAULT NULL COMMENT '上传ID，仅在手动分片上传时使用',
    `e_tag`       varchar(255) DEFAULT NULL COMMENT '分片 ETag',
    `part_number` int(11)      DEFAULT NULL COMMENT '分片号。每一个上传的分片都有一个分片号，一般情况下取值范围是1~10000',
    `part_size`   bigint(20)   DEFAULT NULL COMMENT '文件大小，单位字节',
    `hash_info`   text CHARACTER SET utf8 COMMENT '哈希信息',
    `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8 COMMENT ='文件分片信息表，仅在手动分片上传时使用';
