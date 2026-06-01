"use client";

import {
  Alert,
  Box,
  Button,
  Container,
  Stack,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from "@mui/material";
import React from "react";

import { defaultSelections, buildQaMetadata } from "@/lib/qaSettings";
import { ControlSchema, ModeSchema, QaMetadata, QaSelections } from "@/types/qa";

interface PreliminarySettingsProps {
  modes: ModeSchema[];
  loading?: boolean;
  /** False when no agent is available yet (sending would fail). */
  canSubmit?: boolean;
  onSubmit: (requirement: string, metadata: QaMetadata) => void;
}

export const PreliminarySettings: React.FC<PreliminarySettingsProps> = ({
  modes,
  loading = false,
  canSubmit = true,
  onSubmit,
}) => {
  // Single mode for now ("Тест-кейсы"); the schema is built to support more later.
  const mode: ModeSchema | undefined = modes[0];

  const [selections, setSelections] = React.useState<QaSelections>(
    mode ? defaultSelections(mode) : {}
  );
  const [requirement, setRequirement] = React.useState<string>("");

  // Reset selections when the mode/schema changes (e.g. backend schema arrives).
  React.useEffect(() => {
    if (mode) {
      setSelections(defaultSelections(mode));
    }
  }, [mode]);

  if (!mode) {
    return null;
  }

  const handleMultiChange = (controlId: string, values: string[]): void => {
    setSelections((prev) => ({ ...prev, [controlId]: values }));
  };

  const handleSingleChange = (controlId: string, value: string | null): void => {
    if (value !== null) {
      setSelections((prev) => ({ ...prev, [controlId]: [value] }));
    }
  };

  const handleSubmit = (): void => {
    const text = requirement.trim();
    if (!text) {
      return;
    }
    onSubmit(text, buildQaMetadata(mode.id, selections));
  };

  return (
    <Box sx={{ height: "100%", overflow: "auto", bgcolor: "background.paper" }}>
      <Container maxWidth="md" sx={{ py: 4 }}>
        <Typography variant="h4" gutterBottom>
          Генерация: {mode.label}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Задайте параметры, вставьте требование и нажмите «Сгенерировать».
        </Typography>

        <Stack spacing={3}>
          {mode.controls.map((control: ControlSchema) => (
            <Box key={control.id}>
              <Typography variant="subtitle1" sx={{ mb: 1 }}>
                {control.label}
              </Typography>

              {control.type === "multi" ? (
                <ToggleButtonGroup
                  value={selections[control.id] ?? []}
                  onChange={(_e, values: string[]) => handleMultiChange(control.id, values)}
                  size="small"
                  sx={{ flexWrap: "wrap", gap: 1 }}
                >
                  {control.options.map((option) => (
                    <ToggleButton key={option.id} value={option.id} sx={{ borderRadius: 2 }}>
                      {option.label}
                    </ToggleButton>
                  ))}
                </ToggleButtonGroup>
              ) : (
                <ToggleButtonGroup
                  exclusive
                  value={selections[control.id]?.[0] ?? null}
                  onChange={(_e, value: string | null) => handleSingleChange(control.id, value)}
                  size="small"
                  sx={{ flexWrap: "wrap", gap: 1 }}
                >
                  {control.options.map((option) => (
                    <ToggleButton key={option.id} value={option.id} sx={{ borderRadius: 2 }}>
                      {option.label}
                    </ToggleButton>
                  ))}
                </ToggleButtonGroup>
              )}
            </Box>
          ))}

          <TextField
            label="Требование"
            placeholder="Вставьте текст требования…"
            value={requirement}
            onChange={(e) => setRequirement(e.target.value)}
            multiline
            minRows={6}
            fullWidth
          />

          {!canSubmit && (
            <Alert severity="warning">
              Агент ещё не подключён. Убедитесь, что оркестратор запущен (http://localhost:8080).
            </Alert>
          )}

          <Box>
            <Button
              variant="contained"
              size="large"
              disabled={!requirement.trim() || !canSubmit || loading}
              onClick={handleSubmit}
            >
              Сгенерировать
            </Button>
          </Box>
        </Stack>
      </Container>
    </Box>
  );
};
