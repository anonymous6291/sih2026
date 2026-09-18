package sih2026.sandbox;

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
import sih2026.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class SandboxCodeExecutorContainerManager {
    private static final String IMAGE_NAME = "sandbox_code_executor:1.0";
    private static final String CONTAINER_MEMORY_LIMIT = "--memory=1g";
    private static final String CONTAINER_STARTING_NAME = "sandbox_code_executor";
    private static final String CONTAINER_PORT = "8080";
    private static final String HOST_URL_PROTOCOL_IP = "http://127.0.0.1:";
    private static final int HOST_PORT_STARTING = 9500;
    private static final String EXECUTE_URL = "/execute";
    private static final String PING_URL = "/isAlive";
    private static final int RETRY_LIMIT = 6;
    private static final Duration PING_DELAY = Duration.ofMillis(500);

    private final Map<UserRole, ContainerData> containerMappings;
    private final Logger logger;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    SandboxCodeExecutorContainerManager(Logger logger) {
        this.logger = logger;
        containerMappings = new HashMap<>();

        int containerNumber = 0;

        for (UserRole userRole : UserRole.values()) {
            containerMappings.put(
                    userRole,
                    new ContainerData(
                            CONTAINER_STARTING_NAME + containerNumber,
                            userRole,
                            containerNumber,
                            HOST_PORT_STARTING + containerNumber
                    )
            );
            containerNumber++;
        }
    }

    private String getHostURL(int hostPort, String path) {
        return HOST_URL_PROTOCOL_IP + hostPort + path;
    }

    public String sendData(UserRole userRole, String data) {
        ContainerData containerData = containerMappings.get(userRole);

        try {
            if (containerData == null) {
                return null;
            }

            if (!isContainerUp(containerData)) {

                removeContainer(containerData);

                addContainer(containerData);

                int tryCount = 0;

                while (tryCount <= RETRY_LIMIT) {
                    try {
                        Thread.sleep(PING_DELAY);
                    } catch (InterruptedException _) {
                    }

                    if (isContainerUp(containerData)) {
                        break;
                    }
                    tryCount++;
                }

                if (tryCount > RETRY_LIMIT) {
                    throw new RuntimeException();
                }
            }

            HttpRequest httpRequest = HttpRequest
                    .newBuilder(
                            URI.create(getHostURL(containerData.hostPort(), EXECUTE_URL))
                    )
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(data))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() != 200) {
                return null;
            }

            return httpResponse.body();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Process startDockerProcess(String... cmd) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);

        return processBuilder.start();
    }

    private void addContainer(ContainerData containerData) throws Exception {
        logger.logInfo("Starting docker container with name [" + containerData.containerName() + "].");

        Process process = startDockerProcess(
                "docker",
                "run",
                "-d",
                "--name",
                containerData.containerName(),
                "-p",
                containerData.hostPort() + ":" + CONTAINER_PORT,
                CONTAINER_MEMORY_LIMIT,
                IMAGE_NAME
        );

        while (process.isAlive()) ;
    }

    private void removeContainer(ContainerData containerData) throws Exception {
        logger.logInfo("Stopping docker container with name [" + containerData.containerName() + "].");

        Process process = startDockerProcess(
                "docker",
                "rm",
                containerData.containerName()
        );

        while (process.isAlive()) ;
    }

    private boolean isContainerUp(ContainerData containerData) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(
                    URI.create(getHostURL(containerData.hostPort(), PING_URL))
            ).GET().build();

            HttpResponse<Void> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding());

            return httpResponse.statusCode() == 200;
        } catch (Exception _) {
        }
        return false;
    }

    record ContainerData(String containerName, UserRole userRole, int id, int hostPort) {
    }
}
