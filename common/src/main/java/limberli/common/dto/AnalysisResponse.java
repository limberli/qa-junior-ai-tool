package limberli.common.dto;

import java.time.Instant;

public record AnalysisResponse(
        Long conversationId,
        String testerResponse,
        String analystResponse,
        Instant analyzedAt
) {}
