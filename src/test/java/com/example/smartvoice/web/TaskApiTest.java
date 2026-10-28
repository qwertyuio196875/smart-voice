package com.example.smartvoice.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void pageRenders() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("任务看板")));
    }

    @Test
    void listsSeededTasks() throws Exception {
        mvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(14))))
                .andExpect(jsonPath("$[0]", hasKey("name")))
                .andExpect(jsonPath("$[0]", hasKey("status")));
    }

    @Test
    void returnsStats() throws Exception {
        mvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", greaterThanOrEqualTo(14)))
                .andExpect(jsonPath("$.pending", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.totalDialed", greaterThanOrEqualTo(0)));
    }

    @Test
    void createsTaskAsPending() throws Exception {
        String body = """
                {
                  "name": "接口测试任务",
                  "contactCount": 300,
                  "concurrency": 12,
                  "scheduledAt": "2030-01-01T09:30"
                }
                """;

        mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.dialedCount").value(0));
    }

    @Test
    void rejectsInvalidFields() throws Exception {
        String body = """
                {
                  "name": "A",
                  "contactCount": 0,
                  "concurrency": 99,
                  "scheduledAt": null
                }
                """;

        mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasKey("name")))
                .andExpect(jsonPath("$.errors", hasKey("contactCount")))
                .andExpect(jsonPath("$.errors", hasKey("concurrency")))
                .andExpect(jsonPath("$.errors", hasKey("scheduledAt")));
    }

    @Test
    void rejectsConcurrencyAboveContacts() throws Exception {
        String body = """
                {
                  "name": "并发超限任务",
                  "contactCount": 5,
                  "concurrency": 10,
                  "scheduledAt": "2030-01-01T09:30"
                }
                """;

        mvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasKey("concurrency")));
    }
}
