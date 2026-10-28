package com.example.smartvoice.task;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 进程内的任务存储：启动时写入种子数据，重启后回到种子状态。
 */
@Component
public class TaskStore {

    /** 模拟推进时每一通的外呼被接通的比例。 */
    private static final double CONNECT_RATIO = 0.65;

    private final Map<Long, CallTask> tasks = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public TaskStore() {
        seed();
    }

    public List<CallTask> findAll() {
        return tasks.values().stream()
                .sorted(Comparator.comparing(CallTask::createdAt).reversed())
                .toList();
    }

    public Optional<CallTask> find(long id) {
        return Optional.ofNullable(tasks.get(id));
    }

    public CallTask create(String name, int contactCount, int concurrency, LocalDateTime scheduledAt) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LocalDateTime start = scheduledAt.withNano(0);
        if (start.isBefore(now)) {
            start = now;
        }
        CallTask task = new CallTask(sequence.incrementAndGet(), name.trim(), TaskStatus.PENDING,
                contactCount, 0, 0, 0, concurrency, start, now);
        tasks.put(task.id(), task);
        return task;
    }

    public Stats stats() {
        List<CallTask> all = tasks.values().stream().toList();
        int pending = (int) all.stream().filter(t -> t.status() == TaskStatus.PENDING).count();
        int running = (int) all.stream().filter(t -> t.status() == TaskStatus.RUNNING).count();
        int dialed = all.stream().mapToInt(CallTask::dialedCount).sum();
        return new Stats(all.size(), pending, running, dialed);
    }

    /**
     * 模拟推进：到点的待调度任务转为进行中，进行中任务按并发数增加拨打量，拨完转入已完成。
     */
    public void tick(LocalDateTime now) {
        tasks.replaceAll((id, task) -> advance(task, now));
    }

    private CallTask advance(CallTask task, LocalDateTime now) {
        if (task.status() == TaskStatus.PENDING && !task.scheduledAt().isAfter(now)) {
            return task.running();
        }
        if (task.status() != TaskStatus.RUNNING) {
            return task;
        }
        int remaining = task.contactCount() - task.dialedCount();
        if (remaining <= 0) {
            return task.withProgress(task.dialedCount(), task.connectedCount(), TaskStatus.COMPLETED);
        }
        int increment = Math.min(task.concurrency(), remaining);
        int dialed = task.dialedCount() + increment;
        int connected = task.connectedCount() + (int) Math.round(increment * CONNECT_RATIO);
        TaskStatus next = dialed >= task.contactCount() ? TaskStatus.COMPLETED : TaskStatus.RUNNING;
        return task.withProgress(dialed, connected, next);
    }

    private void seed() {
        seed(TaskStatus.RUNNING, "618 大促预热外呼", 1200, 812, 528, 21, 20, -30, -120);
        seed(TaskStatus.COMPLETED, "会员到期提醒", 340, 340, 214, 9, 8, -1440, -2880);
        seed(TaskStatus.PENDING, "新客首单回访", 260, 0, 0, 0, 15, 10, -60);
        seed(TaskStatus.PENDING, "满意度调研第二批", 180, 0, 0, 0, 10, 120, -45);
        seed(TaskStatus.RUNNING, "沉睡用户唤醒", 900, 312, 186, 12, 25, -10, -240);
        seed(TaskStatus.COMPLETED, "课程报名确认", 156, 156, 96, 4, 8, -1500, -3000);
        seed(TaskStatus.FAILED, "续费提醒·华东", 420, 210, 120, 64, 18, -180, -360);
        seed(TaskStatus.CANCELLED, "弃单挽回", 240, 0, 0, 0, 12, -120, -300);
        seed(TaskStatus.PENDING, "双十一预售通知", 3000, 0, 0, 0, 50, 1440, -30);
        seed(TaskStatus.RUNNING, "售后工单回访", 520, 184, 112, 6, 16, -5, -90);
        seed(TaskStatus.COMPLETED, "活动到场确认", 88, 88, 60, 2, 6, -2880, -4320);
        seed(TaskStatus.PENDING, "老客生日祝福", 120, 0, 0, 0, 8, 180, -80);
        seed(TaskStatus.PENDING, "门店到店邀约", 640, 0, 0, 0, 20, 30, -20);
        seed(TaskStatus.RUNNING, "保险续保提醒", 460, 148, 86, 10, 14, -20, -180);
    }

    private void seed(TaskStatus status, String name, int contacts, int dialed, int connected,
                      int failed, int concurrency, int scheduledOffsetMinutes, int createdOffsetMinutes) {
        long id = sequence.incrementAndGet();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        tasks.put(id, new CallTask(id, name, status, contacts, dialed, connected, failed, concurrency,
                now.plusMinutes(scheduledOffsetMinutes), now.plusMinutes(createdOffsetMinutes)));
    }

    public record Stats(int total, int pending, int running, int totalDialed) {
    }
}
