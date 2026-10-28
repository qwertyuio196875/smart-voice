package com.example.smartvoice.web;

import java.util.Map;

public record ApiError(String message, Map<String, String> errors) {

    public static ApiError of(String message) {
        return new ApiError(message, Map.of());
    }
}
