package com.litevar.agent.auth.filter;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.litevar.agent.auth.annotation.IgnoreAuth;
import com.litevar.agent.auth.annotation.SystemRole;
import com.litevar.agent.auth.annotation.WorkspaceRole;
import com.litevar.agent.auth.service.RoleService;
import com.litevar.agent.auth.service.UserService;
import com.litevar.agent.auth.util.JwtUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.entity.Account;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.SystemRoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author uncle
 * @since 2024/7/4 15:28
 */
public class TokenFilter extends OncePerRequestFilter {
    //<method,<path,role[]>>
    private final Map<String, Map<String, RoleEnum[]>> roleMapCache = new HashMap<>();
    private final Map<String, Map<String, SystemRoleEnum[]>> systemRoleMapCache = new HashMap<>();
    private final Map<String, List<String>> ignoreMap = new HashMap<>();

    private final RoleService roleService;
    private final UserService userService;

    public TokenFilter(ApplicationContext applicationContext) {
        scanIgnoreInterface(applicationContext);
        this.roleService = applicationContext.getBean(RoleService.class);
        this.userService = applicationContext.getBean(UserService.class);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = new UrlPathHelper().getPathWithinApplication(request);
        String method = request.getMethod();
        try {
            if (!isIgnore(method, path)) {
                LoginUser loginUser = authenticate(request);
                LoginContext.set(loginUser);
                checkWorkspaceRole(request, method, path, loginUser);
                checkSystemRole(method, path, loginUser);
            }

            filterChain.doFilter(request, response);
        } catch (ServiceException ex) {
            responseException(ex, response);
        } catch (ValidateException ex) {
            responseException(new ServiceException(ServiceExceptionEnum.BAD_TOKEN), response);
        } finally {
            LoginContext.remove();
        }
    }

    private boolean isIgnore(String method, String path) {
        List<String> ignoreUrl = ignoreMap.get(method);
        return ignoreUrl != null && ignoreUrl.stream().anyMatch(i -> Pattern.matches(i, path));
    }

    private LoginUser authenticate(HttpServletRequest request) {
        String token = JwtUtil.getTokenFromRequest(request);
        if (StrUtil.isEmpty(token)) {
            throw new ServiceException(ServiceExceptionEnum.WITHOUT_TOKEN);
        }
        JWT jwt = JWT.of(token)
                .setKey(CommonConstant.JWT_SECRET.getBytes());
        JWTValidator.of(jwt)
                .validateDate()
                .validateAlgorithm(JWTSignerUtil.hs256(CommonConstant.JWT_SECRET.getBytes()));

        LoginUser loginUser = jwt.getPayload().getClaimsJson().toBean(LoginUser.class);
        Object value = RedisUtil.getValue(String.format(CacheKey.LOGIN_TOKEN, loginUser.getUuid()));
        if (value == null || ObjectUtil.notEqual(value, token)) {
            throw new ServiceException(ServiceExceptionEnum.EXPIRED_LOGIN);
        }
        return loginUser;
    }

    public static LoginUser getLoginUser(String uuid) {
        String token = (String) RedisUtil.getValue(String.format(CacheKey.LOGIN_TOKEN, uuid));
        if (StrUtil.isEmpty(token)) {
            throw new ServiceException(ServiceExceptionEnum.EXPIRED_LOGIN);
        }
        return JWT.of(token)
                .setKey(CommonConstant.JWT_SECRET.getBytes())
                .getPayload().getClaimsJson().toBean(LoginUser.class);
    }

