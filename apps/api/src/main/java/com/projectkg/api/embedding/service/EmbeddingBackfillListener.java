package com.projectkg.api.embedding.service;

import com.projectkg.api.notion.service.DocumentSyncedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EmbeddingBackfillListener {
  private static final Logger logger = LoggerFactory.getLogger(EmbeddingBackfillListener.class);

  private final EmbeddingBackfillService embeddingBackfillService;

  public EmbeddingBackfillListener(EmbeddingBackfillService embeddingBackfillService) {
    this.embeddingBackfillService = embeddingBackfillService;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void backfillEmbeddings(DocumentSyncedEvent event) {
    try {
      int embedded = embeddingBackfillService.backfillDocument(event.documentId());
      if (embedded > 0) {
        logger.info("Backfilled {} embeddings for document {}", embedded, event.documentId());
      }
    } catch (RuntimeException ex) {
      // Ingestion remains durable; a later changed sync or explicit backfill can retry embeddings.
      logger.warn("Embedding backfill failed for document {}", event.documentId(), ex);
    }
  }
}
