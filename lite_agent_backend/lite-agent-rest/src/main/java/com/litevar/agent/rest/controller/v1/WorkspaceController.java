package com.litevar.agent.rest.controller.v1;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.litevar.agent.auth.annotation.IgnoreAuth;
import com.litevar.agent.auth.annotation.WorkspaceRole;
import com.litevar.agent.auth.service.AuthService;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.entity.Workspace;
import com.litevar.agent.base.entity.WorkspaceMember;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.response.PageModel;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.LoginUser;
import com.litevar.agent.base.vo.WorkSpaceVO;
import com.litevar.agent.base.vo.WorkspaceMemberVO;
import com.litevar.agent.core.module.workspace.WorkspaceService;
import com.litevar.agent.core.module.workspace.WorkspaceMemberService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 工作空间
 *
 * @author uncle
 * @since 2024/7/31 17:06
 */
@RestController
@RequestMapping("/v1/workspace")
public class WorkspaceController {
    @Autowired
    private WorkspaceService workspaceService;
    @Autowired
    private AuthService authService;
    @Autowired
    private WorkspaceMemberService workspaceMemberService;

    /**
     * 创建工作空间
     *
     * @param name 工作空间名字
     * @return
     */
    @PostMapping("/add")
    public ResponseData<String> workspace(@RequestParam("name") String name) {
        String workspaceId = workspaceService.addWorkspace(name);
        LoginUser me = LoginContext.me();
        workspaceMemberService.addMemberToWorkspace(me.getId(), me.getEmail(), workspaceId, RoleEnum.ROLE_ADMIN.getCode());

        return ResponseData.success(workspaceId);
    }

    /**
     * 当前账号加入的工作空间
     *
     * @return
     */
    @GetMapping("/list")
    public ResponseData<List<WorkSpaceVO>> workspace() {
        List<WorkSpaceVO> list = workspaceService.userWorkspaceList(LoginContext.currentUserId());
        return ResponseData.success(list);
    }

    /**
     * 工作空间的成员列表
     *
     * @param workspaceId 工作空间
     * @param username    用户名
     * @return
     */
    @GetMapping("/memberList")
    public ResponseData<PageModel<WorkspaceMemberVO>> memberList(@RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
                                                                 @RequestParam(value = "username", required = false) String username,
                                                                 @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                                 @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo) {
        PageModel<WorkspaceMemberVO> page = workspaceMemberService.memberList(workspaceId, username, pageNo, pageSize);
        return ResponseData.success(page);
    }

    /**
     * 新增工作空间成员
     *
     * @param emails 邮箱
     * @param role   设定角色(0-普通成员,2-开发者)
     * @return
     */
    @PostMapping("/member")
    @WorkspaceRole(value = {RoleEnum.ROLE_ADMIN})
    public ResponseData<String> member(@RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
                                       @RequestParam("emails") Set<String> emails,
                                       @RequestParam("role") Integer role,
                                       HttpServletRequest request) {
        boolean flag = emails.stream().anyMatch(email -> !Validator.isEmail(email));
        if (flag) {
            throw new ServiceException(ServiceExceptionEnum.ARGUMENT_NOT_VALID);
        }
        RoleEnum roleEnum = RoleEnum.of(role);
        if (roleEnum != RoleEnum.ROLE_USER && roleEnum != RoleEnum.ROLE_DEVELOPER) {
            throw new ServiceException(ServiceExceptionEnum.ARGUMENT_NOT_VALID);
        }
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (StrUtil.isEmpty(referer)) {
            throw new ServiceException(ServiceExceptionEnum.REFERER_NOT_NULL);
        }
        URL url = URLUtil.url(referer);
        String port = url.getPort() != -1 ? ":" + url.getPort() : "";
        String frontUrl = url.getProtocol() + "://" + url.getHost() + port + "/activate";

        workspaceMemberService.addMember(workspaceId, emails, role, frontUrl);
        return ResponseData.success();
    }

    /**
     * 删除工作空间成员
     *
     * @param memberId 成员id
     * @return
     */
    @DeleteMapping("/member/{memberId}")
    public ResponseData<String> member(@PathVariable("memberId") String memberId) {
        workspaceMemberService.removeMember(memberId);
        return ResponseData.success();
    }

    /**
     * 修改成员角色
     *
     * @param memberId 成员id
     * @param role     角色
     * @return
     */
    @PutMapping("/member/{memberId}")
    @WorkspaceRole(value = {RoleEnum.ROLE_ADMIN})
    public ResponseData<String> member(@PathVariable("memberId") String memberId, @RequestParam("role") Integer role) {
        RoleEnum roleEnum = RoleEnum.of(role);
        if (roleEnum != RoleEnum.ROLE_USER && roleEnum != RoleEnum.ROLE_DEVELOPER) {
            throw new ServiceException(ServiceExceptionEnum.ARGUMENT_NOT_VALID);
        }
        workspaceMemberService.updateMemberRole(memberId, roleEnum);

        return ResponseData.success();
    }

    /**
     * 获取激活信息
     *
     * @param token 激活链接中获取参数
     * @return
     */
    @IgnoreAuth
    @GetMapping("/activateInfo")
    public ResponseData<Dict> activateInfo(@RequestParam("token") String token) {
        Object value = RedisUtil.getValue(String.format(CacheKey.ACTIVATE_USER_INFO, token));
        if (value == null) {
            throw new ServiceException(ServiceExceptionEnum.INVITE_EXPIRE);
        }
        WorkspaceMember member = BeanUtil.copyProperties(value, WorkspaceMember.class);
        Workspace workspace = workspaceService.getById(member.getWorkspaceId());
        Optional.ofNullable(workspace).orElseThrow();
        Dict res = Dict.create()
                .set("email", member.getEmail())
                .set("workspaceId", workspace.getId())
                .set("workspaceName", workspace.getName());

        return ResponseData.success(res);
    }

    /**
     * 通过激活链接加入(登录)
     *
     * @param token    激活链接中获取参数
     * @param username 昵称
     * @param password 密码
     * @return
     */
    @IgnoreAuth
    @PostMapping("/activateMember")
    public ResponseData<String> activateMember(@RequestParam("token") String token,
                                               @RequestParam("username") String username,
                                               @RequestParam("password") String password) {
        Object value = RedisUtil.getValue(String.format(CacheKey.ACTIVATE_USER_INFO, token));
        if (value == null) {
            throw new ServiceException(ServiceExceptionEnum.INVITE_EXPIRE);
        }
        WorkspaceMember member = BeanUtil.copyProperties(value, WorkspaceMember.class);
        String userId = workspaceService.addUser(username, password, member.getEmail());

        //创建该账号自己的默认工作空间
        String workspaceId = workspaceService.addWorkspace(member.getEmail() + "'s workspace");
        workspaceMemberService.addMemberToWorkspace(userId, member.getEmail(), workspaceId, RoleEnum.ROLE_ADMIN.getCode());

        //加入空间
        workspaceMemberService.addMemberToWorkspace(userId, member.getEmail(), member.getWorkspaceId(), member.getRole());

        RedisUtil.delKey(String.format(CacheKey.ACTIVATE_USER_INFO, token));

        String jwtToken = authService.login(member.getEmail(), password);

        return ResponseData.success(jwtToken);
    }
}
