package com.example.smartvoice.task;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TaskStoreTest {

    private final TaskStore store = new TaskStore();

    @Test
    void newTaskStartsPendingWithZeroProgress() {
        CallTask task = store.create("测试任务A", 100, 10, LocalDateTime.now().plusMinutes(5));

        assertThat(task.status()).isEqualTo(TaskStatus.PENDING);
        assertThat(task.dialedCount()).isZero();
        assertThat(task.connectedCount()).isZero();
        assertThat(store.find(task.id())).isPresent();
    }

    @Test
    void pendingTaskStartsWhenScheduleArrives() {
        LocalDateTime now = LocalDateTime.now();
        CallTask task = store.create("测试任务B", 100, 10, now.plusMinutes(1));

        store.tick(now);
        assertThat(store.find(task.id()).orElseThrow().status()).isEqualTo(TaskStatus.PENDING);

        store.tick(now.plusMinutes(2));
        assertThat(store.find(task.id()).orElseThrow().status()).isEqualTo(TaskStatus.RUNNING);
    }

    @Test
    void runningTaskAdvancesByConcurrency() {
        LocalDateTime now = LocalDateTime.now();
        CallTask task = store.create("测试任务C", 100, 10, now);
        store.tick(now);

        store.tick(now.plusSeconds(5));

        CallTask running = store.find(task.id()).orElseThrow();
        assertThat(running.dialedCount()).isEqualTo(10);
        assertThat(running.connectedCount()).isBetween(0, 10);
    }

    @Test
    void taskCompletesWhenAllContactsAreDialed() {
        LocalDateTime now = LocalDateTime.now();
        CallTask task = store.create("测试任务D", 25, 10, now);
        store.tick(now);

        for (int i = 0; i < 10; i++) {
            store.tick(now.plusSeconds(5L * (i + 1)));
        }

        CallTask completed = store.find(task.id()).orElseThrow();
        assertThat(completed.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completed.dialedCount()).isEqualTo(25);
        assertThat(completed.connectedCount()).isLessThanOrEqualTo(25);
    }

    @Test
    void statsAggregateStatusesAndDialedCount() {
        TaskStore.Stats stats = store.stats();

        assertThat(stats.total()).isEqualTo(14);
        assertThat(stats.pending()).isEqualTo(5);
        assertThat(stats.running()).isEqualTo(4);
        assertThat(stats.totalDialed()).isPositive();
    }
}
