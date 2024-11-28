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
import com.litevar.agent.auth.annotation.WorkspaceRole;
import com.litevar.agent.auth.service.RoleService;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.enums.RoleEnum;
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
    private final Map<String, List<String>> ignoreMap = new HashMap<>();

    private final RoleService roleService;

    public TokenFilter(ApplicationContext applicationContext) {
        scanIgnoreInterface(applicationContext);
        this.roleService = applicationContext.getBean(RoleService.class);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = new UrlPathHelper().getPathWithinApplication(request);
        String method = request.getMethod();
        List<String> ignoreUrl = ignoreMap.get(method);
        boolean match;
        if (ignoreUrl == null) {
            match = false;
        } else {
            match = ignoreUrl.stream().anyMatch(i -> Pattern.matches(i, path));
        }

        if (!match) {
            try {
                String token = getTokenFromRequest(request);
                if (StrUtil.isNotEmpty(token)) {
                    //校验token有效性
                    JWT jwt = JWT.of(token)
                            .setKey(CommonConstant.JWT_SECRET.getBytes());
                    JWTValidator.of(jwt)
                            .validateDate()
                            .validateAlgorithm(JWTSignerUtil.hs256(CommonConstant.JWT_SECRET.getBytes()));

                    LoginUser loginUser = jwt.getPayload().getClaimsJson().toBean(LoginUser.class);
                    Object value = RedisUtil.getValue(String.format(CacheKey.LOGIN_TOKEN, loginUser.getUuid()));
                    if (value == null) {
                        throw new ServiceException(ServiceExceptionEnum.EXPIRED_LOGIN);
                    }

                    LoginContext.set(loginUser);

                    //校验角色
                    Map<String, RoleEnum[]> roleMap = roleMapCache.get(method);
                    if (roleMap != null) {
                        Optional<String> roleOpt = roleMap.keySet().stream().filter(i -> Pattern.matches(i, path)).findFirst();
                        if (roleOpt.isPresent()) {
                            RoleEnum[] needRole = roleMap.get(roleOpt.get());
                            String workspaceId = request.getHeader(CommonConstant.HEADER_WORKSPACE_ID);
                            if (StrUtil.isEmpty(workspaceId)) {
                                throw new ServiceException(ServiceExceptionEnum.WORKSPACE_ID_HEADER_NULL);
                            }
                            boolean hasRole = false;
                            Integer i = roleService.getRoles(workspaceId, loginUser.getId());
                            RoleEnum role = RoleEnum.of(i);
                            for (RoleEnum need : needRole) {
                                if (role == need) {
                                    hasRole = true;
                                    break;
                                }
                            }
                            if (!hasRole) {
                                throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION);
                            }
                        }
                    }

                } else {
                    throw new ServiceException(ServiceExceptionEnum.WITHOUT_TOKEN);
                }
            } catch (ServiceException ex) {
                responseException(ex, response);
                return;

            } catch (ValidateException ex) {
                responseException(new ServiceException(ServiceExceptionEnum.BAD_TOKEN), response);
                return;
            }
        }
        filterChain.doFilter(request, response);
        LoginContext.remove();
    }

    /**
     * 从request中获取token
     *
     * @param request
     * @return
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String authToken = request.getHeader(CommonConstant.HEADER_AUTH);
        if (ObjectUtil.isEmpty(authToken)) {
            return null;
        } else {
            //token不是以Bearer开头，则响应回格式不正确
            if (!authToken.startsWith(CommonConstant.JWT_TOKEN_PREFIX)) {
                throw new ServiceException(ServiceExceptionEnum.ERROR_JWT_TOKEN);
            }
            try {
                authToken = authToken.substring(CommonConstant.JWT_TOKEN_PREFIX.length() + 1);
            } catch (StringIndexOutOfBoundsException e) {
                throw new ServiceException(ServiceExceptionEnum.ERROR_JWT_TOKEN);
            }
        }
        return authToken;
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
            if (method.hasMethodAnnotation(IgnoreAuth.class)) {
                Set<String> set = info.getPathPatternsCondition().getPatterns()
                        .stream().map(i -> {
                            String patternString = i.getPatternString();
                            // 将路径变量转换为正则表达式
                            return patternString.replaceAll("\\{[^/]+\\}", "[^/]+");
                        }).collect(Collectors.toSet());

                for (RequestMethod requestMethod : info.getMethodsCondition().getMethods()) {
                    List<String> list = ignoreMap.computeIfAbsent(requestMethod.name(), k -> new ArrayList<>());
                    list.addAll(set);
                }
            }
            if (method.hasMethodAnnotation(WorkspaceRole.class)) {
                RoleEnum[] role = method.getMethodAnnotation(WorkspaceRole.class).value();
                if (role.length != 0) {
                    info.getPathPatternsCondition().getPatterns().forEach(i -> {
                        String patternString = i.getPatternString();
                        String path = patternString.replaceAll("\\{[^/]+\\}", "[^/]+");

                        for (RequestMethod requestMethod : info.getMethodsCondition().getMethods()) {
                            Map<String, RoleEnum[]> roleMap = roleMapCache.computeIfAbsent(requestMethod.name(), k -> new HashMap<>());
                            roleMap.put(path, role);
                        }
                    });
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
