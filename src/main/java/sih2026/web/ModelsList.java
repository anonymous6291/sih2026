package sih2026.web;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sih2026.agent.Agent;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
public class ModelsList {
    private final Agent agent;

    public ModelsList(Agent agent) {
        this.agent = agent;
    }

    @GetMapping("/models")
    public Models getModelsList() {
        List<ModelData> modelsData = new LinkedList<>();
        Map<String, String> models = agent.getModels();
        models.forEach((id, name) -> modelsData.add(new ModelData(id, name)));
        return new Models(modelsData);
    }

    public record Models(List<ModelData> models) {
    }

    public record ModelData(String id, String name) {
    }
}
