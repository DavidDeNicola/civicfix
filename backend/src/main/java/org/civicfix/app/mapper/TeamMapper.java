package org.civicfix.app.mapper;

import org.civicfix.app.dto.CreateTeamRequest;
import org.civicfix.app.dto.TeamResponse;
import org.civicfix.app.model.Team;
import org.springframework.stereotype.Component;

/** Conversione fra l'entità Team e il suo DTO di risposta. */


@Component
public class TeamMapper {

    public TeamResponse toResponse(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getCategory(), team.getMembers().size());
    }

    public Team toEntity(CreateTeamRequest request) {
        Team team = new Team();
        team.setName(request.name());
        team.setCategory(request.category());
        return team;
    }
}
