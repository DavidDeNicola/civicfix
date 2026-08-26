package org.civicfix.app.dto;

import org.civicfix.app.model.ReportPhoto;

import java.time.LocalDateTime;

public record ReportPhotoResponse(Long id, String url, LocalDateTime uploadedAt) {
    public static ReportPhotoResponse from(ReportPhoto photo) {
        return new ReportPhotoResponse(photo.getId(), "/uploads/reports/" + photo.getFilePath(), photo.getUploadedAt());
    }
}
