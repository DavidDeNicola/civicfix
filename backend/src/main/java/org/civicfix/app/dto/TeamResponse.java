package org.civicfix.app.dto;

import org.civicfix.app.model.ReportCategory;

public record TeamResponse(Long id, String name, ReportCategory category, int memberCount) {
}
