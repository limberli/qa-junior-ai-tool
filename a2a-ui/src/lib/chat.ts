import { AgentCard, MessageSendParams, TaskState } from "@a2a-js/sdk";
import { v4 as uuidv4 } from "uuid";

import { ChatContext } from "@/types/chat";

// Terminal states that should reset tasks
export const terminalStates: TaskState[] = [
  "completed",
  "canceled",
  "failed",
  "rejected",
  "unknown",
];

export const createMessageSendParams = (
  messageText: string,
  contextId: string,
  taskId?: string,
  metadata?: Record<string, unknown>
): MessageSendParams => ({
  message: {
    contextId,
    kind: "message",
    messageId: uuidv4(),
    parts: [{ kind: "text", text: messageText }],
    role: "user",
    ...(taskId && { taskId }),
  },
  // Generation settings travel here (e.g. { qa: { mode, selections } }); the orchestrator
  // reads params.metadata and forwards it to the tester agent.
  ...(metadata && { metadata }),
});

export const createTempChatContext = (contextId: string, agent: AgentCard): ChatContext => ({
  contextId,
  agent,
  messagesAndTasks: [],
  pendingMessage: null,
  messageText: "",
  loading: true,
});
