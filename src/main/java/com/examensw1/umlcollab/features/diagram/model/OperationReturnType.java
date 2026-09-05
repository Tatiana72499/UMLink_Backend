package com.examensw1.umlcollab.features.diagram.model;

public enum OperationReturnType {
    VOID("void"),
    STRING("String"),
    INTEGER("Integer"),
    LONG("Long"),
    DOUBLE("Double"),
    BOOLEAN("Boolean"),
    UUID("UUID"),
    LOCAL_DATE("LocalDate"),
    LOCAL_DATE_TIME("LocalDateTime");

    private final String displayName;

    OperationReturnType(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }
}
