package sih2026.agent.model.central;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import sih2026.logging.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ModelManager {
    private static final String CONFIG_FILE_NAME = "central_models.json";
    private final Logger logger;
    private final ObjectMapper jsonParser = new ObjectMapper();
    private final Map<String, Model> models = new ConcurrentHashMap<>(1);
    private final AtomicReference<Model> defaultModel = new AtomicReference<>(null);
    private final ModelConfigValidator modelConfigValidator = new ModelConfigValidator();

    ModelManager(Logger logger) throws Exception {
        this.logger = logger;
        try (BufferedReader bufferedReader = Files.newBufferedReader(Path.of(CONFIG_FILE_NAME))) {
            String data = bufferedReader.readAllAsString();

            if (data.endsWith("\n")) {
                data = data.substring(0, data.length() - 1);
            }

            Models modelsData = jsonParser.readValue(data, Models.class);

            modelsData.models().forEach(modelData -> {
                try {
                    modelConfigValidator.assertValidConfig(modelData);

                    String id = modelData.model_id();

                    if (models.containsKey(id)) {
                        throw new IllegalArgumentException("Duplicate model_ids found in the config file of models.");
                    }

                    Model model = new Model(modelData);

                    if (modelData.is_default()) {
                        defaultModel.set(model);
                    }

                    models.put(id, model);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public ModelCommunicator.Response sendConversation(String modelId, String prompt, Conversation conversation) throws Exception {
        try {
            Model model = models.get(modelId);

            if (model == null) {
                throw new IllegalArgumentException("Model with modeId [" + modelId + "] doesn't exist.");
            }

            return model.sendQuery(prompt, conversation);
        } catch (Exception e) {
            logger.logError(e.getMessage());
            throw e;
        }
    }

    public ModelCommunicator.Response sendQueryToDefaultModel(String prompt, Conversation conversation) throws Exception {
        try {
            Model model = defaultModel.get();

            if (model == null) {
                if (models.isEmpty()) {
                    throw new RuntimeException("No models available to process the query.");
                }

                model = models.values().stream().findFirst().get();
            }

            return model.sendQuery(prompt, conversation);
        } catch (Exception e) {
            logger.logError(e.getMessage());
            throw e;
        }
    }

    public void registerModel(Model.ModelConfig modelConfig) throws Exception {
        try {
            modelConfigValidator.assertValidConfig(modelConfig);

            models.put(modelConfig.model_id(), new Model(modelConfig));
        } catch (Exception e) {
            logger.logError("Failed to register a model with config [\n" + modelConfig.toString() + "\n] : " + e.getMessage());
            throw e;
        }
    }

    public void deregisterModel(String modelId) {
        Model model = models.remove(modelId);

        if (model == null) {
            return;
        }

        model.shutdown();
    }

    @PreDestroy
    public void overwriteConfig() {
        try {
            List<Model.ModelConfig> modelsData = models.values().stream().map(Model::getModelConfig).toList();

            Models models = new Models(modelsData);

            String config = jsonParser.writeValueAsString(models);

            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(Path.of(config))) {
                bufferedWriter.write(config);
            }
        } catch (Exception e) {
            IO.println("Error occurred while overwriting config of models. " + e);
        }
    }

    public Map<String, String> getModels() {
        Set<String> modelIds = models.keySet();

        Map<String, String> modelsMappings = new HashMap<>(modelIds.size());

        modelIds.forEach(modelId -> modelsMappings.put(modelId, models.get(modelId).getModelName()));

        return modelsMappings;
    }

    public List<Model.ModelConfig> getModelsConfig() {
        return models.values().stream().map(Model::getModelConfig).toList();
    }

    record Models(List<Model.ModelConfig> models) {
    }
}
