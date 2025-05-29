package com.litevar.agent.rest.controller.desktop;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.litevar.agent.base.dto.LocalAgentInfoDTO;
import com.litevar.agent.base.entity.Agent;
import com.litevar.agent.base.entity.AgentDatasetRela;
import com.litevar.agent.base.entity.Dataset;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.vo.AgentDetailVO;
import com.litevar.agent.base.vo.DatasetVO;
import com.litevar.agent.base.vo.ModelVO;
import com.litevar.agent.base.vo.ToolVO;
import com.litevar.agent.core.module.agent.AgentService;
import com.litevar.agent.core.module.local.LocalAgentService;
import com.litevar.agent.rest.service.AgentDatasetRelaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 本地agent数据同步
 *
 * @author uncle
 * @since 2024/11/14 14:18
 */
@RestController
@RequestMapping("/dataSync")
public class DataSyncController {
    @Autowired
    private LocalAgentService localAgentService;
    @Autowired
    private AgentDatasetRelaService agentDatasetRelaService;
    @Autowired
    private AgentService agentService;

    /**
     * 新增修改agent
     *
     * @return
     */
    @PostMapping("/agent")
    public ResponseData<String> agent(@RequestBody List<LocalAgentInfoDTO> data) {
        localAgentService.agent(data);
        data.forEach(dto -> {
            if (ObjectUtil.isNotEmpty(dto.getDatasetIds())) {
                agentDatasetRelaService.removeByColumn(AgentDatasetRela::getAgentId, dto.getId());
                agentDatasetRelaService.bind(dto.getId(), dto.getDatasetIds());
            }
        });
        return ResponseData.success();
    }

    /**
     * agent 详情
     *
     * @param id agent id
     * @return
     */
    @GetMapping("/agent/{id}")
    public ResponseData<AgentDetailVO> agentDetail(@PathVariable("id") String id) {
        Agent agent = agentService.findById(id);
        AgentDetailVO vo = agentService.agentDetail(agent);
        List<Dataset> datasets = agentDatasetRelaService.listDatasets(id);
        if (ObjectUtil.isNotEmpty(datasets)) {
            vo.setDatasetList(BeanUtil.copyToList(datasets, DatasetVO.class));
        }
        return ResponseData.success(vo);
    }

    /**
     * 删除agent
     *
     * @param id agentId
     * @return
     */
    @DeleteMapping("/agent/{id}")
    public ResponseData<String> agent(@PathVariable("id") String id) {
        localAgentService.removeAgent(id);
        return ResponseData.success();
    }

    /**
     * 新增修改model
     *
     * @return
     */
    @PostMapping("/model")
    public ResponseData<String> model(@RequestBody List<ModelVO> data) {
        localAgentService.model(data);
        return ResponseData.success();
    }

    /**
     * 删除model
     *
     * @param id model id
     * @return
     */
    @DeleteMapping("/model/{id}")
    public ResponseData<String> model(@PathVariable("id") String id) {
        localAgentService.removeModel(id);
        return ResponseData.success();
    }

    /**
     * 新增修改tool
     *
     * @return
     */
    @PostMapping("/tool")
    public ResponseData<String> tool(@RequestBody List<ToolVO> data) {
        localAgentService.tool(data);
        return ResponseData.success();
    }

    /**
     * 删除tool
     *
     * @param id tool id
     * @return
     */
    @DeleteMapping("/tool/{id}")
    public ResponseData<String> tool(@PathVariable("id") String id) {
        localAgentService.removeTool(id);
        return ResponseData.success();
    }
}
