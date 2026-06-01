package limberli.tester.service;

import limberli.tester.config.QaCatalogProperties;
import limberli.tester.config.QaCatalogProperties.Control;
import limberli.tester.config.QaCatalogProperties.Mode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Assembles the LLM system prompt from the catalog and the user's selections.
 *
 * Order: base → (per selected option, per control, in catalog order) prompt fragments → output.
 * If a control has no selection, its catalog defaults are used — so the prompt is always
 * complete even when the caller omits settings.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QaPromptBuilder {

    public static final String DEFAULT_MODE = "test-cases";

    private final QaCatalogProperties catalog;

    public String build(QaSettings settings) {
        String modeId = settings != null && settings.mode() != null ? settings.mode() : DEFAULT_MODE;
        Mode mode = catalog.mode(modeId);
        if (mode == null) {
            mode = catalog.mode(DEFAULT_MODE);
        }
        if (mode == null) {
            throw new IllegalStateException("No QA mode configured in qa-prompts.yml");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(mode.getBase().strip()).append("\n\n");

        for (Control control : mode.getControls()) {
            List<String> chosen = selectionFor(settings, control);
            for (String optionId : chosen) {
                QaCatalogProperties.Option option = control.option(optionId);
                if (option != null && option.getPrompt() != null) {
                    sb.append("- ").append(option.getPrompt().strip()).append("\n");
                } else {
                    log.warn("Unknown option '{}' for control '{}' — ignored", optionId, control.getId());
                }
            }
        }

        sb.append("\n").append(mode.getOutput().strip());
        return sb.toString();
    }

    /** Selected option ids for a control, falling back to the catalog defaults. */
    private List<String> selectionFor(QaSettings settings, Control control) {
        List<String> chosen = settings != null ? settings.selected(control.getId()) : List.of();
        if (chosen == null || chosen.isEmpty()) {
            return control.getDefaults();
        }
        if ("single".equals(control.getType()) && chosen.size() > 1) {
            return List.of(chosen.get(0));
        }
        return chosen;
    }
}
