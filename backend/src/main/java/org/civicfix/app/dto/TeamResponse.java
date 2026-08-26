package org.civicfix.app.dto;

import org.civicfix.app.model.ReportCategory;
import org.civicfix.app.model.Team;

public record TeamResponse(Long id, String name, ReportCategory category, int memberCount) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getCategory(), team.getMembers().size());
    }
}
