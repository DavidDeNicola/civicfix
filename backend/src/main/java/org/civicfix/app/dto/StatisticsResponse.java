package org.civicfix.app.dto;

import java.util.List;
import java.util.Map;

public record StatisticsResponse(
        long totalReports,
        Map<String, Long> byStatus,
        Map<String, Long> byCategory,
        Map<String, Long> byPriority,
        List<MonthlyCount> reportsPerMonth,
        Double averageResolutionHours,
        List<TeamCount> topTeams
) {
    public record MonthlyCount(String month, long count) {}

    public record TeamCount(String teamName, long resolvedCount) {}
}
