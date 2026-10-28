package com.example.smartvoice.web;

import com.example.smartvoice.task.CallTask;
import com.example.smartvoice.task.TaskStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskApiController {

    private final TaskStore store;

    public TaskApiController(TaskStore store) {
        this.store = store;
    }

    @GetMapping("/tasks")
    public List<CallTask> tasks() {
        return store.findAll();
    }

    @GetMapping("/stats")
    public TaskStore.Stats stats() {
        return store.stats();
    }

    @PostMapping("/tasks")
    public ResponseEntity<CallTask> create(@Valid @RequestBody CreateTaskRequest request) {
        if (request.concurrency() > request.contactCount()) {
            throw new InvalidRequestException("concurrency", "并发数不能超过名单数量");
        }
        CallTask created = store.create(request.name(), request.contactCount(),
                request.concurrency(), request.scheduledAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
