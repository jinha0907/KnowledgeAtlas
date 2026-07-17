package com.projectkg.api.notion.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NotionApiClient implements NotionClient {
  private static final int MAX_BLOCK_DEPTH = 32;

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String notionVersion;
  private final String notionToken;

  public NotionApiClient(
      ObjectMapper objectMapper,
      @Value("${notion.base-url:https://api.notion.com/v1}") String notionBaseUrl,
      @Value("${notion.token:}") String notionToken,
      @Value("${notion.version:2022-06-28}") String notionVersion
  ) {
    this.objectMapper = objectMapper;
    this.notionVersion = notionVersion;
    this.notionToken = notionToken == null ? "" : notionToken.trim();
    this.restClient = RestClient.builder()
        .baseUrl(notionBaseUrl)
        .defaultHeader("Notion-Version", notionVersion)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Override
  public PageResult searchPagesUpdatedAfter(Instant since, String startCursor, int pageSize) {
    ensureConfigured();

    Map<String, Object> payload = new java.util.LinkedHashMap<>();
    payload.put("filter", Map.of("value", "page", "property", "object"));
    payload.put("sort", Map.of("direction", "descending", "timestamp", "last_edited_time"));
    payload.put("page_size", pageSize);
    if (startCursor != null && !startCursor.isBlank()) {
      payload.put("start_cursor", startCursor);
    }

    String response = restClient.post()
        .uri("/search")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + notionToken)
        .header("Notion-Version", notionVersion)
        .body(payload)
        .retrieve()
        .body(String.class);

    try {
      JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
      List<NotionPage> pages = new ArrayList<>();
      for (JsonNode node : root.path("results")) {
        String id = node.path("id").asText("");
        Instant lastEdited = parseInstant(node.path("last_edited_time").asText(null));
        if (id.isBlank() || lastEdited == null) {
          continue;
        }
        if (since != null && !lastEdited.isAfter(since)) {
          continue;
        }

        String title = extractTitle(node);
        pages.add(new NotionPage(id, title, lastEdited, objectMapper.writeValueAsString(node)));
      }
      return new PageResult(
          pages,
          root.path("has_more").asBoolean(false),
          root.path("next_cursor").isNull() ? null : root.path("next_cursor").asText(null));
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to parse Notion search response", ex);
    }
  }

  @Override
  public List<NotionBlock> fetchBlockChildren(String pageId) {
    ensureConfigured();

    List<NotionBlock> blocks = new ArrayList<>();
    fetchBlockDescendants(pageId, "page:" + pageId, 0, blocks);
    return blocks;
  }

  private void fetchBlockDescendants(
      String parentBlockId,
      String parentPath,
      int depth,
      List<NotionBlock> blocks
  ) {
    if (depth > MAX_BLOCK_DEPTH) {
      throw new IllegalStateException("Notion block nesting exceeds supported depth: " + MAX_BLOCK_DEPTH);
    }

    String cursor = null;

    while (true) {
      String uri = "/blocks/" + parentBlockId + "/children?page_size=100";
      if (cursor != null && !cursor.isBlank()) {
        uri += "&start_cursor=" + cursor;
      }

      String response = restClient.get()
          .uri(uri)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + notionToken)
          .header("Notion-Version", notionVersion)
          .retrieve()
          .body(String.class);

      try {
        JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
        for (JsonNode blockNode : root.path("results")) {
          String blockId = blockNode.path("id").asText("");
          String type = blockNode.path("type").asText("");
          Instant updatedAt = parseInstant(blockNode.path("last_edited_time").asText(null));
          if (blockId.isBlank()) {
            continue;
          }

          String text = extractBlockText(blockNode, type);
          String path = parentPath + "/" + type + ":" + blockId;
          blocks.add(new NotionBlock(
              blockId,
              text,
              path,
              updatedAt,
              objectMapper.writeValueAsString(blockNode)));

          if (blockNode.path("has_children").asBoolean(false)) {
            fetchBlockDescendants(blockId, path, depth + 1, blocks);
          }
        }

        boolean hasMore = root.path("has_more").asBoolean(false);
        cursor = root.path("next_cursor").isNull() ? null : root.path("next_cursor").asText(null);
        if (!hasMore) {
          break;
        }
      } catch (IOException ex) {
        throw new IllegalStateException("Failed to parse Notion blocks response", ex);
      }
    }

  }

  private Instant parseInstant(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(text);
    } catch (Exception ex) {
      return null;
    }
  }

  private void ensureConfigured() {
    if (notionToken.isBlank()) {
      throw new IllegalStateException("notion.token is required for Notion sync");
    }
  }

  private String extractTitle(JsonNode pageNode) {
    JsonNode titleArray = null;
    java.util.Iterator<Map.Entry<String, JsonNode>> properties = pageNode.path("properties").fields();
    while (properties.hasNext()) {
      JsonNode property = properties.next().getValue();
      if ("title".equals(property.path("type").asText())) {
        titleArray = property.path("title");
        break;
      }
    }

    if (titleArray == null || !titleArray.isArray() || titleArray.isEmpty()) {
      return "Untitled";
    }
    StringBuilder sb = new StringBuilder();
    for (JsonNode titleNode : titleArray) {
      sb.append(titleNode.path("plain_text").asText(""));
    }
    String title = sb.toString().trim();
    return title.isBlank() ? "Untitled" : title;
  }

  private String extractBlockText(JsonNode blockNode, String type) {
    JsonNode richTextArray = blockNode.path(type).path("rich_text");
    if (richTextArray.isArray() && !richTextArray.isEmpty()) {
      return joinPlainText(richTextArray);
    }

    if ("child_page".equals(type) || "child_database".equals(type)) {
      return blockNode.path(type).path("title").asText("").trim();
    }
    if ("equation".equals(type)) {
      return blockNode.path(type).path("expression").asText("").trim();
    }
    if ("table_row".equals(type)) {
      StringBuilder tableText = new StringBuilder();
      for (JsonNode cell : blockNode.path(type).path("cells")) {
        if (tableText.length() > 0) {
          tableText.append(" | ");
        }
        tableText.append(joinPlainText(cell));
      }
      return tableText.toString().trim();
    }
    if ("bookmark".equals(type) || "embed".equals(type) || "link_preview".equals(type)) {
      return blockNode.path(type).path("url").asText("").trim();
    }

    return "";
  }

  private String joinPlainText(JsonNode richTextArray) {
    StringBuilder sb = new StringBuilder();
    for (JsonNode textNode : richTextArray) {
      sb.append(textNode.path("plain_text").asText(""));
    }
    return sb.toString().trim();
  }
}
