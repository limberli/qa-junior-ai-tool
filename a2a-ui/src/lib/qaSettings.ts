import { ModeSchema, QaMetadata, QaSelections } from "@/types/qa";

/** Initial selections for a mode, taken from each control's `defaults`. */
export const defaultSelections = (mode: ModeSchema): QaSelections => {
  const selections: QaSelections = {};
  for (const control of mode.controls) {
    selections[control.id] = [...control.defaults];
  }
  return selections;
};

/** Toggle an option for a control, respecting single- vs multi-select semantics. */
export const toggleSelection = (
  selections: QaSelections,
  controlId: string,
  optionId: string,
  type: "single" | "multi"
): QaSelections => {
  const current = selections[controlId] ?? [];

  if (type === "single") {
    return { ...selections, [controlId]: [optionId] };
  }

  const next = current.includes(optionId)
    ? current.filter((id) => id !== optionId)
    : [...current, optionId];

  return { ...selections, [controlId]: next };
};

/** Wraps the chosen settings into the A2A message metadata shape: { qa: { mode, selections } }. */
export const buildQaMetadata = (modeId: string, selections: QaSelections): QaMetadata => ({
  qa: { mode: modeId, selections },
});
