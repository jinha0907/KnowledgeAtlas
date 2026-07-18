package com.projectkg.api.analysis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.projectkg.api.analysis.dto.DocumentAnalysisResponse;
import com.projectkg.api.analysis.provider.DocumentAnalysisProvider.AnalysisResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DocumentAnalysisServiceTest {

  @Test
  void shouldReturnDisabledWithoutLoadingOrSendingDocumentContent() {
    DocumentAnalysisService service = new DocumentAnalysisService(
        null, null, null, Optional.empty());

    DocumentAnalysisResponse response = service.analyze(42L);

    assertEquals("disabled", response.status());
    assertEquals(null, response.analysis());
  }

  @Test
  void shouldNormalizeAndDeduplicateBoundedTags() {
    AnalysisResult result = DocumentAnalysisService.validateResult(new AnalysisResult(
        "  Delivery plan overview.  ",
        List.of(" Roadmap ", "roadmap", "release   plan")));

    assertEquals("Delivery plan overview.", result.summary());
    assertEquals(List.of("Roadmap", "release plan"), result.tags());
  }

  @Test
  void shouldRejectMissingSummaryOrTooManyTags() {
    assertThrows(IllegalArgumentException.class, () -> DocumentAnalysisService.validateResult(
        new AnalysisResult("", List.of("roadmap"))));
    assertThrows(IllegalArgumentException.class, () -> DocumentAnalysisService.validateResult(
        new AnalysisResult("Summary", List.of("a", "b", "c", "d", "e", "f", "g", "h", "i"))));
  }
}
