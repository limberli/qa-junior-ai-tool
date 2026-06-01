package limberli.tester.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds the qa-prompts.yml catalog (modes → controls → options).
 *
 * This is the single source of truth shared by two consumers:
 *   - {@code QaPromptBuilder} — composes the LLM system prompt from option.prompt fragments.
 *   - {@code ModesController}  — exposes the schema (labels/options/defaults) to the UI.
 *
 * Adding a new technique/type is a one-line YAML edit; no Java changes required.
 */
@Data
@ConfigurationProperties(prefix = "qa")
public class QaCatalogProperties {

    private List<Mode> modes = List.of();

    public Mode mode(String id) {
        return modes.stream().filter(m -> m.getId().equals(id)).findFirst().orElse(null);
    }

    @Data
    public static class Mode {
        private String id;
        private String label;
        private String base;
        private String output;
        private List<Control> controls = List.of();

        public Control control(String id) {
            return controls.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
        }
    }

    @Data
    public static class Control {
        private String id;
        private String label;
        /** "single" | "multi" */
        private String type;
        private List<String> defaults = List.of();
        private List<Option> options = List.of();

        public Option option(String id) {
            return options.stream().filter(o -> o.getId().equals(id)).findFirst().orElse(null);
        }
    }

    @Data
    public static class Option {
        private String id;
        private String label;
        /** Prompt fragment injected into the system prompt when this option is selected. */
        private String prompt;
    }
}
