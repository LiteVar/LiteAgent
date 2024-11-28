package com.litevar.agent.rest.controller.desktop;

import com.litevar.agent.base.dto.LocalAgentInfoDTO;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.vo.ModelVO;
import com.litevar.agent.base.vo.ToolVO;
import com.litevar.agent.core.module.agent.LocalAgentService;
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

    /**
     * 新增修改agent
     *
     * @return
     */
    @PostMapping("/agent")
    public ResponseData<String> agent(@RequestBody List<LocalAgentInfoDTO> data) {
        localAgentService.agent(data);
        return ResponseData.success();
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
