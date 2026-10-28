package com.example.smartvoice.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 每 5 秒推进一次任务进度，让轮询中的看板看得到变化。
 */
@Component
public class TaskSimulator {

    private final TaskStore store;

    public TaskSimulator(TaskStore store) {
        this.store = store;
    }

    @Scheduled(fixedRate = 5000)
    public void tick() {
        store.tick(LocalDateTime.now());
    }
}
