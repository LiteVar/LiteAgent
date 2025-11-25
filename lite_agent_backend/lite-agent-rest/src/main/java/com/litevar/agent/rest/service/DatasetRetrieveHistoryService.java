package com.litevar.agent.rest.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.entity.DatasetDocument;
import com.litevar.agent.base.entity.DatasetRetrieveHistory;
import com.litevar.agent.base.entity.DocumentSegment;
import com.litevar.agent.base.response.PageModel;
import com.litevar.agent.base.vo.SegmentVO;
import com.mongoplus.conditions.query.LambdaQueryChainWrapper;
import com.mongoplus.incrementer.id.IdWorker;
import com.mongoplus.model.PageResult;
import com.mongoplus.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author reid
 * @since 3/18/25
 */

@Service
public class DatasetRetrieveHistoryService extends ServiceImpl<DatasetRetrieveHistory> {
    @Lazy
    @Autowired
    private SegmentService segmentService;
    @Autowired
    private DocumentService documentService;

    public DatasetRetrieveHistory createHistory(String datasetId, String agentId, String content) {
        DatasetRetrieveHistory history = new DatasetRetrieveHistory();
        history.setId(IdWorker.getIdStr());
        history.setDatasetId(datasetId);
        history.setAgentId(agentId);
        history.setContent(content);
        history.setRetrieveType(StrUtil.isBlank(agentId) ? "TEST" : "AGENT");
        return history;
    }

    public PageModel<DatasetRetrieveHistory> getHistoryByDatasetId(String datasetId, Integer pageNo, Integer pageSize) {
        LambdaQueryChainWrapper<DatasetRetrieveHistory> wrapper = lambdaQuery()
                .eq(DatasetRetrieveHistory::getDatasetId, datasetId)
                .orderByDesc(DatasetRetrieveHistory::getCreateTime);

        PageResult<DatasetRetrieveHistory> pageResult = this.page(wrapper, pageNo, pageSize);
        return new PageModel<>(pageNo, pageSize, pageResult.getTotalSize(), pageResult.getContentData());
    }

    public List<SegmentVO> historyDetail(String historyId) {
        DatasetRetrieveHistory history = getById(historyId);
        List<DatasetRetrieveHistory.RetrieveSegment> segments = history.getRetrieveSegmentList();
        List<SegmentVO> result = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(segments)) {
            Map<String, DatasetRetrieveHistory.RetrieveSegment> segmentMap = segments.parallelStream()
                    .collect(Collectors.toMap(DatasetRetrieveHistory.RetrieveSegment::getId, i -> i));

            List<DocumentSegment> segmentList = segmentService.lambdaQuery()
                    .projectDisplay(DocumentSegment::getId, DocumentSegment::getContent, DocumentSegment::getTokenCount,
                            DocumentSegment::getFileId, DocumentSegment::getWordCount)
                    .in(DocumentSegment::getId, segmentMap.keySet()).list();

            List<String> documentIds = segments.parallelStream().map(DatasetRetrieveHistory.RetrieveSegment::getDocumentId).toList();
            Map<String, String> documentMap = documentService.lambdaQuery()
                    .projectDisplay(DatasetDocument::getId, DatasetDocument::getName)
                    .in(DatasetDocument::getId, documentIds).list()
                    .parallelStream().collect(Collectors.toMap(DatasetDocument::getId, DatasetDocument::getName));


            segmentList.forEach(s -> {
                SegmentVO vo = BeanUtil.copyProperties(s, SegmentVO.class);
                DatasetRetrieveHistory.RetrieveSegment retrieveSegment = segmentMap.get(s.getId());
                vo.setDatasetId(retrieveSegment.getDatasetId());
                vo.setDocumentId(retrieveSegment.getDocumentId());
                vo.setDocumentName(documentMap.get(retrieveSegment.getDocumentId()));
                vo.setScore(retrieveSegment.getScore());
                result.add(vo);
            });
        }
        return result.stream().sorted(Comparator.comparing(SegmentVO::getScore).reversed()).toList();
    }
}
