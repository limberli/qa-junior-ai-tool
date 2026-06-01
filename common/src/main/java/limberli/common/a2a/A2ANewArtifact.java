package limberli.common.a2a;

import java.util.List;

/** Artifact format for A2A SDK 0.3.x+ (message/send response). Uses artifactId + kind-based parts. */
public record A2ANewArtifact(String artifactId, String name, List<A2APart> parts) {}