import React from "react";

import { getModes } from "@/lib/api/getModes";
import { DEFAULT_MODES } from "@/config/modes";
import { ModeSchema } from "@/types/qa";

export interface UseModesReturn {
  modes: ModeSchema[];
  loading: boolean;
  fromBackend: boolean;
}

interface UseModesParams {
  baseUrl?: string;
  customHeaders?: Record<string, string>;
}

/**
 * Loads the settings schema from the backend (single source of truth) and falls back to
 * the bundled DEFAULT_MODES config when the backend is unreachable. Re-fetches when the
 * orchestrator URL becomes available.
 */
export const useModes = (params?: UseModesParams): UseModesReturn => {
  const [modes, setModes] = React.useState<ModeSchema[]>(DEFAULT_MODES);
  const [loading, setLoading] = React.useState<boolean>(false);
  const [fromBackend, setFromBackend] = React.useState<boolean>(false);

  const baseUrl = params?.baseUrl;
  const customHeaders = params?.customHeaders;

  React.useEffect(() => {
    if (!baseUrl) {
      return;
    }

    let cancelled = false;
    setLoading(true);

    getModes(baseUrl, customHeaders)
      .then((fetched) => {
        if (cancelled) return;
        if (fetched) {
          setModes(fetched);
          setFromBackend(true);
        } else {
          setModes(DEFAULT_MODES);
          setFromBackend(false);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [baseUrl]);

  return { modes, loading, fromBackend };
};
