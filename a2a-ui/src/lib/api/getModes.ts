import { createA2AProxyFetch } from "@/lib/api/proxy-fetch";
import { ModeSchema } from "@/types/qa";

/**
 * Fetches the generation-settings schema from the orchestrator's GET /api/modes,
 * routed through the Next.js proxy (same as all other backend calls).
 *
 * @param baseUrl - orchestrator base URL (the active agent's url)
 * @returns the modes schema, or null if unavailable (caller falls back to local config)
 */
export const getModes = async (
  baseUrl: string,
  customHeaders?: Record<string, string>
): Promise<ModeSchema[] | null> => {
  try {
    const proxyFetch: typeof fetch = createA2AProxyFetch(customHeaders);
    const response: Response = await proxyFetch(`${baseUrl.replace(/\/$/, "")}/api/modes`);

    if (!response.ok) {
      return null;
    }

    const modes: ModeSchema[] = await response.json();
    return Array.isArray(modes) && modes.length > 0 ? modes : null;
  } catch {
    return null;
  }
};
