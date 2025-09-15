package com.litevar.dingtalk.adapter.common.auth.service;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.auto.SaTokenDaoByObjectFollowString;
import cn.dev33.satoken.session.SaSession;
import cn.hutool.core.util.StrUtil;
import com.litevar.dingtalk.adapter.common.auth.entity.SaTokenMongoData;
import com.mongoplus.conditions.query.QueryWrapper;
import com.mongoplus.conditions.update.UpdateWrapper;
import com.mongoplus.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Sa-Token Mongo持久化实现类
 * @author Teoan
 * @since 2025/7/28 10:06
 */
@Service
public class SaTokenMongoService extends ServiceImpl<SaTokenMongoData> implements SaTokenDaoByObjectFollowString {

    LocalDateTime getExpireAtFromTimeout(long timeout) {
        // 当接受到的值是`SaTokenDao.NEVER_EXPIRE`时，说明永不过期，对应的我们需要把 expireAt 设置为null mongodb就不会删除这个记录
        return timeout == SaTokenDao.NEVER_EXPIRE ? null : LocalDateTime.now().plusSeconds(timeout);
    }


    /**
     * 写入 value，并设定存活时间（单位: 秒）
     *
     * @param key     键名称
     * @param value   值
     * @param timeout 数据有效期（值大于0时限时存储，值=-1时永久存储，值=0或小于-2时不存储）
     */
    @Override
    public void set(String key, String value, long timeout) {
        if (timeout == 0 || timeout <= SaTokenDao.NOT_VALUE_EXPIRE) {
            return;
        }
        SaTokenMongoData data = SaTokenMongoData.builder().key(key).string(value).expireAt(getExpireAtFromTimeout(timeout)).build();

        saveOrUpdateWrapper(data, new QueryWrapper<SaTokenMongoData>().eq("key", key));
    }

    /**
     * 获取 value，如无返空
     *
     * @param key 键名称
     * @return value
     */
    @Override
    public String get(String key) {
        return Optional.ofNullable(getOneByKey(key)).map(SaTokenMongoData::getString).orElse(null);
    }

    /**
     * 更新 value （过期时间不变）
     *
     * @param key   键名称
     * @param value 值
     */
    @Override
    public void update(String key, String value) {
        long expire = getTimeout(key);
        // -2 = 无此键
        if (expire == SaTokenDao.NOT_VALUE_EXPIRE) {
            return;
        }
        this.set(key, value, expire);
    }

    /**
     * 删除 value
     *
     * @param key 键名称
     */
    @Override
    public void delete(String key) {
        remove(new UpdateWrapper<SaTokenMongoData>().eq("key", key));
    }

    /**
     * 获取 value 的剩余存活时间（单位: 秒）
     *
     * @param key 指定 key
     * @return 这个 key 的剩余存活时间
     */
    @Override
    public long getTimeout(String key) {
        LocalDateTime localDateTime = Optional.ofNullable(getOneByKey(key)).map(SaTokenMongoData::getExpireAt).orElse(LocalDateTime.MIN);

        long seconds = Duration.between(LocalDateTime.now(), localDateTime).getSeconds();
        if (seconds < 0) {
            return 0;
        }
        return seconds;
    }

    /**
     * 修改 value 的剩余存活时间（单位: 秒）
     *
     * @param key     指定 key
     * @param timeout 过期时间（单位: 秒）
     */
    @Override
    public void updateTimeout(String key, long timeout) {
        // 判断是否想要设置为永久
        if (timeout == SaTokenDao.NEVER_EXPIRE) {
            long expire = getTimeout(key);
            //noinspection StatementWithEmptyBody
            if (expire == SaTokenDao.NEVER_EXPIRE) {
                // 如果其已经被设置为永久，则不作任何处理
            } else {
                // 如果尚未被设置为永久，那么再次set一次
                this.set(key, this.get(key), timeout);
            }
            return;
        }

        this.set(key, this.get(key), timeout);
    }

    /**
     * 获取 Object，如无返空
     *
     * @param key 键名称
     * @return object
     */
    @Override
    public Object getObject(String key) {
        return Optional.ofNullable(getOneByKey(key)).map(SaTokenMongoData::getSession).orElse(null);
    }


    /**
     * 写入 Object，并设定存活时间 （单位: 秒）
     *
     * @param key     键名称
     * @param object  值
     * @param timeout 存活时间（值大于0时限时存储，值=-1时永久存储，值=0或小于-2时不存储）
     */
    @Override
    public void setObject(String key, Object object, long timeout) {
        if (timeout == 0 || timeout <= SaTokenDao.NOT_VALUE_EXPIRE) {
            return;
        }
        // 判断是否为永不过期
        SaTokenMongoData data = SaTokenMongoData.builder().key(key).session((SaSession) object).expireAt(getExpireAtFromTimeout(timeout)).build();

        saveOrUpdateWrapper(data, new QueryWrapper<SaTokenMongoData>().eq("key", key));

    }

    /**
     * 更新 Object （过期时间不变）
     *
     * @param key    键名称
     * @param object 值
     */
    @Override
    public void updateObject(String key, Object object) {
        long expire = getObjectTimeout(key);
        // -2 = 无此键
        if (expire == SaTokenDao.NOT_VALUE_EXPIRE) {
            return;
        }
        this.setObject(key, object, expire);
    }

    /**
     * 删除 Object
     *
     * @param key 键名称
     */
    @Override
    public void deleteObject(String key) {
        delete(key);
    }

    /**
     * 获取 Object 的剩余存活时间 （单位: 秒）
     *
     * @param key 指定 key
     * @return 这个 key 的剩余存活时间
     */
    @Override
    public long getObjectTimeout(String key) {
        return getTimeout(key);
    }

    /**
     * 修改 Object 的剩余存活时间（单位: 秒）
     *
     * @param key     指定 key
     * @param timeout 剩余存活时间
     */
    @Override
    public void updateObjectTimeout(String key, long timeout) {
        updateTimeout(key, timeout);
    }


    /**
     * 搜索数据
     *
     * @param prefix   前缀
     * @param keyword  关键字
     * @param start    开始处索引
     * @param size     获取数量  (-1代表从 start 处一直取到末尾)
     * @param sortType 排序类型（true=正序，false=反序）
     * @return 查询到的数据集合
     */
    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        QueryWrapper<SaTokenMongoData> queryWrapper = new QueryWrapper<>();

        // 构建查询条件
        if (StrUtil.isNotBlank(prefix)) {
            queryWrapper.likeLeft("key", prefix);
        }

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like("key", keyword);
        }

        // 设置排序
        if (sortType) {
            queryWrapper.orderByAsc("key");
        } else {
            queryWrapper.orderByDesc("key");
        }

        // 执行查询并处理分页
        List<SaTokenMongoData> dataList =list(queryWrapper).stream().skip(start).limit(size).toList();

        // 提取 key 字段值
        return dataList.stream()
                .map(SaTokenMongoData::getKey).toList();
    }


    public SaTokenMongoData getOneByKey(String key){
        return this.one(new QueryWrapper<SaTokenMongoData>().eq("key", key));
    }


}