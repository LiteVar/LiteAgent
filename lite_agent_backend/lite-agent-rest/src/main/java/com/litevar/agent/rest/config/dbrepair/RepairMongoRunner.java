package com.litevar.agent.rest.config.dbrepair;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.PathUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.auth.service.UserService;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.*;
import com.litevar.agent.base.enums.PluginStatus;
import com.litevar.agent.base.enums.SystemRoleEnum;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.OutMessage;
import com.litevar.agent.base.vo.ToolVO;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.core.module.storage.StorageServiceV2;
import com.litevar.agent.core.module.tool.ToolService;
import com.litevar.agent.rest.config.LitevarProperties;
import com.litevar.agent.rest.config.StorageProperties;
import com.litevar.agent.rest.service.DatasetService;
import com.litevar.agent.rest.service.UploadFileService;
import com.litevar.agent.rest.service.UploadFileServiceV2;
import com.mongoplus.conditions.query.QueryWrapper;
import com.mongoplus.conditions.update.UpdateWrapper;
import com.mongoplus.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author reid
 * @since 5/6/25
 */
@Slf4j
@Component
public class RepairMongoRunner implements CommandLineRunner {
    @Autowired
    private ModelService modelService;

    @Autowired
    private ToolService toolService;
    @Autowired
    private BaseMapper baseMapper;

    @Autowired
    private DatasetService datasetService;
    @Autowired
    private UserService userService;
    @Autowired
    private LitevarProperties litevarProperties;
    @Autowired
    private UploadFileService uploadFileService;
    @Autowired
    private UploadFileServiceV2 uploadFileServiceV2;
    @Autowired
    private StorageProperties storageProperties;
    @Autowired
    private StorageServiceV2 storageService;

    @Override
    public void run(String... args) throws Exception {
        Set<String> repairedKey = baseMapper.list(new QueryWrapper<RepairRecord>()
                        .projectDisplay(RepairRecord::getRepairKey), RepairRecord.class)
                .stream().map(RepairRecord::getRepairKey).collect(Collectors.toSet());

        String datasetField = "repair:datasetField";
        String accountSystemRole = "repair:accountSystemRole";
        String builtInPluginData = "repair:builtInPluginData";
        String iconFile = "repair:iconFile";

        // UploadFile表数据迁移
        String uploadFile = "repair:uploadFile";
        //模型状态字段默认值
        String modelStatus = "repair:modelStatus";

//		List<LlmModel> models = modelService.list().stream().filter(v -> v.getType().equalsIgnoreCase("TEXT")).toList();
//		if (models.isEmpty()) {
//			return;
//		}
//
//		models.parallelStream().forEach(v -> v.setType("LLM"));
//		modelService.updateBatchByIds(models);

//        Boolean flag = RedisUtil.setNx("repair:tool", 1);
//        if (flag) {
//            repairToolData();
//        }
//
//        //反思input字段数据拆分
//        Boolean reflectFlag = RedisUtil.setNx("repair:reflect", 1);
//        if (reflectFlag) {
//            repairReflectData();
//        }
//
//        Boolean openToolSchemaFlag = RedisUtil.setNx("repair:openToolSchema", 1);
//        if (openToolSchemaFlag) {
//            repairOpenToolSchema();
//        }
//
//        Boolean modelAliasFlag = RedisUtil.setNx("repair:modelAlias", 1);
//        if (modelAliasFlag) {
//            repairModelAliasData();
//        }
        Boolean datasetLlmModelIdFlag = RedisUtil.setNx("repair:datasetField", 1);
        if (datasetLlmModelIdFlag) {
            repairDatasetField();
        }

        if (!repairedKey.contains(accountSystemRole) && markRepair(accountSystemRole, "3.0.0")) {
            repairAccountSystemRole();
        }

        if (!repairedKey.contains(builtInPluginData) && markRepair(builtInPluginData, "3.0.0")) {
            builtIdPluginData();
        }

        if (!repairedKey.contains(iconFile) && markRepair(iconFile, "3.0.0")) {
            repairIconFile();
        }

        if (!repairedKey.contains(uploadFile) && markRepair(uploadFile, "3.0.0")) {
            repairUploadFile();
        }

        if (!repairedKey.contains(modelStatus) && markRepair(modelStatus, "3.0.0")) {
            repairModelStatus();
        }
    }

