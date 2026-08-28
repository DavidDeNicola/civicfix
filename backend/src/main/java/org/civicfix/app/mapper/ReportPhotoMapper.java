package org.civicfix.app.mapper;

import org.civicfix.app.dto.ReportPhotoResponse;
import org.civicfix.app.model.ReportPhoto;
import org.springframework.stereotype.Component;

@Component
public class ReportPhotoMapper {

    /** Sul database si salva il solo nome del file: l'URL pubblico si compone qui. */
    private static final String PREFISSO_URL = "/uploads/reports/";

    public ReportPhotoResponse toResponse(ReportPhoto photo) {
        return new ReportPhotoResponse(
                photo.getId(),
                PREFISSO_URL + photo.getFilePath(),
                photo.getUploadedAt()
        );
    }
}
