package com.example.smartvoice.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateTaskRequest(
        @NotBlank(message = "请填写任务名称")
        @Size(min = 2, max = 40, message = "任务名称需 2–40 个字符")
        String name,

        @NotNull(message = "请填写名单数量")
        @Min(value = 1, message = "名单数量至少为 1")
        @Max(value = 5000, message = "名单数量最多为 5000")
        Integer contactCount,

        @NotNull(message = "请填写并发数")
        @Min(value = 1, message = "并发数至少为 1")
        @Max(value = 50, message = "并发数最多为 50")
        Integer concurrency,

        @NotNull(message = "请选择计划开始时间")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime scheduledAt
) {
}