    private boolean markRepair(String repairKey, String version) {
        RepairRecord record = new RepairRecord();
        record.setRepairKey(repairKey);
        record.setVersion(version);
        try {
            return baseMapper.save(record);
        } catch (Exception ex) {
            log.warn("修复记录写入失败, repairKey={}", repairKey, ex);
            return false;
        }
    }

    private void repairDatasetField() {
        datasetService.update(datasetService.lambdaUpdate().unset(Dataset::getLlmModelId, Dataset::getSummaryCollectionName));
    }

    /**
     * 模型状态字段默认值
     *
     * @since v3.0.0
     */
    private void repairModelStatus() {
        List<LlmModel> models = modelService.list();
        models.forEach(model -> {
            model.setStatus(1);
            RedisUtil.delKey(CacheKey.MODEL_INFO + ":" + model.getId());
        });
        modelService.updateBatchByIds(models);
    }

    /**
     * @since v3.0.0
     */
    private void repairAccountSystemRole() {
        //将系统的第一个用户改为系统管理员,其他的为普通用户
        List<Account> list = baseMapper.list(new QueryWrapper<Account>().lambdaQuery().orderByAsc(Account::getCreateTime), Account.class);
        if (list.isEmpty()) {
            return;
        }
        Account firstAccount = list.remove(0);
        list.forEach(account -> userService.updateSystemRole(account.getId(), SystemRoleEnum.ROLE_USER.getSystemRole()));
        userService.updateSystemRole(firstAccount.getId(), SystemRoleEnum.ROLE_SYSTEM_ADMIN.getSystemRole());
    }

    /**
     * @since v3.0.0
     */
    private void repairIconFile() {
        Path basePath = Paths.get(litevarProperties.getIconPath());

        List<Account> accounts = baseMapper.list(new QueryWrapper<Account>().lambdaQuery()
                .projectDisplay(Account::getId, Account::getAvatar), Account.class);
        for (Account account : accounts) {
            String url = uploadLegacyIcon(basePath, account.getAvatar(), "account", account.getId());
            if (StrUtil.isBlank(url)) {
                continue;
            }
            baseMapper.update(new UpdateWrapper<Account>()
                    .set(Account::getAvatar, url)
                    .eq(Account::getId, account.getId()), Account.class);
        }

        List<Agent> agents = baseMapper.list(new QueryWrapper<Agent>().lambdaQuery()
                .projectDisplay(Agent::getId, Agent::getIcon), Agent.class);
        for (Agent agent : agents) {
            String url = uploadLegacyIcon(basePath, agent.getIcon(), "agent", agent.getId());
            if (StrUtil.isBlank(url)) {
                continue;
            }
            baseMapper.update(new UpdateWrapper<Agent>()
                    .set(Agent::getIcon, url)
                    .eq(Agent::getId, agent.getId()), Agent.class);
        }

        List<Dataset> datasets = baseMapper.list(new QueryWrapper<Dataset>().lambdaQuery()
                .projectDisplay(Dataset::getId, Dataset::getIcon), Dataset.class);
        for (Dataset dataset : datasets) {
            String url = uploadLegacyIcon(basePath, dataset.getIcon(), "dataset", dataset.getId());
            if (StrUtil.isBlank(url)) {
                continue;
            }
            baseMapper.update(new UpdateWrapper<Dataset>()
                    .set(Dataset::getIcon, url)
                    .eq(Dataset::getId, dataset.getId()), Dataset.class);
        }

        List<ToolProvider> toolProviders = baseMapper.list(new QueryWrapper<ToolProvider>().lambdaQuery()
                .projectDisplay(ToolProvider::getId, ToolProvider::getIcon), ToolProvider.class);
        for (ToolProvider toolProvider : toolProviders) {
            String url = uploadLegacyIcon(basePath, toolProvider.getIcon(), "toolProvider", toolProvider.getId());
            if (StrUtil.isBlank(url)) {
                continue;
            }
            baseMapper.update(new UpdateWrapper<ToolProvider>()
                    .set(ToolProvider::getIcon, url)
                    .eq(ToolProvider::getId, toolProvider.getId()), ToolProvider.class);
        }

        List<Plugin> plugins = baseMapper.list(new QueryWrapper<Plugin>().lambdaQuery()
                .projectDisplay(Plugin::getId, Plugin::getIcon), Plugin.class);
        for (Plugin plugin : plugins) {
            String url = uploadLegacyIcon(basePath, plugin.getIcon(), "plugin", plugin.getId());
            if (StrUtil.isBlank(url)) {
                continue;
            }
            baseMapper.update(new UpdateWrapper<Plugin>()
                    .set(Plugin::getIcon, url)
                    .eq(Plugin::getId, plugin.getId()), Plugin.class);
        }

        List<PluginConnector> connectors = baseMapper.list(new QueryWrapper<PluginConnector>().lambdaQuery()
                .projectDisplay(PluginConnector::getId, PluginConnector::getIcon), PluginConnector.class);
        for (PluginConnector connector : connectors) {
            String url = uploadLegacyIcon(basePath, connector.getIcon(), "pluginConnector", connector.getId());
            if (StrUtil.isBlank(url)) {
                continue;
            }
            baseMapper.update(new UpdateWrapper<PluginConnector>()
                    .set(PluginConnector::getIcon, url)
                    .eq(PluginConnector::getId, connector.getId()), PluginConnector.class);
        }
    }

