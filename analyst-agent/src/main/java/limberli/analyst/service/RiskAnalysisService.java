package limberli.analyst.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Calls Ollama via Spring AI to perform requirements risk analysis.
 *
 * Architectural note: Analyst is intentionally kept independent from Tester.
 * Both agents receive the raw document. If cross-agent analysis were needed
 * (e.g., Analyst reviewing Tester's output), the orchestrator would chain them
 * sequentially via CompletableFuture.thenCompose().
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAnalysisService {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            You are a Senior Business Analyst and Requirements Engineer specializing in
            risk assessment and quality assurance for software projects.

            Analyze the provided requirements document and produce a structured risk report.

            ## 1. Completeness Assessment
            Identify missing requirements, undefined edge cases, and incomplete specifications.
            Rate completeness: Complete / Mostly Complete / Incomplete / Severely Incomplete.

            ## 2. Contradiction Analysis
            Find conflicting requirements, logical inconsistencies, and contradictory constraints.
            For each contradiction: quote both conflicting statements and explain the conflict.

            ## 3. Ambiguity Report
            List vague terms, undefined concepts, and requirements open to multiple interpretations.
            Suggest specific, measurable alternatives for each ambiguity.

            ## 4. Risk Matrix
            | ID | Description | Category | Severity | Likelihood | Mitigation |
            |-----|-------------|----------|----------|------------|------------|
            Categories: Technical / Business / Quality / Security / Compliance
            Severity: Critical / High / Medium / Low

            ## 5. Testability Assessment
            For each requirement, rate testability: Testable / Partially Testable / Not Testable.
            For non-testable requirements, explain what acceptance criteria are missing.

            ## 6. Recommendations
            Actionable improvements, ordered by impact. Include suggested requirement rewrites
            where applicable.

            Be specific and reference exact requirement text when pointing out issues.
            Use professional language suitable for a project risk review board.
            """;

    public String analyzeRisks(String documentText) {
        log.info("Starting risk analysis, documentLength={}", documentText.length());
        long start = System.currentTimeMillis();

        String result = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(documentText)
                .call()
                .content();

        log.info("Risk analysis complete, durationMs={} responseLength={}",
                System.currentTimeMillis() - start, result.length());
        return result;
    }
}
