package limberli.tester.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Generation settings as they arrive from the UI inside A2A metadata ({@code metadata.qa}).
 *
 * Intentionally generic: {@code selections} maps a control id (e.g. "caseTypes",
 * "techniques", "stepDetail") to the list of chosen option ids. Single-selects are
 * just a list of one. This way new controls require zero changes here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QaSettings(String mode, Map<String, List<String>> selections) {

    public List<String> selected(String controlId) {
        if (selections == null) {
            return List.of();
        }
        return selections.getOrDefault(controlId, List.of());
    }
}
