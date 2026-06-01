package limberli.common.dto;

import java.time.Instant;

public record ConversationDto(
        Long id,
        String documentTextPreview,
        String testerResponse,
        String analystResponse,
        Instant createdAt
) {}
