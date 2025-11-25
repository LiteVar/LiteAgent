package com.litevar.agent.rest.config;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.entity.AgentChatMessage;
import com.litevar.agent.base.entity.Dataset;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.entity.ToolProvider;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.OutMessage;
import com.litevar.agent.base.vo.ToolVO;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.core.module.tool.ToolService;
import com.litevar.agent.rest.service.DatasetService;
import com.mongoplus.conditions.query.QueryWrapper;
import com.mongoplus.conditions.update.UpdateWrapper;
import com.mongoplus.mapper.BaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Override
    public void run(String... args) throws Exception {
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
    }

    private void repairDatasetField() {
        datasetService.update(datasetService.lambdaUpdate().unset(Dataset::getLlmModelId,Dataset::getSummaryCollectionName));
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
}
