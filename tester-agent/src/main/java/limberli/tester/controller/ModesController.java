package limberli.tester.controller;

import limberli.tester.config.QaCatalogProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes the generation-settings schema to clients (the UI renders controls from this).
 *
 * Built from the same {@link QaCatalogProperties} catalog that drives the system prompt,
 * but the prompt fragments are stripped — the client only needs labels/types/options/defaults.
 */
@RestController
@RequiredArgsConstructor
public class ModesController {

    private final QaCatalogProperties catalog;

    public record ModeSchema(String id, String label, List<ControlSchema> controls) {}
    public record ControlSchema(String id, String label, String type,
                                List<String> defaults, List<OptionSchema> options) {}
    public record OptionSchema(String id, String label) {}

    @GetMapping(value = "/modes", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ModeSchema> modes() {
        return catalog.getModes().stream()
                .map(m -> new ModeSchema(
                        m.getId(),
                        m.getLabel(),
                        m.getControls().stream()
                                .map(c -> new ControlSchema(
                                        c.getId(),
                                        c.getLabel(),
                                        c.getType(),
                                        c.getDefaults(),
                                        c.getOptions().stream()
                                                .map(o -> new OptionSchema(o.getId(), o.getLabel()))
                                                .toList()))
                                .toList()))
                .toList();
    }
}
