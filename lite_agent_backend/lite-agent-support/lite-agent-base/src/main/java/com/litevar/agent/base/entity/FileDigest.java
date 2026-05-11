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
 * 文件摘要信息
 *
 * @author uncle
 * @since 2025/12/31 10:24
 */
@Data
@CollectionName("file_digest")
@MongoCompoundIndex(value = "{'$filePath': 1, '$fileHash':1}", unique = true)
public class FileDigest {
    @ID(type = IdTypeEnum.ASSIGN_ID)
    private String id;

    /**
     * 文件路径(不包括文件名)
     */
    private String filePath;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件哈希值
     */
    private String fileHash;

    /**
     * 文件访问key
     */
    private String fileKey;

    /**
     * 文件上传时间
     */
    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
