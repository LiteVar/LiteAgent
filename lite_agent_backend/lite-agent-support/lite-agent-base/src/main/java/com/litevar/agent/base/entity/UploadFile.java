package com.litevar.agent.base.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.annotation.index.MongoCompoundIndex;
import com.mongoplus.enums.FieldFill;
import com.mongoplus.enums.IdTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author reid
 * @since 3/24/25
 */

@Data
@CollectionName("upload_file")
@MongoCompoundIndex(value = "{'$datasetId': 1, '$md5Hash': 1}", unique = true)
public class UploadFile {
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    private String userId;
    private String datasetId;
    private String name;
    private String path;
    private String markdownName;
    private String markdownPath;
    private String mimeType;
    private String extension;
    private String storageType = "local";
    private Long size;

    /**
     * MD5 hash of the file
     */
    private String md5Hash;

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
