package com.example.smartvoice.task;

import java.time.LocalDateTime;

public record CallTask(
        long id,
        String name,
        TaskStatus status,
        int contactCount,
        int dialedCount,
        int connectedCount,
        int failedCount,
        int concurrency,
        LocalDateTime scheduledAt,
        LocalDateTime createdAt
) {

    public double connectRate() {
        return dialedCount == 0 ? 0 : (double) connectedCount / dialedCount * 100;
    }

    public CallTask running() {
        return new CallTask(id, name, TaskStatus.RUNNING, contactCount, dialedCount,
                connectedCount, failedCount, concurrency, scheduledAt, createdAt);
    }

    public CallTask withProgress(int dialed, int connected, TaskStatus next) {
        return new CallTask(id, name, next, contactCount, dialed, connected,
                failedCount, concurrency, scheduledAt, createdAt);
    }
}
