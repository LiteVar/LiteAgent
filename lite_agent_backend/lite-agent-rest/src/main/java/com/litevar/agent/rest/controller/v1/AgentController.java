package com.litevar.agent.rest.controller.v1;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjectUtil;
import com.litevar.agent.auth.annotation.WorkspaceRole;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.dto.AgentDTO;
import com.litevar.agent.base.entity.Agent;
import com.litevar.agent.base.entity.AgentApiKey;
import com.litevar.agent.base.entity.AgentDatasetRela;
import com.litevar.agent.base.entity.Dataset;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.StreamException;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.valid.AddAction;
import com.litevar.agent.base.vo.AgentCreateForm;
import com.litevar.agent.base.vo.AgentDetailVO;
import com.litevar.agent.base.vo.AgentUpdateForm;
import com.litevar.agent.base.vo.DatasetVO;
import com.litevar.agent.core.module.agent.AgentApiKeyService;
import com.litevar.agent.core.module.agent.AgentService;
import com.litevar.agent.rest.config.LitevarProperties;
import com.litevar.agent.rest.openai.agent.AgentManager;
import com.litevar.agent.rest.service.AgentDatasetRelaService;
import com.litevar.agent.rest.service.AgentImportProgressPublisher;
import com.litevar.agent.rest.service.DatasetService;
import com.litevar.agent.rest.util.AgentExportUtil;
import com.litevar.agent.rest.util.AgentImportUtil;
import com.litevar.agent.rest.util.FileDownloadUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * agent接口
 *
 * @author reid
 * @since 2024/8/9
 */

@Validated
@RestController
@RequestMapping("/v1/agent")
public class AgentController {
    @Autowired
    private AgentService agentService;
    @Autowired
    private AgentDatasetRelaService agentDatasetRelaService;
    @Autowired
    private DatasetService datasetService;
    @Autowired
    private AgentApiKeyService agentApiKeyService;
    @Autowired
    private AgentExportUtil agentExportUtil;
    @Autowired
    private AgentImportUtil agentImportUtil;
    @Autowired
    private AgentImportProgressPublisher agentImportProgressPublisher;

    @Autowired
    private LitevarProperties litevarProperties;

    /**
     * agent列表
     *
     * @param workspaceId
     * @param tab         0-全部,1-系统,2-来自分享(弃用),3-我的,4-本地agent
     * @param name        agent名称
     * @return
     */
    @GetMapping("/list")
    public ResponseData<List<AgentDTO>> agentList(@RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
                                                  @RequestParam(value = "tab", defaultValue = "0") Integer tab,
                                                  @RequestParam(value = "name", required = false) String name) {
        List<AgentDTO> list = agentService.agentList(workspaceId, tab, name, 1);
        return ResponseData.success(list);
    }

    /**
     * agent列表(管理)
     *
     * @param workspaceId
     * @param tab         0-全部,1-系统,2-来自分享(弃用),3-我的
     * @param name        agent名称
     * @return
     */
    @GetMapping("/adminList")
    public ResponseData<List<AgentDTO>> agentAdminListAdmin(@RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
                                                            @RequestParam(value = "tab", defaultValue = "0") Integer tab,
                                                            @RequestParam(value = "name", required = false) String name) {
        List<AgentDTO> list = agentService.agentAdminList(workspaceId, tab, name);
        return ResponseData.success(list);
    }

    /**
     * agent详情
     *
     * @param id agent id
     * @return
     */
    @GetMapping("/{id}")
    public ResponseData<AgentDetailVO> info(@PathVariable("id") String id) {
        return ResponseData.success(agentService.info(id));
    }

    /**
     * agent详情(管理)
     *
     * @param id
     * @return
     */
    @GetMapping("/adminInfo/{id}")
    public ResponseData<AgentDetailVO> adminInfo(@PathVariable("id") String id) {
        AgentDetailVO detail = agentService.adminInfo(id);

        List<DatasetVO> vo = null;

        Boolean isDraft = RedisUtil.exists(String.format(CacheKey.AGENT_DRAFT, id));
        if (isDraft) {
            Object value = RedisUtil.getValue(String.format(CacheKey.AGENT_DATASET_DRAFT, id));
            List<String> datasetIds = (List<String>) value;
            List<Dataset> ds = datasetService.searchDatasetsByIds(datasetIds);
            vo = BeanUtil.copyToList(ds, DatasetVO.class);
        } else {
            List<Dataset> datasets = agentDatasetRelaService.listDatasets(id);
            if (ObjectUtil.isNotEmpty(datasets)) {
                vo = BeanUtil.copyToList(datasets, DatasetVO.class);
            }
        }
        detail.setDatasetList(vo);
        return ResponseData.success(detail);
    }

    /**
     * 新建agent
     *
     * @return
     */
    @PostMapping("/add")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<Agent> createAgent(
            @RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
            @Validated @RequestBody AgentCreateForm form
    ) {
        return ResponseData.success(agentService.addAgent(workspaceId, form, LoginContext.currentUserId()));
    }

