package com.litevar.wechat.adapter.common.mongoplus.config;

import com.litevar.wechat.adapter.common.auth.utils.LoginUtil;
import com.mongoplus.handlers.MetaObjectHandler;
import com.mongoplus.mapping.MongoConverter;
import com.mongoplus.model.AutoFillMetaObject;
import com.mongoplus.strategy.conversion.ConversionStrategy;
import com.mongoplus.strategy.mapping.MappingStrategy;
import com.mongoplus.toolkit.InstantUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * @author Teoan
 * @since 2024/11/25 11:55
 */
@Configuration
public class MongoPlusConfigure {

    /**
     * 自动填充器
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        final String createTime = "create_time";
        final String updateTime = "update_time";
        final String createBy = "create_by";
        final String updateBy = "update_by";
        return new MetaObjectHandler() {
            @Override
            public void insertFill(AutoFillMetaObject insertAutoFillMetaObject) {
                insertAutoFillMetaObject.fillValue(createTime, now());
                insertAutoFillMetaObject.fillValue(createBy, LoginUtil.getCurrentUserId());
                insertAutoFillMetaObject.fillValue(updateTime, now());
                insertAutoFillMetaObject.fillValue(updateBy, LoginUtil.getCurrentUserId());
            }

            @Override
            public void updateFill(AutoFillMetaObject updateAutoFillMetaObject) {
                updateAutoFillMetaObject.fillValue(updateTime, now());
                updateAutoFillMetaObject.fillValue(updateBy, LoginUtil.getCurrentUserId());
            }
        };
    }

    /**
     * 写数据库时手动将时间转为UTC
     */
    private LocalDateTime now() {
        return LocalDateTime.now().atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    @Bean
    public ZoneLocalDateTimeConversionStrategy zoneLocalDateTimeConversionStrategy() {
        return new ZoneLocalDateTimeConversionStrategy();
    }

    @Bean
    public ZoneLocalDateTimeMappingStrategy zoneLocalDateTimeMappingStrategy() {
        return new ZoneLocalDateTimeMappingStrategy();
    }

    /**
     * 读数据库后,将UTC转为当前系统时区时间
     */
    public static class ZoneLocalDateTimeConversionStrategy implements ConversionStrategy<LocalDateTime> {
        @Override
        public LocalDateTime convertValue(Object fieldValue, Class<?> fieldType, MongoConverter mongoConverter) throws IllegalAccessException {
            LocalDateTime time = fieldValue.getClass().equals(Long.class) ?
                    InstantUtil.convertTimestampToLocalDateTime((Long) fieldValue) :
                    InstantUtil.convertTimestampToLocalDateTime8(((Date) fieldValue).toInstant());
            //将UTC时间转为CST时间
            return time.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        }
    }

    /**
     * 写数据库前,将当前时间转为UTC时间
     */
    public static class ZoneLocalDateTimeMappingStrategy implements MappingStrategy<LocalDateTime> {
        @Override
        public Object mapping(LocalDateTime fieldValue) throws IllegalAccessException {
            return Date.from(fieldValue.atZone(ZoneId.systemDefault())
                    .withZoneSameInstant(ZoneOffset.UTC).toInstant());
        }
    }
}