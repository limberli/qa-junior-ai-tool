package limberli.common.a2a;

import java.util.List;

/** Task response format for A2A SDK 0.3.x+ (message/send). */
public record A2ATask(
        String id,
        String contextId,
        String kind,
        A2ATaskStatus status,
        List<A2ANewArtifact> artifacts
) {}
