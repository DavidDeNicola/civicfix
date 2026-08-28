package org.civicfix.app.service;

import lombok.RequiredArgsConstructor;
import org.civicfix.app.dto.CreateTeamRequest;
import org.civicfix.app.dto.TeamResponse;
import org.civicfix.app.mapper.TeamMapper;
import org.civicfix.app.model.Team;
import org.civicfix.app.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Gestione dei team: creazione e lista. Nessuna logica particolare —
 * la complessità sta nell'assegnazione (in ReportService/UserService), non qui.
 */

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;

    public TeamResponse createTeam(CreateTeamRequest request) {
        Team team = teamMapper.toEntity(request);
        return teamMapper.toResponse(teamRepository.save(team));
    }

    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll().stream().map(teamMapper::toResponse).toList();
    }
}
