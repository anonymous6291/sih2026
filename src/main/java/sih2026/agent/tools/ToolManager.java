package sih2026.agent.tools;

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
import sih2026.database.user.UserRole;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ToolManager {
    private static final String TOOL_USAGE_FORMAT = """
            The first line of your output must contains the tool_name, the second line must contain the list of documentIds of documents which you want
            to forward to the tool (empty if none) and at last you can append your message or description providing the details. Remember, tool_name and
            list of documentIds is necessary and must never be missing. If you don't want to attach any documents then simply keep the list empty,
            don't remove the list!
            
            Points to remember strictly:
            1) You can use at most one tool at a time.
            2) First line should be tool_name, the second line should be list of documentIds (empty if none) and at last your description or message.
            3) Strictly comply to the given tool usage format, failure to do that will result in serious error.
            
            Tool usage format:
            
            tool_name:
            [documentId1, documentId2, ......]
            extra information or description
            
            List of available tools along with their description and usage example:
            
            """;
    private final Map<String, Tool> toolMap = new ConcurrentHashMap<>();

    public void registerTool(String toolName, Tool tool) {
        if (toolMap.containsKey(toolName)) {
            throw new IllegalArgumentException("Tool with name [" + toolName + "] already exists.");
        }
        toolMap.put(toolName, tool);
    }

    public String getAllToolUsageDescription() {
        StringBuilder description = new StringBuilder(TOOL_USAGE_FORMAT);
        description.append("\n");
        for (Tool tool : toolMap.values()) {
            description.append(tool.getUsageDescription());
            description.append("\n------------------------------------------------------------\n");
        }
        return description.toString();
    }

    public Tool getTool(String toolName) {
        return toolMap.get(toolName);
    }

    public Tool.ToolResponse useTool(String username, UserRole userRole, String toolName, String request) throws Exception {
        if (toolMap.containsKey(toolName)) {
            throw new IllegalArgumentException("Tool with name [" + toolName + "] doesn't exist.");
        }
        return toolMap.get(toolName).performTask(username, userRole, request);
    }
}

/*

> Read documents

> Write code

> Use RAG


 */