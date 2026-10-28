package com.example.smartvoice.task;

public enum TaskStatus {
    PENDING("待调度"),
    RUNNING("进行中"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    FAILED("已失败");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
