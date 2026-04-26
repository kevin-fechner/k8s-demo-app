package com.demo.productservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CursorPage<T>(
        List<T> data,
        String nextCursor,
        boolean hasMore,
        int size
) {
}