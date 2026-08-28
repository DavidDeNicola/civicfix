package org.civicfix.app.mapper;

import org.civicfix.app.dto.UpdateResponse;
import org.civicfix.app.model.Update;
import org.springframework.stereotype.Component;

@Component
public class UpdateMapper {

    public UpdateResponse toResponse(Update update) {
        return new UpdateResponse(
                update.getId(),
                update.getType(),
                update.getMessage(),
                update.getOldStatus(),
                update.getNewStatus(),
                update.getAuthor().getUsername(),
                update.getCreatedAt()
        );
    }
}
