package com.litevar.agent.core.module.workspace;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import com.litevar.agent.base.entity.Account;
import com.litevar.agent.base.entity.Workspace;
import com.litevar.agent.base.entity.WorkspaceMember;
import com.litevar.agent.base.enums.AccountStatus;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.vo.WorkSpaceVO;
import com.mongoplus.conditions.query.QueryWrapper;
import com.mongoplus.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author uncle
 * @since 2024/8/1 15:41
 */
@Slf4j
@Service
public class WorkspaceService extends ServiceImpl<Workspace> {
    @Autowired
    private WorkspaceMemberService workspaceMemberService;

    public String addWorkspace(String name) {
        Workspace workspace = this.one(lambdaQuery()
                .projectDisplay(Workspace::getId)
                .eq(Workspace::getName, name));
        if (workspace != null) {
            throw new ServiceException(ServiceExceptionEnum.DUPLICATE_WORKSPACE_NAME);
        }
        workspace = new Workspace();
        workspace.setName(name);
        this.save(workspace);
        return workspace.getId();
    }

    /**
     * 指定用户的工作空间列表
     */
    public List<WorkSpaceVO> userWorkspaceList(String userId) {
        List<WorkspaceMember> allWorkspace = workspaceMemberService.getByColumn(WorkspaceMember::getUserId, userId);

        if (allWorkspace.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> workspaceIds = allWorkspace.stream().map(WorkspaceMember::getWorkspaceId).collect(Collectors.toSet());
        Map<String, String> workspaceMap = this.list(lambdaQuery()
                        .projectDisplay(Workspace::getId, Workspace::getName)
                        .in(Workspace::getId, workspaceIds))
                .stream().collect(Collectors.toMap(Workspace::getId, Workspace::getName));
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

    /**
     * 新增用户
     */
    public String addUser(String username, String password, String email) {
        Account account = baseMapper.one(new QueryWrapper<Account>()
                .projectDisplay(Account::getId)
                .eq(Account::getEmail, email), Account.class);
        if (account != null) {
            throw new ServiceException(ServiceExceptionEnum.ACCOUNT_EXIST);
        }
        account = new Account();
        account.setEmail(email);
        account.setSalt(RandomUtil.randomString(6));
        account.setPassword(SecureUtil.md5(account.getSalt() + password));
        account.setName(username);
        account.setStatus(AccountStatus.ACTIVE.getValue());
        baseMapper.save(account);
        return account.getId();
    }
}
