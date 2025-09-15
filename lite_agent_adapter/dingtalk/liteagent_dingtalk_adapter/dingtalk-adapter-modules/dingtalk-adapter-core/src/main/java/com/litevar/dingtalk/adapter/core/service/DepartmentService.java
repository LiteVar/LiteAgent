package com.litevar.dingtalk.adapter.core.service;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiUserListsimpleRequest;
import com.dingtalk.api.request.OapiV2DepartmentListparentbyuserRequest;
import com.dingtalk.api.request.OapiV2DepartmentListsubRequest;
import com.dingtalk.api.response.OapiUserListsimpleResponse;
import com.dingtalk.api.response.OapiV2DepartmentListparentbyuserResponse;
import com.dingtalk.api.response.OapiV2DepartmentListsubResponse;
import com.litevar.dingtalk.adapter.common.auth.utils.DingTalkAppTokenUtil;
import com.litevar.dingtalk.adapter.common.core.exception.ServiceException;
import com.litevar.dingtalk.adapter.core.dto.AgentRobotRefDTO;
import com.litevar.dingtalk.adapter.core.dto.GetDepartmentUserListDTO;
import com.taobao.api.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.litevar.dingtalk.adapter.common.core.constant.DingTalkConstants.DING_TALK_NOT_PERMISSIONS_FLAG;

/**
 *
 * 部门服务
 *
 * @author Teoan
 * @since 2025/8/13 15:48
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    private final DingTalkAppTokenUtil dingTalkAppTokenUtil;

    private final IAgentRobotService agentRobotService;

    /**
     * 获取部门列表
     *
     * @param deptId
     * @return
     */
    public List<JSONObject> getDepartmentList(Long deptId, String robotCode) {
        AgentRobotRefDTO agentRobotRef = agentRobotService.getAgentRobotRefByRobotCode(robotCode);
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/department/listsub");
        OapiV2DepartmentListsubRequest req = new OapiV2DepartmentListsubRequest();
        req.setDeptId(deptId);
        req.setLanguage("zh_CN");
        OapiV2DepartmentListsubResponse rsp;
        try {
            rsp = client.execute(req, dingTalkAppTokenUtil.getDingTalkAppToken(agentRobotRef.getRobotClientId(), agentRobotRef.getRobotClientSecret()));
            // 无权限时抛出异常
            if (StrUtil.isNotBlank(rsp.getErrorCode()) &&
                    StrUtil.contains(rsp.getMsg(), DING_TALK_NOT_PERMISSIONS_FLAG)) {
                throw new ServiceException(rsp.getMsg());
            }
        } catch (Exception e) {
            log.error("获取部门列表失败", e);
            throw new ServiceException(StrUtil.format("获取部门列表失败:{}", e.getMessage()));
        }
        JSONArray depJsonArray = JSONUtil.parseArray(rsp.getResult());
        if(ObjUtil.isEmpty(deptId)||deptId.equals(1L)){
            // 请求根部门时返回用户信息
            List<JSONObject> result = new java.util.ArrayList<>(depJsonArray.toList(JSONObject.class).stream().peek(jsonObject -> {
                jsonObject.set("type", "department");
            }).toList());
            GetDepartmentUserListDTO getDepartmentUserListDTO = GetDepartmentUserListDTO.builder().robotCode(robotCode).deptId(1L).size(100L).cursor(0L).build();
            Boolean hasMore = true;
            while (hasMore){
                OapiUserListsimpleResponse.PageResult pageResult = getDepartmentUserList(getDepartmentUserListDTO);
                List<JSONObject> userResult = JSONUtil.parseArray(pageResult.getList()).toList(JSONObject.class).stream().peek(jsonObject -> { jsonObject.set("type","user");}).toList();
                result.addAll(userResult);
                getDepartmentUserListDTO.setCursor(pageResult.getNextCursor());
                hasMore = pageResult.getHasMore();
            }
            return result;
        }else {
            return depJsonArray.toList(JSONObject.class);
        }
    }


    /**
     * 获取部门用户列表
     *
     * @param getDepartmentUserListDTO
     */
    public OapiUserListsimpleResponse.PageResult getDepartmentUserList(GetDepartmentUserListDTO getDepartmentUserListDTO) {
        AgentRobotRefDTO agentRobotRef = agentRobotService.getAgentRobotRefByRobotCode(getDepartmentUserListDTO.getRobotCode());
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/user/listsimple");
        OapiUserListsimpleRequest req = new OapiUserListsimpleRequest();
        req.setDeptId(getDepartmentUserListDTO.getDeptId());
        req.setCursor(getDepartmentUserListDTO.getCursor());
        req.setSize(getDepartmentUserListDTO.getSize());
        req.setOrderField(getDepartmentUserListDTO.getOrderField());
        req.setContainAccessLimit(false);
        req.setLanguage("zh_CN");
        OapiUserListsimpleResponse rsp = null;
        try {
            rsp = client.execute(req, dingTalkAppTokenUtil.getDingTalkAppToken(agentRobotRef.getRobotClientId()
                    , agentRobotRef.getRobotClientSecret()));
            // 无权限时抛出异常
            if (StrUtil.isNotBlank(rsp.getErrorCode()) &&
                    StrUtil.contains(rsp.getMsg(), DING_TALK_NOT_PERMISSIONS_FLAG)) {
                throw new ServiceException(rsp.getMsg());
            }
        } catch (Exception e) {
            log.error("获取部门用户列表异常", e);
            throw new ServiceException(StrUtil.format("获取部门用户列表异常:{}", e.getMessage()));
        }
        return rsp.getResult();
    }


    /**
     * 获取指定用户的所有父部门列表
     */
    public List<Long> getParentDeptListByUserId(String userId, String robotCode) {
        AgentRobotRefDTO agentRobotRef = agentRobotService.getAgentRobotRefByRobotCode(robotCode);
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/department/listparentbyuser");
        OapiV2DepartmentListparentbyuserRequest req = new OapiV2DepartmentListparentbyuserRequest();
        req.setUserid(userId);
        try {
            OapiV2DepartmentListparentbyuserResponse rsp = client.execute(req, dingTalkAppTokenUtil.getDingTalkAppToken(agentRobotRef.getRobotClientId()
                    , agentRobotRef.getRobotClientSecret()));
            List<OapiV2DepartmentListparentbyuserResponse.DeptParentResponse> parentList = rsp.getResult().getParentList();
            // 存在多层 需去重
            return parentList.stream().map(OapiV2DepartmentListparentbyuserResponse.DeptParentResponse::getParentDeptIdList).flatMap(List::stream).distinct().toList();
        } catch (ApiException e) {
            log.error("获取指定用户的所有父部门列表异常", e);
            throw new ServiceException(StrUtil.format("获取指定用户的所有父部门列表异常:{}", e.getMessage()));
        }

    }


}
