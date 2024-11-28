package com.litevar.agent.core.module.workspace;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.Account;
import com.litevar.agent.base.entity.Workspace;
import com.litevar.agent.base.entity.WorkspaceMember;
import com.litevar.agent.base.enums.AccountStatus;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.repository.AccountRepository;
import com.litevar.agent.base.repository.WorkspaceMemberRepository;
import com.litevar.agent.base.repository.WorkspaceRepository;
import com.litevar.agent.base.response.PageModel;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.WorkSpaceVO;
import com.litevar.agent.base.vo.WorkspaceMemberVO;
import com.litevar.agent.core.util.MailSendUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author uncle
 * @since 2024/8/1 15:41
 */
@Slf4j
@Service
public class WorkspaceService {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired
    private WorkspaceMemberService workspaceMemberService;
    @Resource
    private MailSendUtil mailSendUtil;
    @Autowired
    private MongoTemplate mongoTemplate;

    public String addWorkspace(String name) {
        Workspace workspace = workspaceRepository.findByName(name);
        if (workspace != null) {
            throw new ServiceException(ServiceExceptionEnum.DUPLICATE_WORKSPACE_NAME);
        }
        workspace = new Workspace();
        workspace.setName(name);
        return workspaceRepository.insert(workspace).getId();
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
        workspaceMemberRepository.insert(member);
    }

    /**
     * 指定用户的工作空间列表
     */
    public List<WorkSpaceVO> userWorkspaceList(String userId, Integer adminFlag) {
        List<WorkspaceMember> allWorkspace = workspaceMemberRepository.findByUserId(userId);
        if (allWorkspace.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> workspaceIds;
        if (adminFlag == 1) {
            workspaceIds = allWorkspace.stream().filter(i -> ObjectUtil.notEqual(i.getRole(), RoleEnum.ROLE_USER.getCode())).map(WorkspaceMember::getWorkspaceId).collect(Collectors.toSet());
        } else {
            workspaceIds = allWorkspace.stream().map(WorkspaceMember::getWorkspaceId).collect(Collectors.toSet());
        }
        Map<String, String> workspaceMap = workspaceRepository.findAllById(workspaceIds).stream().collect(Collectors.toMap(Workspace::getId, Workspace::getName));
        List<WorkSpaceVO> list = new ArrayList<>();
        for (WorkspaceMember member : allWorkspace) {
            WorkSpaceVO vo = new WorkSpaceVO();
            vo.setId(member.getWorkspaceId());
            vo.setName(workspaceMap.get(member.getWorkspaceId()));
            vo.setRole(RoleEnum.of(member.getRole()));
            list.add(vo);
        }
        return list;
    }

    public PageModel<WorkspaceMemberVO> memberList(String workspaceId, String username, Integer pageNo, Integer pageSize) {
        Query query = new Query();
        Criteria criteria = Criteria.where("workspaceId").is(workspaceId);

        if (StrUtil.isNotEmpty(username)) {
            List<String> userIds = accountRepository.findByNameLike(username).stream().map(Account::getId).toList();
            if (!userIds.isEmpty()) {
                criteria.and("userId").in(userIds);
            }
        }
        query.addCriteria(criteria);

        long total = mongoTemplate.count(query, WorkspaceMember.class);

        PageRequest page = PageRequest.of(pageNo, pageSize, Sort.by("createTime").descending());
        query.with(page);

        List<WorkspaceMember> list = mongoTemplate.find(query, WorkspaceMember.class);
        List<String> userIds = list.stream().map(WorkspaceMember::getUserId).toList();
        Map<String, String> userMap = accountRepository.findAllById(userIds).stream().collect(Collectors.toMap(Account::getId, Account::getName));
        List<WorkspaceMemberVO> res = new ArrayList<>();
        for (WorkspaceMember member : list) {
            WorkspaceMemberVO vo = BeanUtil.copyProperties(member, WorkspaceMemberVO.class);
            vo.setName(userMap.get(member.getUserId()));
            res.add(vo);
        }

        return new PageModel<>(pageNo, pageSize, total, res);
    }

    /**
     * 新增成员
     * 没注册的邮箱,发送激活邮件到邮箱
     */
    public void addMember(String workspaceId, Set<String> emails, Integer role, String url) {
        workspaceRepository.findById(workspaceId).orElseThrow();

        Map<String, String> existAccount = accountRepository.findByEmailIn(emails)
                .parallelStream()
                .collect(Collectors.toMap(Account::getEmail, Account::getId));
        boolean exist = !workspaceMemberRepository.findByWorkspaceIdAndEmailIn(workspaceId, emails).isEmpty();
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

    public void removeMember(String memberId) {
        WorkspaceMember member = workspaceMemberRepository.findById(memberId).orElseThrow();
        if (member.getRole().equals(RoleEnum.ROLE_ADMIN.getCode())) {
            //不能退管理员
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        }
        String userId = LoginContext.currentUserId();
        RoleEnum role = workspaceMemberService.userRole(member.getWorkspaceId(), userId);
        if (role != RoleEnum.ROLE_ADMIN &&
                !StrUtil.equals(member.getUserId(), LoginContext.currentUserId())) {
            //除了管理员,其余的只能退出自己
            throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION_OPERATE);
        }
        workspaceMemberRepository.deleteById(memberId);

        log.info("成员id:{},成功能退出工作空间:{},操作人id:{}", memberId, member.getWorkspaceId(), userId);

        proxy().updateRole(member.getUserId(), member.getWorkspaceId());
    }

    public void updateMemberRole(String memberId, RoleEnum role) {
        WorkspaceMember member = workspaceMemberRepository.findById(memberId).orElseThrow();
        Integer originRole = member.getRole();
        if (originRole.equals(RoleEnum.ROLE_ADMIN.getCode())) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        }
        if (originRole.equals(role.getCode())) {
            return;
        }

        member.setRole(role.getCode());
        workspaceMemberRepository.save(member);

        proxy().updateRole(member.getUserId(), member.getWorkspaceId());
    }

    public Workspace getWorkspace(String id) {
        return workspaceRepository.findById(id).orElseThrow();
    }

    /**
     * 新增用户
     */
    public String addUser(String username, String password, String email) {
        Account account = accountRepository.findByEmail(email);
        if (account != null) {
            throw new ServiceException(ServiceExceptionEnum.ACCOUNT_EXIST);
        }
        account = new Account();
        account.setEmail(email);
        account.setSalt(RandomUtil.randomString(6));
        account.setPassword(SecureUtil.md5(account.getSalt() + password));
        account.setName(username);
        account.setStatus(AccountStatus.ACTIVE.getValue());
        accountRepository.insert(account);

        return account.getId();
    }

    @CacheEvict(value = CacheKey.USER_ROLE, key = "#userId + ':' + #workspaceId")
    public void updateRole(String userId, String workspaceId) {

    }

    private WorkspaceService proxy() {
        return (WorkspaceService) AopContext.currentProxy();
    }
}
