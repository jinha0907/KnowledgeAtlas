package com.projectkg.api.graph.dto;

import java.util.List;

public record ProjectGraphDto(List<GraphNodeDto> nodes, List<GraphEdgeDto> edges) {}