    private String uploadLegacyIcon(Path basePath, String icon, String entityName, String entityId) {
        if (StrUtil.isBlank(icon) || StrUtil.startWithAnyIgnoreCase(icon, "http://", "https://")) {
            return null;
        }
        Path filePath = basePath.resolve(icon).normalize();
        if (!Files.isRegularFile(filePath)) {
            log.warn("文件不存在, entity={}, id={}, icon={}", entityName, entityId, icon);
            return null;
        }
        String contentType = "application/octet-stream";
        try {
            contentType = Files.probeContentType(filePath);
        } catch (IOException ex) {
            log.warn("文件类型探测失败, entity={}, id={}, icon={}", entityName, entityId, icon, ex);
        }
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            String fileKey = uploadFileServiceV2.uploadFile(inputStream, storageProperties.getImagePath(), filePath.getFileName().toString(), contentType);
            return uploadFileServiceV2.generateNoExpireFileUrl(fileKey);
        } catch (IOException ex) {
            log.warn("图标迁移失败, entity={}, id={}, icon={}", entityName, entityId, icon, ex);
            return null;
        }
    }

    /**
     * 内置插件数据
     *
     * @since v3.0.0
     */
    public void builtIdPluginData() {
        //系统管理员
        Account admin = baseMapper.one(new QueryWrapper<Account>().lambdaQuery()
                .projectDisplay(Account::getId)
                .eq(Account::getSystemRole, SystemRoleEnum.ROLE_SYSTEM_ADMIN.getSystemRole()), Account.class);
        if (admin == null) {
            return;
        }

        //钉钉
        Plugin dingTalk = new Plugin();
        dingTalk.setName("钉钉机器人插件");
        dingTalk.setStatus(PluginStatus.INIT.getStatus());
        dingTalk.setIcon("https://liteagent-oss.liteagent.cn/public/lag_plugins/icon-dingtalk.png");
        dingTalk.setDescription("通过钉钉机器人插件，可将自定义 Agent绑定至钉钉机器人，在群聊或私聊中提供 AI服务，提升协同效率。功能支持完全取决于 Agent能力，无需开发即可配置使用，轻松集成到企业现有钉钉生态。提供数据分析，更便捷地获取使用反馈。");
        dingTalk.setPackageUrl("https://liteagent-oss.liteagent.cn/public/lag_plugins/dingtalk-adapter-server-0.1.0.tar");
        dingTalk.setUserId(admin.getId());
        baseMapper.save(dingTalk);

        //微信公众号
        Plugin wechat = new Plugin();
        wechat.setName("微信公众号插件");
        wechat.setStatus(PluginStatus.INIT.getStatus());
        wechat.setIcon("https://liteagent-oss.liteagent.cn/public/lag_plugins/icon-wechat.png");
        wechat.setDescription("通过微信公众号插件，可将公众号与 Agent快速绑定，实现用户在微信内直接与 AI互动。场景适配开放，完全取决于 Agent的能力。配置简单，无需开发即可接入，助力业务高效触达用户。提供数据分析，更便捷地获取用户反馈。");
        wechat.setPackageUrl("https://liteagent-oss.liteagent.cn/public/lag_plugins/wechat-adapter-server-0.1.0.tar");
        wechat.setUserId(admin.getId());
        baseMapper.save(wechat);

        //website
        Plugin website = new Plugin();
        website.setName("网站插件");
        website.setStatus(PluginStatus.INIT.getStatus());
        website.setIcon("https://liteagent-oss.liteagent.cn/public/lag_plugins/icon-website.png");
        website.setDescription("通过网站插件，可将 Agent快速嵌入官网或业务网站，为访客提供 AI服务。场景服务完全取决于 Agent能力。配置简单，无需开发即可接入，助力业务高效触达用户，提升转化与用户体验。提供数据分析，更便捷地获取用户反馈。");
        website.setPackageUrl("https://liteagent-oss.liteagent.cn/public/lag_plugins/website-adapter-server-0.1.0.tar");
        website.setUserId(admin.getId());
        baseMapper.save(website);
    }

    private void repairModelAliasData() {
        //数据清洗:新增模型别名字段,给个默认值为模型名字
        List<LlmModel> models = modelService.list();
        models.forEach(model -> {
            if (StrUtil.isBlank(model.getAlias())) {
                model.setAlias(model.getName() + "-" + model.getId().substring(model.getId().length() - 6));
                modelService.updateById(model);
                RedisUtil.delKey(CacheKey.MODEL_INFO + ":" + model.getId());
            }
        });
    }

    private void repairToolData() {
        //数据清洗: 去掉工具function rootParam字段
        List<ToolProvider> tools = toolService.list();
        tools.forEach(tool -> {
            try {
                ToolVO vo = BeanUtil.copyProperties(tool, ToolVO.class);
                toolService.updateTool(vo);
            } catch (Exception ex) {
                log.error("工具rootParam:", ex);
            }
        });
    }

    private void repairReflectData() {
        Pattern ri = Pattern.compile("\"rawInput\":\"([^\"]*)\"");
        Pattern ro = Pattern.compile("\"rawOutput\":\"([^\"]*)\"");
        List<AgentChatMessage> list = baseMapper.list(new QueryWrapper<AgentChatMessage>().eq("task_message.message.type", "reflect"), AgentChatMessage.class);
        for (AgentChatMessage message : list) {
            boolean updateFlag = false;
            for (AgentChatMessage.TaskMessage taskMessage : message.getTaskMessage()) {
                for (OutMessage outMessage : taskMessage.getMessage()) {
                    if (StrUtil.equals(outMessage.getType(), "reflect")
                            && StrUtil.equals(outMessage.getRole(), "reflection")) {
                        String str = JSONUtil.toJsonStr(outMessage.getContent());
                        OutMessage.ReflectContent content = JSONUtil.toBean(str, OutMessage.ReflectContent.class);
                        if (StrUtil.isNotEmpty(content.getInput())) {
                            Matcher mi = ri.matcher(content.getInput());
                            Matcher mo = ro.matcher(content.getInput());
                            if (mi.find() && mo.find()) {
                                String rawInput = mi.group(1);
                                content.setRawInput(rawInput);
                                String rawOutput = mo.group(1);
                                content.setRawOutput(rawOutput);
                                log.info("[reflect]原文:{},拆成:\n{}\n{}", content.getInput(), rawInput, rawOutput);
                                content.setInput(null);
                                outMessage.setContent(content);
                                updateFlag = true;

                            } else {
                                log.warn("[reflect]正则匹配失败:{}", content.getInput());
                            }
                        }
                    }
                }
            }
            if (updateFlag) {
                baseMapper.update(new UpdateWrapper<AgentChatMessage>()
                        .set(AgentChatMessage::getTaskMessage, message.getTaskMessage())
                        .eq(AgentChatMessage::getId, message.getId()), AgentChatMessage.class);
            }
        }
    }

    private void repairOpenToolSchema() {
//        log.info("清洗open tool schema数据");
//        List<ToolProvider> list = toolService.list();
//        List<ToolProvider> data = list.parallelStream().filter(tool -> StrUtil.isNotBlank(tool.getOpenSchemaStr())).toList();
//        if (!data.isEmpty()) {
//            for (ToolProvider toolProvider : data) {
//                //如果有open tool schema,直接覆盖到schemaStr字段
//                toolProvider.setSchemaType(ToolSchemaType.OPEN_TOOL_Third.getValue());
//                toolProvider.setSchemaStr(toolProvider.getOpenSchemaStr());
//                log.info("工具id:{}修改了openSchemaStr到schemaStr", toolProvider.getId());
//            }
//            toolService.updateBatchByIds(data);
//
//            data.forEach(tool -> {
//                try {
//                    ToolVO vo = BeanUtil.copyProperties(tool, ToolVO.class);
//                    toolService.updateTool(vo);
//                } catch (Exception ex) {
//                    log.error("工具openToolSchema:", ex);
//                }
//            });
//        }
    }

    private void repairUploadFile() {
        List<UploadFile> files = uploadFileService.list();
        List<UploadFileV2> uploadFiles = new ArrayList<>(files.size());

        ConcurrentHashMap<String, String> fileKeyMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, String> mdKeyMap = new ConcurrentHashMap<>();

        files.parallelStream().forEach(v -> {
            Path path = Path.of(v.getPath());
            if (!path.toFile().exists()) {
                return;
            }

            String filename = FilenameUtils.getName(path.toString());
            String extension = FilenameUtils.getExtension(filename);
            String md5Hex = DigestUtils.md5Hex(FileUtil.readBytes(path));
            //避免文件名过长写入失败,使用md5重命名
            String newFilename = md5Hex + "." + extension;

            Path keyPrefix = Path.of(storageProperties.getDocumentPath(), v.getDatasetId(), md5Hex).normalize();
            String fileKey = keyPrefix.resolve(newFilename).toString();
            log.info("开始迁移文件, fileId={}, srcPath={}, fileKey={}", v.getId(), path, fileKey);
            try {
                storageService.writeFile(fileKey, FileUtil.readBytes(path));
                fileKeyMap.put(v.getId(), fileKey);
                log.info("文件迁移成功, fileId={}, srcPath={}, fileKey={}", v.getId(), path, fileKey);
            } catch (Exception e) {
                log.error("文件迁移失败, fileId={}, srcPath={}, fileKey={}", v.getId(), path, fileKey, e);
            }

            if (!v.getExtension().equalsIgnoreCase("md")
                    && !v.getPath().equalsIgnoreCase(v.getMarkdownPath())
                    && StrUtil.isNotBlank(v.getMarkdownPath())
            ) {
                Path mdPath = Path.of(v.getMarkdownPath());
                if (!mdPath.toFile().exists()) {
                    return;
                }

                String mdName = newFilename + ".md";
                String mdKey = keyPrefix.resolve("md").resolve(mdName).toString();
                try {
                    storageService.writeFile(mdKey, FileUtil.readBytes(mdPath));
                    mdKeyMap.put(v.getId(), mdKey);
                    log.info("Markdown文件迁移成功, fileId={}, srcPath={}, mdKey={}", v.getId(), mdPath, mdKey);
                } catch (Exception e) {
                    log.error("Markdown文件迁移失败, fileId={}, srcPath={}, mdKey={}", v.getId(), mdPath, mdKey, e);
                }

                Path imagePath = mdPath.getParent().resolve("imgs").normalize();
                if (!PathUtil.exists(imagePath, false)) {
                    return;
                }

                Path imageKeyPrefix = Path.of(mdKey).getParent().resolve("imgs").normalize();

                try (Stream<Path> imageStream = Files.walk(imagePath)) {
                    List<Path> imagesFiles = imageStream.filter(Files::isRegularFile).toList();
                    for (Path img : imagesFiles) {
                        String imageName = img.getFileName().toString();
                        Path imageKey = imageKeyPrefix.resolve(imageName).normalize();

                        log.info("开始迁移图片, fileId={}, imagePath={}, imageKey={}", v.getId(), img, imageKey);
                        storageService.writeFile(imageKey.toString(), FileUtil.readBytes(img));
                    }
                } catch (Exception e) {
                    log.error("图片迁移失败, fileId={}", v.getId(), e);
                }
            }
        });

        files.forEach(v -> {
            UploadFileV2 bean = BeanUtil.toBean(v, UploadFileV2.class);
            bean.setFilename(v.getName());
            bean.setStorageType(storageProperties.getType());
            bean.setFileKey(fileKeyMap.get(v.getId()));
            bean.setMarkdownKey(mdKeyMap.get(v.getId()));
            bean.setMd5Hash(DigestUtils.md5Hex(v.getDatasetId() + v.getMd5Hash()));

            uploadFiles.add(bean);
        });

        uploadFileServiceV2.saveBatch(uploadFiles);
    }
}
