package sih2026.agent;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import org.springframework.stereotype.Service;
import sih2026.agent.model.central.*;
import sih2026.agent.tools.Tool;
import sih2026.agent.tools.ToolManager;
import sih2026.database.chat.ChatMessage;
import sih2026.database.chat.ChatStorageHandler;
import sih2026.database.chat.DocumentNameIdPair;
import sih2026.database.user.UserRole;
import sih2026.logging.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class Agent {
    private static final int MAX_LOOP_LENGTH = 5;
    private static final int MAX_CONVERSATION_SIZE = 6;

    private static final String MESSAGE_USER_ROLE_STRING = "user";

    private static final String AGENT_PROMPT_HEADING = """
            Your are an AI agent operating in {OS_NAME} operating system. I am a controller and I will give you a task. You have to
            use the given tools to perform the task. You can break the task into smaller steps and use one tool at a time.
            
            """.replaceAll("\\{OS_NAME}", System.getProperty("os.name"));

    private static final String SUMMARY_PROMPT = """
            You are a personal assistant of an AI Agent. Your task is to generate short summary of the long conversation, such that none of the
            critical information is lost. Preserve the critical information while eliminating the unnecessary information.
            """;

    private final Logger logger;
    private final ChatStorageHandler chatStorageHandler;
    private final ModelManager modelManager;
    private final ToolManager toolManager;

    public Agent(Logger logger, ChatStorageHandler chatStorageHandler, ModelManager modelManager, ToolManager toolManager) {
        this.logger = logger;
        this.chatStorageHandler = chatStorageHandler;
        this.modelManager = modelManager;
        this.toolManager = toolManager;
    }

    public void process(AgentRequestData agentRequestData, Consumer<AgentResponseData> eventConsumer) {
        AgentResponseData finalResponseData;
        try {
            int count = 0;

            String fullPrompt = generateFullPrompt();

            String query = agentRequestData.query();
            String modelId = agentRequestData.model_id();
            String username = agentRequestData.username();
            UserRole userRole = agentRequestData.userRole();

            List<Message> conversationMessages = new LinkedList<>();

            Conversation conversation = new Conversation(conversationMessages);

            String previousChatSummary = chatStorageHandler.getChatSummary(username, userRole, agentRequestData.chat_id());

            if (!previousChatSummary.isBlank()) {
                conversationMessages.add(
                        new Message(
                                "summary",
                                previousChatSummary
                        )
                );
            }

            conversationMessages.add(
                    new Message(
                            MESSAGE_USER_ROLE_STRING,
                            appendQueryAndDocumentNameId(
                                    query,
                                    agentRequestData.documents()
                            )
                    )
            );

            String finalResponse = "";

            List<DocumentNameIdPair> documentNameIdPairs = new LinkedList<>();

            while (count < MAX_LOOP_LENGTH) {
                eventConsumer.accept(
                        new AgentResponseData(
                                "success",
                                true,
                                true,
                                "Thinking....",
                                agentRequestData.chat_id(),
                                agentRequestData.message_id(),
                                List.of()
                        )
                );

                ModelCommunicator.Response modelResponse =
                        modelManager.sendConversation(
                                modelId,
                                fullPrompt,
                                conversation
                        );

                String modelResponseRole = modelResponse.role();
                String modelResponseContent = modelResponse.content();

                int newLineIndex = modelResponseContent.indexOf("\n");

                String toolName;

                if (newLineIndex == -1 || (toolName = modelResponseContent.substring(0, newLineIndex)).isBlank()) {
                    finalResponse = modelResponseContent;
                    break;
                }

                String toolRequest = modelResponseContent.substring(newLineIndex + 1);

                if (toolName.endsWith(":")) {
                    toolName = toolName.substring(0, toolName.length() - 1);
                }

                Tool tool = toolManager.getTool(toolName);

                if (tool == null) {
                    throw new RuntimeException("Invalid model response [" + modelResponseContent + "], model used tool which doesn't exist.");
                }

                conversationMessages.add(
                        new Message(
                                modelResponseRole,
                                modelResponseContent
                        )
                );

                eventConsumer.accept(
                        new AgentResponseData(
                                "success",
                                true,
                                true,
                                tool.getRunningDescription(),
                                agentRequestData.chat_id(),
                                agentRequestData.message_id(),
                                List.of()
                        )
                );

                Tool.ToolResponse toolResponse = tool.performTask(
                        username,
                        userRole,
                        toolRequest
                );

                if (toolResponse.stop()) {
                    finalResponse = toolResponse.response();

                    toolResponse
                            .documents()
                            .forEach(docNameIdPair ->

                                    documentNameIdPairs
                                            .add(
                                                    new DocumentNameIdPair(
                                                            docNameIdPair.document_name(),
                                                            docNameIdPair.document_id()
                                                    )
                                            )
                            );
                    break;
                }

                conversationMessages.add(
                        new Message(
                                "user",
                                toolResponse.response()
                        )
                );

                if (conversationMessages.size() > MAX_CONVERSATION_SIZE) {
                    eventConsumer.accept(
                            new AgentResponseData(
                                    "success",
                                    true,
                                    true,
                                    "Summarizing....",
                                    agentRequestData.chat_id(),
                                    agentRequestData.message_id(),
                                    List.of()
                            )
                    );

                    conversation = generatePartialSummary(conversation);

                    conversationMessages = conversation.messages();
                }

                count++;
            }

            if (count >= MAX_LOOP_LENGTH) {
                throw new RuntimeException("Loop run length exceeded.");
            }

            finalResponseData = new AgentResponseData(
                    "success",
                    false,
                    false,
                    finalResponse,
                    agentRequestData.chat_id(),
                    agentRequestData.message_id(),
                    documentNameIdPairs);

            String chatSummary = ""; //generateFullSummary(conversation).messages().getFirst().content();

            chatStorageHandler.setChatSummary(
                    username,
                    userRole,
                    agentRequestData.chat_id(),
                    chatSummary
            );
        } catch (Exception e) {
            e.printStackTrace();

            logger.logError("Response generation for [" + agentRequestData + "] failed : " + e);

            finalResponseData = new AgentResponseData(
                    "error",
                    false,
                    false,
                    "Failed to process the request.",
                    agentRequestData.chat_id(),
                    agentRequestData.message_id(),
                    List.of()
            );
        }

        eventConsumer.accept(finalResponseData);
        try {
            chatStorageHandler.addChatMessage(
                    agentRequestData.username(),
                    agentRequestData.userRole(),
                    agentRequestData.chat_id(),
                    new ChatMessage(true, agentRequestData.message_id(), agentRequestData.query(), agentRequestData.documents()),
                    new ChatMessage(false, agentRequestData.message_id(), finalResponseData.message(), finalResponseData.documents())
            );
        } catch (Exception e) {
            logger.logSevere("Failed to add chat message with chat id [" + agentRequestData.chat_id() + "].");
        }
    }

    private String appendQueryAndDocumentNameId(String query, List<DocumentNameIdPair> documentNameIdPairs) {
        StringBuilder result = new StringBuilder(query);
        if (documentNameIdPairs.isEmpty()) {
            return result.toString();
        }
        result.append("\nUploaded documents and names:\n");
        for (DocumentNameIdPair nameIdPair : documentNameIdPairs) {
            result.append("Document Name: ").append(nameIdPair.document_name()).append("\n");
            result.append("Document Id: ").append(nameIdPair.document_id()).append("\n\n");
        }
        return result.toString();
    }

    private String generateFullPrompt() {
        String toolsUsageDescription = toolManager.getAllToolUsageDescription();

        return AGENT_PROMPT_HEADING + toolsUsageDescription;
    }

    private Conversation generatePartialSummary(Conversation conversation) throws Exception {
        List<Message> oldMessages = conversation.messages();

        Message firstLastMessage = oldMessages.removeLast();
        Message secondLastMessage = oldMessages.removeLast();

        Conversation newConversation = generateFullSummary(conversation);

        List<Message> newMessages = newConversation.messages();

        newMessages.add(secondLastMessage);
        newMessages.add(firstLastMessage);

        return newConversation;
    }

    private Conversation generateFullSummary(Conversation conversation) throws Exception {
        ModelCommunicator.Response response = modelManager.sendQueryToDefaultModel(SUMMARY_PROMPT, conversation);

        List<Message> newMessages = new LinkedList<>();

        newMessages.add(new Message("summary", response.content()));

        return new Conversation(newMessages);
    }

    public Map<String, String> getModels() {
        return modelManager.getModels();
    }

    public List<Model.ModelConfig> getModelsConfigs() {
        return modelManager.getModelsConfig();
    }

    public void registerModel(Model.ModelConfig modelConfig) throws Exception {
        modelManager.registerModel(modelConfig);
    }

    public void deregisterModel(String modelId) {
        modelManager.deregisterModel(modelId);
    }
}
