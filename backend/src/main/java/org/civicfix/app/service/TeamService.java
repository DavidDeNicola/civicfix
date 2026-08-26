package org.civicfix.app.service;

import lombok.RequiredArgsConstructor;
import org.civicfix.app.dto.CreateTeamRequest;
import org.civicfix.app.dto.TeamResponse;
import org.civicfix.app.model.Team;
import org.civicfix.app.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamResponse createTeam(CreateTeamRequest request) {
        Team team = new Team();
        team.setName(request.name());
        team.setCategory(request.category());
        return TeamResponse.from(teamRepository.save(team));
    }

    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll().stream().map(TeamResponse::from).toList();
    }
}
