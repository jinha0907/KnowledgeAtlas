package com.projectkg.api.embedding.dto;

import com.projectkg.api.embedding.service.EmbeddingIdentity;

public record EmbeddingIdentityDto(String provider, String model, int dimensions) {
  public static EmbeddingIdentityDto from(EmbeddingIdentity identity) {
    return new EmbeddingIdentityDto(identity.provider(), identity.model(), identity.dimensions());
  }
}
