package com.litevar.agent.core.module.plugin;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.entity.Plugin;
import com.litevar.agent.base.enums.PluginStatus;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.vo.PluginVO;
import com.mongoplus.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

/**
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Service
public class PluginService extends ServiceImpl<Plugin> {
    @Resource
    private PluginConnectorService connectorService;
    @Resource
    private PluginRunnerClient runnerClient;

    @Value("${plugin.runner.base-url:}")
    private String runnerUrl;

    public Plugin findById(String id) {
        Plugin plugin = this.getById(id);
        if (plugin == null) {
            throw new ServiceException(ServiceExceptionEnum.NOT_FOUND_RECORD);
        }
        return plugin;
    }

    public String createPlugin(PluginVO vo, MultipartFile file) {
        //确认runner可用
        runnerClient.ensurePaired();

        String pluginId = IdUtil.getSnowflakeNextIdStr();

        //上传插件包到runner
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", file.getResource())
                .filename(StrUtil.blankToDefault(file.getOriginalFilename(), "image.tar"));
        runnerClient.uploadPackage(pluginId, bodyBuilder.build());

        Plugin plugin = new Plugin();
        plugin.setId(pluginId);
        plugin.setName(vo.getName());
        plugin.setIcon(vo.getIcon());
        plugin.setDescription(vo.getDescription());
        plugin.setStatus(PluginStatus.INIT.getStatus());
        plugin.setUserId(LoginContext.currentUserId());
        plugin.setPackageName(file.getOriginalFilename());
        this.save(plugin);
        return plugin.getId();
    }

    public void updatePlugin(PluginVO vo) {
        Plugin plugin = findById(vo.getId());
        PluginStatus status = PluginStatus.of(plugin.getStatus());
        if (!StrUtil.equals(plugin.getName(), vo.getName())) {
            //开启插件后就不能改插件名
            if (status != PluginStatus.INIT) {
                throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
            }
            boolean exist = this.lambdaQuery().projectDisplay(Plugin::getId)
                    .eq(Plugin::getName, vo.getName())
                    .ne(Plugin::getId, plugin.getId()).count() > 0;
            if (exist) {
                throw new ServiceException(ServiceExceptionEnum.DUPLICATE_PLUGIN_NAME);
            }
        }
        plugin.setName(vo.getName());
        plugin.setIcon(vo.getIcon());
        plugin.setDescription(vo.getDescription());
        this.updateById(plugin);
    }

    public void enable(String pluginId) {
        Plugin plugin = findById(pluginId);
        PluginStatus current = PluginStatus.of(plugin.getStatus());
        if (current == PluginStatus.ENABLED || current == PluginStatus.ENABLING) {
            throw new ServiceException(ServiceExceptionEnum.PLUGIN_ENABLED);
        }

        Integer previousStatus = plugin.getStatus();
        String previousUrl = plugin.getUrl();
        plugin.setStatus(PluginStatus.ENABLING.getStatus());
        this.updateById(plugin);

        try {
            //启动
            PluginRunnerClient.RunnerEnableResponse response = runnerClient.enable(pluginId, plugin.getPackageUrl());
            if (response == null || response.getHostPort() == null) {
                throw new ServiceException(ServiceExceptionEnum.RUNNER_START_FAILURE);
            }
            //runnerUrl去掉端口号,拼接上hostPort得到容器的url
            URI uri = URI.create(runnerUrl);
            String url = uri.getScheme() + "://" + uri.getHost() + ":" + response.getHostPort();

            plugin.setUrl(url);
            plugin.setStatus(PluginStatus.ENABLED.getStatus());
            this.updateById(plugin);
        } catch (Exception ex) {
            try {
                runnerClient.disable(pluginId);
            } catch (Exception ignore) {
            }
            plugin.setStatus(previousStatus);
            plugin.setUrl(previousUrl);
            this.updateById(plugin);
            if (ex instanceof ServiceException e) {
                throw e;
            }
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        }
    }

    public void disable(String pluginId) {
        Plugin plugin = findById(pluginId);
        if (!PluginStatus.ENABLED.getStatus().equals(plugin.getStatus())) {
            throw new ServiceException(ServiceExceptionEnum.PLUGIN_NOT_ENABLED);
        }
        //disable插件,所有connector也要disable
        connectorService.disableConnector(pluginId);

        //docker stop plugin
        runnerClient.disable(pluginId);

        plugin.setStatus(PluginStatus.DISABLED.getStatus());
        this.updateById(plugin);
    }

    public void deletePlugin(String id) {
        Plugin plugin = findById(id);
        //todo 如果有connector,不删除容器了吧,万一有重要数据
        PluginStatus status = PluginStatus.of(plugin.getStatus());
        //只有初始化状态能删除
        if (status != PluginStatus.INIT) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "插件状态不允许删除");
        }
        this.removeById(id);
    }

    public Plugin ensurePluginEnabled(String pluginId) {
        Plugin plugin = findById(pluginId);
        if (plugin == null) {
            throw new ServiceException(ServiceExceptionEnum.PLUGIN_NOT_ENABLED);
        }
        if (ObjectUtil.notEqual(PluginStatus.ENABLED.getStatus(), plugin.getStatus())) {
            throw new ServiceException(ServiceExceptionEnum.PLUGIN_NOT_ENABLED);
        }
        return plugin;
    }

}
