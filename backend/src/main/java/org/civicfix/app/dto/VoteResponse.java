package org.civicfix.app.dto;

public record VoteResponse(
        long voteCount,
        boolean votedByCurrentUser
) {
}
