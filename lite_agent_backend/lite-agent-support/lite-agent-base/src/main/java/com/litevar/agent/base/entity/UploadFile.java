package com.litevar.agent.base.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionName;
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
public class UploadFile {
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    private String name;
    private String fileKey;
    private String mimeType;
    private String extension;
    private String storageType = "local";
    private Integer size;

    /**
     * MD5 hash of the file
     */
    private String md5Hash;

    private String createBy;

    /**
     * 创建时间
     */
    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
