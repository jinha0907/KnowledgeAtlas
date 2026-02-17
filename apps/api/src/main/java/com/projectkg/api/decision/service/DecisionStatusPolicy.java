package com.projectkg.api.decision.service;

import java.util.Set;

public final class DecisionStatusPolicy {
  private static final Set<String> VALID_STATUS = Set.of("proposed", "accepted", "obsolete");

  private DecisionStatusPolicy() {}

  public static void validateStatusValue(String status) {
    if (status == null || !VALID_STATUS.contains(status)) {
      throw new IllegalArgumentException("status must be one of proposed, accepted, obsolete");
    }
  }

  public static void validateTransition(String from, String to) {
    validateStatusValue(from);
    validateStatusValue(to);

    if (from.equals(to)) {
      return;
    }

    boolean valid = ("proposed".equals(from) && "accepted".equals(to))
        || ("accepted".equals(from) && "obsolete".equals(to));

    if (!valid) {
      throw new IllegalArgumentException("invalid status transition: " + from + " -> " + to);
    }
  }
}
