package org.civicfix.app.dto;

import java.time.LocalDateTime;

public record ReportPhotoResponse(Long id, String url, LocalDateTime uploadedAt) {
}
