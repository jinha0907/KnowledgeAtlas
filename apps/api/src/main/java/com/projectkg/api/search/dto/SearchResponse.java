package com.projectkg.api.search.dto;

import java.util.List;

public record SearchResponse(
    String answer,
    List<SearchCitationDto> citations
) {}
