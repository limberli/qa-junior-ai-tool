package limberli.tester.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Calls Ollama via Spring AI to generate structured QA test cases from a requirements document.
 *
 * The system prompt is no longer hardcoded: it is composed at request time by
 * {@link QaPromptBuilder} from qa-prompts.yml + the caller's {@link QaSettings}, so the
 * preliminary settings chosen in the UI actually drive what the model produces.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestCaseGenerationService {

    private final ChatClient chatClient;
    private final QaPromptBuilder promptBuilder;

    public String generateTestCases(String documentText, QaSettings settings) {
        String systemPrompt = promptBuilder.build(settings);
        log.info("Generating test cases, documentLength={} mode={} promptLength={}",
                documentText.length(),
                settings != null ? settings.mode() : "default",
                systemPrompt.length());
        long start = System.currentTimeMillis();

        String result = chatClient.prompt()
                .system(systemPrompt)
                .user(documentText)
                .call()
                .content();

        log.info("Test case generation complete, durationMs={} responseLength={}",
                System.currentTimeMillis() - start, result.length());
        return result;
    }
}
