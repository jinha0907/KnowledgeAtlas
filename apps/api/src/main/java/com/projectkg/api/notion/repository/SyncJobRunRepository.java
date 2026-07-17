package com.projectkg.api.notion.repository;

import java.time.Instant;
import java.util.Optional;

public interface SyncJobRunRepository {
  long createRunning(String sourceType);

  void markSuccess(long id, int syncedDocuments, Instant sourceWatermarkAt);

  void markFailed(long id, String errorMessage);

  Optional<Instant> findLatestSuccessfulSourceWatermark(String sourceType);
}
