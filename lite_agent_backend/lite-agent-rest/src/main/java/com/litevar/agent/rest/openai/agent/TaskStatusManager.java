package com.litevar.agent.rest.openai.agent;

import com.litevar.agent.base.enums.TaskStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务状态管理器 - 内存管理任务状态
 *
 * @author uncle
 * @since 2025/8/11
 */
@Slf4j
public class TaskStatusManager {

    private static final Map<String, Integer> taskStatusMap = new ConcurrentHashMap<>();

    public static void update(String taskId, TaskStatus status) {
        taskStatusMap.put(taskId, status.getStatus());
        log.info("Task status updated taskId:{}=>{}", taskId, status.getMessage());
        if (status == TaskStatus.COMPLETED) {
            //任务结束,清理数据
            taskStatusMap.remove(taskId);
        }
    }

    public static TaskStatus get(String taskId) {
        Integer status = taskStatusMap.get(taskId);
        if (status == null) {
            return null;
        }
        return TaskStatus.of(status);
    }
}