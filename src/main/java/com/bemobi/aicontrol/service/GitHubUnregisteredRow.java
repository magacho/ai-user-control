package com.bemobi.aicontrol.service;

import java.time.LocalDate;

public record GitHubUnregisteredRow(
    String gitHubLogin,
    String gitHubEmail,
    LocalDate lastUsage,
    Integer linesSuggested,
    Integer linesAccepted
) { }
