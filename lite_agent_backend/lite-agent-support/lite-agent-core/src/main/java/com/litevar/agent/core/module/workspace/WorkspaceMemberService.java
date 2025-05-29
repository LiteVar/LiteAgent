package com.litevar.agent.core.module.workspace;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.Account;
import com.litevar.agent.base.entity.Workspace;
import com.litevar.agent.base.entity.WorkspaceMember;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.response.PageModel;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.WorkspaceMemberVO;
import com.litevar.agent.core.util.MailSendUtil;
import com.mongoplus.conditions.query.LambdaQueryChainWrapper;
import com.mongoplus.conditions.query.QueryWrapper;
import com.mongoplus.model.PageResult;
import com.mongoplus.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author uncle
 * @since 2024/8/13 18:05
 */
@Slf4j
@Service
public class WorkspaceMemberService extends ServiceImpl<WorkspaceMember> {
    @Resource
    private MailSendUtil mailSendUtil;

    @Cacheable(value = CacheKey.USER_ROLE, key = "#userId + ':' + #workspaceId", unless = "#result == null")
    public Integer userRoleInt(String workspaceId, String userId) {
        WorkspaceMember member = this.one(this.lambdaQuery()
                .eq(WorkspaceMember::getWorkspaceId, workspaceId)
                .eq(WorkspaceMember::getUserId, userId));
        return Optional.ofNullable(member).orElseThrow().getRole();
    }

    @CacheEvict(value = CacheKey.USER_ROLE, key = "#userId + ':' + #workspaceId")
    public void updateRole(String userId, String workspaceId) {

    }

    public RoleEnum userRole(String workspaceId, String userId) {
        Integer role = proxy().userRoleInt(workspaceId, userId);
        return RoleEnum.of(role);
    }

    /**
     * 新增成员
     */
    public void addMemberToWorkspace(String userId, String email, String workspaceId, Integer role) {
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspaceId);
        member.setEmail(email);
        member.setRole(role);
        member.setUserId(userId);
        this.save(member);
    }

    /**
     * 新增成员
     * 没注册的邮箱,发送激活邮件到邮箱
     */
    public void addMember(String workspaceId, Set<String> emails, Integer role, String url) {
        Optional.ofNullable(baseMapper.getById(workspaceId, Workspace.class)).orElseThrow();
        Map<String, String> existAccount = baseMapper.list(new QueryWrapper<Account>().lambdaQuery()
                        .in(Account::getEmail, emails), Account.class)
                .parallelStream().collect(Collectors.toMap(Account::getEmail, Account::getId));

        Boolean exist = this.exist(this.lambdaQuery().eq(WorkspaceMember::getWorkspaceId, workspaceId)
                .in(WorkspaceMember::getEmail, emails));
        if (exist) {
            throw new ServiceException(ServiceExceptionEnum.DUPLICATE_EMAIL);
        }

        for (String email : emails) {
            if (existAccount.containsKey(email)) {
                //注册过的,直接加入
                String userId = existAccount.get(email);
                addMemberToWorkspace(userId, email, workspaceId, role);

            } else {
                //没注册过,发邮件邀请
                String token = SecureUtil.md5(email);
                WorkspaceMember member = new WorkspaceMember();
                member.setWorkspaceId(workspaceId);
                member.setEmail(email);
                member.setRole(role);
                RedisUtil.setValue(String.format(CacheKey.ACTIVATE_USER_INFO, token), member, 3, TimeUnit.DAYS);
                String link = url + "?token=" + token;
                mailSendUtil.send(email, "工作空间邀请", "请在有效期3天之内点击加入下方链接加入工作空间\n " + link);
            }
        }
    }

    public PageModel<WorkspaceMemberVO> memberList(String workspaceId, String username, Integer pageNo, Integer pageSize) {
        LambdaQueryChainWrapper<WorkspaceMember> chain = this.lambdaQuery()
                .eq(WorkspaceMember::getWorkspaceId, workspaceId);

        if (StrUtil.isNotEmpty(username)) {
            List<String> userIds = baseMapper.list(new QueryWrapper<Account>().lambdaQuery()
                            .projectDisplay(Account::getId)
                            .like(Account::getName, username), Account.class)
                    .stream().map(Account::getId).toList();
            if (userIds.isEmpty()) {
                return new PageModel<>(pageNo, pageSize, 0L, Collections.emptyList());
            } else {
                chain.in(WorkspaceMember::getUserId, userIds);
            }
        }

        chain.orderByDesc(WorkspaceMember::getCreateTime);
        PageResult<WorkspaceMember> page = this.page(chain, pageNo, pageSize);
        List<String> userIds = page.getContentData().stream().map(WorkspaceMember::getUserId).toList();
        Map<String, String> userMap = baseMapper.getByIds(userIds, Account.class).stream().collect(Collectors.toMap(Account::getId, Account::getName));
        List<WorkspaceMemberVO> res = new ArrayList<>();
        for (WorkspaceMember member : page.getContentData()) {
            WorkspaceMemberVO vo = BeanUtil.copyProperties(member, WorkspaceMemberVO.class);
            vo.setName(userMap.get(member.getUserId()));
            res.add(vo);
        }

        return new PageModel<>(pageNo, pageSize, page.getTotalSize(), res);
    }

    public void removeMember(String memberId) {
        WorkspaceMember member = Optional.ofNullable(this.getById(memberId)).orElseThrow();
        if (member.getRole().equals(RoleEnum.ROLE_ADMIN.getCode())) {
            //不能退管理员
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        }
        String userId = LoginContext.currentUserId();
        RoleEnum role = userRole(member.getWorkspaceId(), userId);
        if (role != RoleEnum.ROLE_ADMIN &&
                !StrUtil.equals(member.getUserId(), LoginContext.currentUserId())) {
            //除了管理员,其余的只能退出自己
            throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION_OPERATE);
        }
        this.removeById(memberId);

        log.info("成员id:{},成功能退出工作空间:{},操作人id:{}", memberId, member.getWorkspaceId(), userId);

        proxy().updateRole(member.getUserId(), member.getWorkspaceId());
    }

    public void updateMemberRole(String memberId, RoleEnum role) {
        WorkspaceMember member = Optional.ofNullable(this.getById(memberId)).orElseThrow();
        Integer originRole = member.getRole();
        if (originRole.equals(RoleEnum.ROLE_ADMIN.getCode())) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        }
        if (originRole.equals(role.getCode())) {
            return;
        }

        member.setRole(role.getCode());
        this.updateById(member);

        proxy().updateRole(member.getUserId(), member.getWorkspaceId());
    }

    private WorkspaceMemberService proxy() {
        return (WorkspaceMemberService) AopContext.currentProxy();
    }
}
