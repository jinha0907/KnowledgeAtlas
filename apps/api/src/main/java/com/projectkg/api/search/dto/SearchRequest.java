package com.projectkg.api.search.dto;

public record SearchRequest(
    String query,
    Integer topK
) {}
