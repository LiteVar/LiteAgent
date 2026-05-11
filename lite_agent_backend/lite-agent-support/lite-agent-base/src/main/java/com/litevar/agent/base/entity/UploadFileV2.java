package com.litevar.agent.base.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoIndex;
import com.mongoplus.enums.FieldFill;
import com.mongoplus.enums.IdTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author reid
 * @since 2026/1/23
 */

@Data
@CollectionName("upload_file_v2")
public class UploadFileV2 {
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    private String userId;
    /**
     * 知识库ID
     */
    private String datasetId;
    /**
     * 文件名
     */
    private String filename;
    /**
     * 文件访问key
     */
    private String fileKey;
    /**
     * 知识库文件转markdown后的key
     */
    private String markdownKey;
    /**
     * 文件类型mime
     */
    private String mimeType;
    /**
     * 文件扩展名
     */
    private String extension;
    /**
     * MD5 hash of the file
     */
    @MongoIndex
    private String md5Hash;
    /**
     * 存储类型: local/oss
     */
    private String storageType = "local";
    /**
     * 文件大小
     */
    private Long size;

    /**
     * 创建时间
     */
    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    @CollectionField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
