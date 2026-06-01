import { ModeSchema } from "@/types/qa";

/**
 * Fallback modes config, used only when the backend schema (GET /api/modes) is unavailable.
 * Mirrors tester-agent's qa-prompts.yml (option ids must match). The backend is the source
 * of truth; this keeps the settings screen functional offline and documents the contract.
 *
 * To add a mode/control/option for real, edit qa-prompts.yml on the backend — the UI will
 * pick it up automatically. Update this fallback only to keep parity.
 */
export const DEFAULT_MODES: ModeSchema[] = [
  {
    id: "test-cases",
    label: "Тест-кейсы",
    controls: [
      {
        id: "caseTypes",
        label: "Тип тест-кейса",
        type: "multi",
        defaults: ["ui"],
        options: [
          { id: "ui", label: "UI" },
          { id: "api", label: "API" },
          { id: "db", label: "БД" },
          { id: "combined", label: "Комбинированный" },
        ],
      },
      {
        id: "techniques",
        label: "Техника тест-дизайна",
        type: "multi",
        defaults: ["boundary", "positive-negative"],
        options: [
          { id: "equivalence", label: "Эквивалентное разбиение" },
          { id: "boundary", label: "Граничные значения" },
          { id: "decision-table", label: "Таблица решений" },
          { id: "pairwise", label: "Pairwise" },
          { id: "positive-negative", label: "Позитив/негатив" },
        ],
      },
      {
        id: "stepDetail",
        label: "Детализация шагов",
        type: "single",
        defaults: ["medium"],
        options: [
          { id: "short", label: "Короткая (3–5)" },
          { id: "medium", label: "Средняя (6–10)" },
          { id: "detailed", label: "Подробная (10+)" },
        ],
      },
    ],
  },
];