    /**
     * 删除agent
     *
     * @param id agent id
     * @return
     */
    @DeleteMapping("/{id}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> deleteAgent(@PathVariable("id") String id) {
        agentService.removeAgent(id);
        return ResponseData.success();
    }

    /**
     * 修改agent
     *
     * @param id   agentId
     * @param form
     * @return
     */
    @PutMapping("/{id}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<Agent> updateAgent(
            @PathVariable("id") String id,
            @Validated @RequestBody AgentUpdateForm form
    ) {
        return ResponseData.success(agentService.updateAgent(id, form));
    }

    /**
     * 发布
     *
     * @param id agent id
     * @return
     */
    @PutMapping("/release/{id}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> release(@PathVariable("id") String id) {
        agentService.release(id);
        agentDatasetRelaService.removeByColumn(AgentDatasetRela::getAgentId, id);

        Object value = RedisUtil.getValue(String.format(CacheKey.AGENT_DATASET_DRAFT, id));
        if (value != null) {
            List<String> datasetIds = (List<String>) value;
            agentDatasetRelaService.bind(id, datasetIds);
            RedisUtil.delKey(String.format(CacheKey.AGENT_DATASET_DRAFT, id));
        }
        AgentManager.clearSessionFromAgent(id);
        return ResponseData.success();
    }

    /**
     * 生成agent ApiKey
     *
     * @param id agent id
     * @return
     */
    @PostMapping("/generateApiKey/{id}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<AgentApiKey> generateApiKey(@PathVariable("id") String id) {
        AgentApiKey one = agentApiKeyService.one(agentApiKeyService.lambdaQuery()
                .projectDisplay(AgentApiKey::getId, AgentApiKey::getApiKey)
                .eq(AgentApiKey::getAgentId, id));
        if (one != null) {
            RedisUtil.delKey(CacheKey.AGENT_API_KEY + ":" + one.getApiKey());
            agentApiKeyService.removeById(one.getId());
        }

        String apiKey = "sk-" + UUID.fastUUID().toString(true);
        String url = litevarProperties.getExternalApiUrl() + "/liteAgent/v1";

        AgentApiKey agentApiKey = new AgentApiKey();
        agentApiKey.setAgentId(id);
        agentApiKey.setApiKey(apiKey);
        agentApiKey.setApiUrl(url);
        agentApiKeyService.save(agentApiKey);
        return ResponseData.success(agentApiKey);
    }

    /**
     * 重置方法序列
     *
     * @param agentId
     */
    @PostMapping("/resetSequence/{agentId}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> resetSequence(@PathVariable("agentId") String agentId) {
        RedisUtil.delKey(String.format(CacheKey.REFLECT_TOOL_INFO, agentId));
        return ResponseData.success();
    }

    /**
     * 上传agent文件
     * 读取agent文件返回预览数据供用户确认
     *
     * @param file agent文件
     * @return
     */
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<AgentImportUtil.ImportDescriptor> previewImport(@RequestPart("file") MultipartFile file,
                                                                        @RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId) {
        AgentImportUtil.ImportDescriptor preview = agentImportUtil.previewAgent(file, workspaceId);
        return ResponseData.success(preview);
    }

    /**
     * 导入agent
     *
     * @param workspaceId 工作空间id
     * @param token       预览数据中的token值
     * @param param       修改确认后的数据
     * @return
     */
    @PostMapping(value = "/import/{token}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> importAgent(@RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
                                            @PathVariable("token") @NotBlank String token,
                                            @RequestBody @Validated(value = AddAction.class) AgentImportUtil.ImportDescriptor param) {
        String agentId = agentImportUtil.importAgent(workspaceId, token, param);
        return ResponseData.success(agentId);
    }

    /**
     * agent 导入进度
     *
     * @param token 预览数据中的token值
     * @return SSE 流
     */
    @GetMapping(value = "/import/progress/{token}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public reactor.core.publisher.Flux<ServerSentEvent<String>> importProgress(@PathVariable("token") @NotBlank String token) {
        if (!agentImportUtil.hasImportContext(token)) {
            throw new StreamException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "token不存在或已过期");
        }
        Flux<String> flux = Flux.concat(
                        Flux.just("开始导入工作"),
                        agentImportProgressPublisher.listen(token))
                .take(Duration.ofMinutes(10));
        return flux.map(message -> ServerSentEvent.<String>builder()
                .data(message)
                .build());
    }

    /**
     * 导出agent
     *
     * @param id        agent id
     * @param plainText 是否明文,默认为false
     * @return
     */
    @GetMapping("/export/{id}")
    public void exportAgent(@PathVariable("id") String id,
                            @RequestParam(value = "plainText", defaultValue = "false") boolean plainText,
                            HttpServletResponse response) throws IOException {
        Agent agent = agentService.findById(id);
        byte[] bytes = agentExportUtil.exportAgent(agent, plainText);
        FileDownloadUtil.download(response, agent.getName() + ".agent", bytes);
    }
}
