package com.litevar.agent.rest.service;

import com.litevar.agent.base.entity.AgentDatasetRela;
import com.litevar.agent.base.entity.Dataset;
import com.mongoplus.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author reid
 * @since 3/11/25
 */

@Service
public class AgentDatasetRelaService extends ServiceImpl<AgentDatasetRela> {
    @Autowired
    private DatasetService datasetService;

    public void bind(String agentId, List<String> datasetIds) {
        List<AgentDatasetRela> list = new ArrayList<>();
        datasetIds.forEach(id -> {
            AgentDatasetRela rela = new AgentDatasetRela();
            rela.setAgentId(agentId);
            rela.setDatasetId(id);
            list.add(rela);
        });
        this.saveBatch(list);
    }

    public List<Dataset> listDatasets(String agentId) {
        List<String> datasetIds = this.lambdaQuery()
                .eq(AgentDatasetRela::getAgentId, agentId)
                .list()
                .stream()
                .map(AgentDatasetRela::getDatasetId)
                .collect(Collectors.toList());

        return datasetService.searchDatasetsByIds(datasetIds);
    }

    public int countAgents(String datasetId) {
        return (int) this.lambdaQuery()
                .eq(AgentDatasetRela::getDatasetId, datasetId)
                .count();
    }
}
