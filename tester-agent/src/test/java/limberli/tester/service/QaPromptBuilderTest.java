package limberli.tester.service;

import limberli.tester.config.QaCatalogProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the system prompt is composed from the real qa-prompts.yml according to the
 * user's selections — proving the preliminary settings actually drive generation.
 */
class QaPromptBuilderTest {

    private QaPromptBuilder builder() throws Exception {
        List<PropertySource<?>> sources =
                new YamlPropertySourceLoader().load("qa-prompts", new ClassPathResource("qa-prompts.yml"));
        MutablePropertySources mps = new MutablePropertySources();
        sources.forEach(mps::addLast);
        Binder binder = new Binder(ConfigurationPropertySources.from(mps));
        QaCatalogProperties catalog = binder.bind("qa", QaCatalogProperties.class).get();
        return new QaPromptBuilder(catalog);
    }

    @Test
    void buildsPromptFromSelectedOptions() throws Exception {
        QaSettings settings = new QaSettings("test-cases", Map.of(
                "caseTypes", List.of("api"),
                "techniques", List.of("boundary"),
                "stepDetail", List.of("short")
        ));

        String prompt = builder().build(settings);

        assertThat(prompt).contains("Senior QA");                 // base role
        assertThat(prompt).contains("API-аспекты");               // selected case type
        assertThat(prompt).contains("граничных значений");        // selected technique
        assertThat(prompt).contains("3–5");                       // selected step detail
        assertThat(prompt).contains("| ID | Описание проверки | Шаг | Ожидаемый результат |"); // output format
        assertThat(prompt).doesNotContain("Pairwise");            // unselected technique absent
        assertThat(prompt).doesNotContain("UI-аспекты");          // unselected case type absent
    }

    @Test
    void fallsBackToCatalogDefaultsWhenSelectionsMissing() throws Exception {
        String prompt = builder().build(new QaSettings("test-cases", Map.of()));

        // Defaults from qa-prompts.yml: caseTypes=[ui], techniques=[boundary, positive-negative], stepDetail=[medium]
        assertThat(prompt).contains("UI-аспекты");
        assertThat(prompt).contains("граничных значений");
        assertThat(prompt).contains("6–10");
    }

    @Test
    void nullSettingsStillProducesCompletePrompt() throws Exception {
        String prompt = builder().build(null);

        assertThat(prompt).contains("Senior QA");
        assertThat(prompt).contains("| ID | Описание проверки | Шаг | Ожидаемый результат |");
    }
}
