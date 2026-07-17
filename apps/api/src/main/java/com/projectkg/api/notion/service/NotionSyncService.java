package com.projectkg.api.notion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectkg.api.notion.dto.NotionBlockDto;
import com.projectkg.api.notion.dto.NotionSyncRequest;
import com.projectkg.api.notion.dto.NotionSyncResponse;
import com.projectkg.api.notion.repository.ChunkRepository;
import com.projectkg.api.notion.repository.ContentBlockRepository;
import com.projectkg.api.notion.repository.SourceDocumentRepository;
import com.projectkg.api.notion.repository.SourceDocumentRepository.SourceDocumentRow;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotionSyncService implements NotionDocumentSyncService {
  private final SourceDocumentRepository sourceDocumentRepository;
  private final ContentBlockRepository contentBlockRepository;
  private final ChunkRepository chunkRepository;
  private final TextChunker textChunker;
  private final ObjectMapper objectMapper;

  public NotionSyncService(
      SourceDocumentRepository sourceDocumentRepository,
      ContentBlockRepository contentBlockRepository,
      ChunkRepository chunkRepository,
      TextChunker textChunker,
      ObjectMapper objectMapper
  ) {
    this.sourceDocumentRepository = sourceDocumentRepository;
    this.contentBlockRepository = contentBlockRepository;
    this.chunkRepository = chunkRepository;
    this.textChunker = textChunker;
    this.objectMapper = objectMapper;
  }

  @Transactional
  @Override
  public NotionSyncResponse sync(NotionSyncRequest request) {
    validate(request);

    List<NotionBlockDto> blocks = request.blocks() == null ? List.of() : request.blocks();
    Instant now = Instant.now();
    String normalizedRawJson = normalizeRawJson(request.rawJson(), request.sourceId(), blocks);
    String checksumPayload = buildChecksumPayload(request, normalizedRawJson, blocks, objectMapper);
    String checksum = sha256(checksumPayload);

    Optional<SourceDocumentRow> existing =
        sourceDocumentRepository.findBySource(request.sourceType(), request.sourceId());

    long documentId = sourceDocumentRepository.upsert(
        request.sourceType(),
        request.sourceId(),
        request.title(),
        now,
        normalizedRawJson,
        checksum);

    boolean checksumChanged = existing.isEmpty() || !checksum.equals(existing.get().checksum());
    if (!checksumChanged) {
      return new NotionSyncResponse("ok", documentId, 0, 0, false);
    }

    int upsertedBlocks = 0;
    int upsertedChunks = 0;
    Set<String> incomingBlockIds = new HashSet<>();

    for (NotionBlockDto block : blocks) {
      if (block == null || block.blockId() == null || block.blockId().isBlank()) {
        continue;
      }

      String blockText = block.text() == null ? "" : block.text();
      incomingBlockIds.add(block.blockId());
      contentBlockRepository.upsert(
          documentId,
          block.blockId(),
          blockText,
          block.path(),
          parseInstantOrNow(block.updatedAt(), now));
      upsertedBlocks++;

      List<String> chunkTexts = textChunker.chunk(blockText);
      List<ChunkRepository.ChunkRow> rows = new ArrayList<>();
      for (int i = 0; i < chunkTexts.size(); i++) {
        String chunkText = chunkTexts.get(i);
        rows.add(new ChunkRepository.ChunkRow(i, chunkText, estimateTokenCount(chunkText), sha256(chunkText)));
      }

      chunkRepository.replaceForBlock(documentId, block.blockId(), rows);
      upsertedChunks += rows.size();
    }

    reconcileDeletedBlocks(documentId, incomingBlockIds);

    return new NotionSyncResponse("ok", documentId, upsertedBlocks, upsertedChunks, true);
  }

  private void reconcileDeletedBlocks(long documentId, Set<String> incomingBlockIds) {
    for (String existingBlockId : contentBlockRepository.findBlockIdsByDocumentId(documentId)) {
      if (incomingBlockIds.contains(existingBlockId)) {
        continue;
      }
      chunkRepository.deleteByDocumentAndBlock(documentId, existingBlockId);
      contentBlockRepository.deleteByDocumentAndBlock(documentId, existingBlockId);
    }
  }

  private void validate(NotionSyncRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    if (request.sourceType() == null || request.sourceType().isBlank()) {
      throw new IllegalArgumentException("sourceType is required");
    }
    if (request.sourceId() == null || request.sourceId().isBlank()) {
      throw new IllegalArgumentException("sourceId is required");
    }
  }

  private String normalizeRawJson(String rawJson, String sourceId, List<NotionBlockDto> blocks) {
    try {
      if (rawJson != null && !rawJson.isBlank()) {
        return objectMapper.writeValueAsString(objectMapper.readTree(rawJson));
      }

      Map<String, Object> fallback = Map.of(
          "sourceId", sourceId,
          "blockCount", blocks.size());
      return objectMapper.writeValueAsString(fallback);
    } catch (Exception ex) {
      throw new IllegalArgumentException("rawJson must be valid JSON", ex);
    }
  }

  static String buildChecksumPayload(
      NotionSyncRequest request,
      String normalizedRawJson,
      List<NotionBlockDto> blocks,
      ObjectMapper objectMapper
  ) {
    try {
      List<Map<String, String>> normalizedBlocks = blocks.stream()
          .filter(block -> block != null && block.blockId() != null && !block.blockId().isBlank())
          .sorted(Comparator.comparing(NotionBlockDto::blockId))
          .map(block -> {
            Map<String, String> blockMap = new LinkedHashMap<>();
            blockMap.put("blockId", block.blockId().trim());
            blockMap.put("text", block.text() == null ? "" : block.text().trim());
            blockMap.put("path", block.path() == null ? "" : block.path().trim());
            blockMap.put("updatedAt", block.updatedAt() == null ? "" : block.updatedAt().trim());
            return blockMap;
          })
          .toList();

      Map<String, Object> checksumInput = new LinkedHashMap<>();
      checksumInput.put("sourceType", request.sourceType());
      checksumInput.put("sourceId", request.sourceId());
      checksumInput.put("title", request.title() == null ? "" : request.title().trim());
      checksumInput.put("rawJson", normalizedRawJson);
      checksumInput.put("blocks", normalizedBlocks);
      return objectMapper.writeValueAsString(checksumInput);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to build checksum payload", ex);
    }
  }

  private Instant parseInstantOrNow(String input, Instant fallback) {
    if (input == null || input.isBlank()) {
      return fallback;
    }

    try {
      return OffsetDateTime.parse(input).toInstant();
    } catch (DateTimeParseException ex) {
      return fallback;
    }
  }

  private int estimateTokenCount(String text) {
    if (text == null || text.isBlank()) {
      return 0;
    }
    return text.trim().split("\\s+").length;
  }

  private String sha256(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to compute checksum", ex);
    }
  }
}