    private void checkWorkspaceRole(HttpServletRequest request, String method, String path, LoginUser loginUser) {
        Map<String, RoleEnum[]> roleMap = roleMapCache.get(method);
        if (roleMap == null) {
            return;
        }
        Optional<RoleEnum[]> roleOpt = roleMap.entrySet().stream()
                .filter(entry -> Pattern.matches(entry.getKey(), path))
                .map(Map.Entry::getValue).findFirst();
        if (roleOpt.isEmpty()) {
            return;
        }
        String workspaceId = request.getHeader(CommonConstant.HEADER_WORKSPACE_ID);
        if (StrUtil.isEmpty(workspaceId)) {
            throw new ServiceException(ServiceExceptionEnum.WORKSPACE_ID_HEADER_NULL);
        }
        Integer roleCode = roleService.getRoles(workspaceId, loginUser.getId());
        RoleEnum role = RoleEnum.of(roleCode);
        for (RoleEnum need : roleOpt.get()) {
            if (role == need) {
                return;
            }
        }
        throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION);
    }

    private void checkSystemRole(String method, String path, LoginUser loginUser) {
        Map<String, SystemRoleEnum[]> roleMap = systemRoleMapCache.get(method);
        if (roleMap == null) {
            return;
        }
        Optional<SystemRoleEnum[]> roleOpt = roleMap.entrySet().stream()
                .filter(entry -> Pattern.matches(entry.getKey(), path))
                .map(Map.Entry::getValue).findFirst();
        if (roleOpt.isEmpty()) {
            return;
        }
        Account account = userService.getById(loginUser.getId());
        SystemRoleEnum role = SystemRoleEnum.of(account.getSystemRole());
        for (SystemRoleEnum need : roleOpt.get()) {
            if (role == need) {
                return;
            }
        }
        throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION);
    }

    /**
     * 获取所有不用鉴权的接口
     *
     * @return
     */
    public void scanIgnoreInterface(ApplicationContext applicationContext) {
        RequestMappingHandlerMapping requestMappingHandlerMapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> handlerMethodMap = requestMappingHandlerMapping.getHandlerMethods();

        handlerMethodMap.forEach((info, method) -> {
            if (info.getPathPatternsCondition() == null) {
                return;
            }
            if (!method.hasMethodAnnotation(IgnoreAuth.class)
                    && !method.hasMethodAnnotation(WorkspaceRole.class)
                    && !method.hasMethodAnnotation(SystemRole.class)) {
                return;
            }

            Set<String> resources = info.getPathPatternsCondition().getPatterns()
                    .stream().map(i -> {
                        String patterString = i.getPatternString();
                        //将路径变量转换为正则表达式
                        return patterString.replaceAll("\\{[^/]+\\}", "[^/]+");
                    }).collect(Collectors.toSet());
            for (RequestMethod requestMethod : info.getMethodsCondition().getMethods()) {
                if (method.hasMethodAnnotation(IgnoreAuth.class)) {
                    List<String> list = ignoreMap.computeIfAbsent(requestMethod.name(), k -> new ArrayList<>());
                    list.addAll(resources);
                    continue;
                }
                if (method.hasMethodAnnotation(SystemRole.class)) {
                    SystemRoleEnum[] role = method.getMethodAnnotation(SystemRole.class).value();
                    if (role.length != 0) {
                        Map<String, SystemRoleEnum[]> map = systemRoleMapCache.computeIfAbsent(requestMethod.name(), k -> new HashMap<>());
                        for (String path : resources) {
                            map.put(path, role);
                        }
                    }

                } else if (method.hasMethodAnnotation(WorkspaceRole.class)) {
                    RoleEnum[] role = method.getMethodAnnotation(WorkspaceRole.class).value();
                    if (role.length != 0) {
                        Map<String, RoleEnum[]> map = roleMapCache.computeIfAbsent(requestMethod.name(), k -> new HashMap<>());
                        for (String path : resources) {
                            map.put(path, role);
                        }
                    }
                }
            }
        });
    }

    /**
     * 这里还没走到全局异常处理,所以直接往response中写结果
     */
    private void responseException(ServiceException e, HttpServletResponse response) {
        response.setCharacterEncoding(CharsetUtil.UTF_8);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (ObjectUtil.equal(e.getCode(), ServiceExceptionEnum.EXPIRED_LOGIN.getCode())
                || ObjectUtil.equal(e.getCode(), ServiceExceptionEnum.ERROR_JWT_TOKEN.getCode())
                || ObjectUtil.equal(e.getCode(), ServiceExceptionEnum.WITHOUT_TOKEN.getCode())
                || ObjectUtil.equal(e.getCode(), ServiceExceptionEnum.BAD_TOKEN.getCode())) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        } else {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
        }
        ResponseData<String> error = ResponseData.error(e.getCode(), e.getMessage());
        try {
            JSONConfig config = new JSONConfig();
            config.setIgnoreNullValue(false);
            response.getWriter().write(JSONUtil.toJsonStr(error, config));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
