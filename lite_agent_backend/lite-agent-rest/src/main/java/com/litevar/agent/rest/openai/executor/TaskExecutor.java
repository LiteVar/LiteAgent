package com.litevar.agent.rest.openai.executor;

import com.litevar.agent.base.enums.ExecuteMode;
import com.litevar.agent.base.exception.ServiceException;
import lombok.Data;
import lombok.Getter;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 执行模式(并行,串行,拒绝)
 *
 * @author uncle
 * @since 2025/3/13 15:40
 */
public class TaskExecutor {
    private static final Map<String, TaskExecuteContext> map = new ConcurrentHashMap<>();

    public static CompletableFuture<Object> execute(String id, Integer mode, Callable<Object> task) throws Exception {
        ExecuteMode executeMode = ExecuteMode.of(mode);
        TaskExecuteContext context = map.get(id);
        if (context == null) {
            context = new TaskExecuteContext(executeMode);
            map.put(id, context);
        } else if (context.getMode() != executeMode) {
            map.remove(id);
            context = new TaskExecuteContext(executeMode);
            map.put(id, context);
        }
        return context.execute(task);
    }

    public static void clear(String id) {
        TaskExecuteContext taskExecuteContext = map.get(id);
        if (taskExecuteContext != null) {
            if (taskExecuteContext.taskQueue != null && !taskExecuteContext.taskQueue.isEmpty()) {
                //有任务未执行,延迟清空
                CompletableFuture.delayedExecutor(20, TimeUnit.SECONDS).execute(() -> map.remove(id));
            } else {
                map.remove(id);
            }
        }
    }

    public static class TaskExecuteContext {
        private Queue<DelayedTask> taskQueue;
        private AtomicInteger lack;
        private final ExecuteModeHandler handler;
        @Getter
        private final ExecuteMode mode;

        TaskExecuteContext(ExecuteMode mode) {
            this.mode = mode;
            if (mode == ExecuteMode.SERIAL) {
                taskQueue = new ConcurrentLinkedQueue<>();
                lack = new AtomicInteger(0);
                handler = task -> {
                    DelayedTask delayedTask = new DelayedTask();
                    delayedTask.setCallable(task);
                    CompletableFuture<Object> future = new CompletableFuture<>();
                    delayedTask.setFuture(future);
                    //将任务加入队列等待执行
                    taskQueue.offer(delayedTask);
                    processTask();
                    return future;
                };
            } else if (mode == ExecuteMode.REJECT) {
                lack = new AtomicInteger(0);
                handler = task -> {
                    //拒绝模式,当有任务正在执行,新来的任务都不执行
                    boolean flag = lack.compareAndSet(0, 1);
                    if (flag) {
                        //能执行
                        Object result = task.call();
                        //执行完成后,释放锁
                        lack.set(0);
                        CompletableFuture<Object> future = new CompletableFuture<>();
                        future.complete(result);
                        return future;

                    } else {
                        throw new ServiceException("There are still running tasks, please submit the task later");
                    }
                };
            } else {
                //并行 =>直接放行
                handler = task -> {
                    Object result = task.call();
                    CompletableFuture<Object> future = new CompletableFuture<>();
                    future.complete(result);
                    return future;
                };
            }
        }

        private void processTask() throws Exception {
            if (lack.compareAndSet(0, 1)) {
                while (!taskQueue.isEmpty()) {
                    DelayedTask task = taskQueue.poll();
                    if (task != null) {
                        Object result = task.getCallable().call();
                        task.getFuture().complete(result);
                    }
                }
                lack.set(0);
            }
        }

        CompletableFuture<Object> execute(Callable<Object> task) throws Exception {
            return handler.execute(task);
        }
    }

    private interface ExecuteModeHandler {
        CompletableFuture<Object> execute(Callable<Object> task) throws Exception;
    }

    @Data
    private static class DelayedTask {
        private Callable<Object> callable;
        private CompletableFuture<Object> future;
    }
}
