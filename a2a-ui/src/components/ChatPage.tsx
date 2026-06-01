"use client";

import { ArrowBack } from "@mui/icons-material";
import { Box, Button, Toolbar } from "@mui/material";
import { styled } from "@mui/material/styles";
import React from "react";

import { AppBar } from "@/components/appbar/AppBar";
import { Chat } from "@/components/chat/Chat";
import { PreliminarySettings } from "@/components/settings/PreliminarySettings";
import { Sidebar, drawerWidth } from "@/components/sidebar/Sidebar";
import { useChat } from "@/hooks/useChat";
import { useModes } from "@/hooks/useModes";
import { QaMetadata } from "@/types/qa";

const Main = styled("main", { shouldForwardProp: (prop) => prop !== "open" })<{
  open?: boolean;
}>(({ theme }) => ({
  flexGrow: 1,
  marginLeft: `-${drawerWidth}px`,
  transition: theme.transitions.create("margin", {
    easing: theme.transitions.easing.sharp,
    duration: theme.transitions.duration.leavingScreen,
  }),
  variants: [
    {
      props: ({ open }) => open,
      style: {
        marginLeft: 0,
        transition: theme.transitions.create("margin", {
          easing: theme.transitions.easing.easeOut,
          duration: theme.transitions.duration.enteringScreen,
        }),
      },
    },
  ],
}));

export const ChatPage: React.FC = () => {
  const [sidebarOpen, setSidebarOpen] = React.useState<boolean>(true);

  const chat = useChat();
  const modes = useModes({
    baseUrl: chat.agents.activeAgent?.url,
    customHeaders: chat.settings.getHeadersObject(),
  });

  // Settings screen is the start screen: shown whenever there is no active chat.
  const showSettings: boolean = !chat.activeChatContext;

  const handleSettingsSubmit = (requirement: string, metadata: QaMetadata): void => {
    chat.handleSendMessage(requirement, metadata as unknown as Record<string, unknown>);
  };

  return (
    <Box sx={{ display: "flex" }}>
      <AppBar
        agents={chat.agents.agents}
        activeAgent={chat.agents.activeAgent}
        sidebarOpen={sidebarOpen}
        customHeaders={chat.settings.customHeaders}
        addAgentByUrl={chat.agents.addAgentByUrl}
        onAgentSelect={chat.handleAgentSelect}
        onToggleSidebar={() => setSidebarOpen(true)}
        onNewChat={chat.handleNewChat}
        onAddHeader={chat.settings.addCustomHeader}
        onUpdateHeader={chat.settings.updateCustomHeader}
        onRemoveHeader={chat.settings.removeCustomHeader}
      />

      <Sidebar
        open={sidebarOpen}
        chatContexts={chat.chatContexts.chatContexts}
        selectedContextId={chat.selected.selectedContextId}
        selectedTaskId={chat.selected.selectedTaskId}
        selectedArtifactId={chat.selected.selectedArtifactId}
        onContextSelect={chat.handleContextSelect}
        onTaskSelect={chat.handleTaskSelect}
        onArtifactSelect={chat.handleArtifactSelect}
        onNewChat={chat.handleNewChat}
        onClose={() => setSidebarOpen(false)}
      />

      <Main open={sidebarOpen}>
        <Toolbar />

        <Box
          sx={{
            flex: 1,
            display: "flex",
            flexDirection: "column",
            height: "calc(100vh - 64px)", // `Toolbar` height
            overflow: "hidden",
          }}
        >
          {showSettings ? (
            <PreliminarySettings
              modes={modes.modes}
              loading={modes.loading}
              canSubmit={!!chat.agents.activeAgent}
              onSubmit={handleSettingsSubmit}
            />
          ) : (
            <>
              <Box sx={{ px: 2, pt: 1 }}>
                <Button
                  startIcon={<ArrowBack />}
                  onClick={chat.handleNewChat}
                  sx={{ textTransform: "none", color: "text.primary" }}
                >
                  Новая генерация
                </Button>
              </Box>
              <Box sx={{ flex: 1, minHeight: 0 }}>
                <Chat
                  activeChatContext={chat.activeChatContext}
                  scrollToTaskId={chat.scrolling.scrollToTaskId}
                  scrollToArtifactId={chat.scrolling.scrollToArtifactId}
                  currentMessageText={chat.currentMessageText}
                  autoFocusChatTextField={chat.autoFocusChatTextField}
                  onSendMessage={chat.handleSendMessage}
                  onChatTextFieldChange={chat.handleMessageTextChange}
                />
              </Box>
            </>
          )}
        </Box>
      </Main>
    </Box>
  );
};
