package com.projectkg.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

class ApiApplicationTests {

  @Test
  void applicationEntryPointHasSpringBootConfiguration() {
    assertNotNull(ApiApplication.class.getAnnotation(SpringBootApplication.class));
  }
}
