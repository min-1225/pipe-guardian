package com.pipeguardian.domain;

public enum RiskLevel {
    NORMAL("정상", "🟢"),
    WARNING("열화 진행 중", "🟡"),
    CRITICAL("위험", "🔴");

    private final String description;
    private final String badge;

    RiskLevel(String description, String badge) {
        this.description = description;
        this.badge = badge;
    }

    public String getDescription() { return description; }
    public String getBadge() { return badge; }
}
