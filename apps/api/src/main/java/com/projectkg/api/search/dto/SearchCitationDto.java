package com.projectkg.api.search.dto;

public record SearchCitationDto(
    double score,
    long documentId,
    String blockId,
    String title,
    String text
) {}
