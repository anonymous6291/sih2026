package sih2026.web.admin;

/*
 * Copyright (c) 2026 Anish Kumar Shaw
 *
 * Licensed under the MIT License.
 * See the LICENSE file in the project root for license terms.
 *
 * Original project:
 * https://github.com/anonymous6291/sih2026
 */

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sih2026.agent.Agent;
import sih2026.agent.model.central.Model;

import java.util.List;

@RestController
@RequestMapping("/admin/model")
public class ManageModels {
    private final Agent agent;

    public ManageModels(Agent agent) {
        this.agent = agent;
    }

    @GetMapping("/list")
    public ModelsConfigs listModelConfigs() {
        return new ModelsConfigs(agent.getModelsConfigs());
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerModel(@RequestBody Model.ModelConfig modelConfig) {
        try {
            agent.registerModel(modelConfig);
            return ResponseEntity.ok("Model added.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/deregister/{modelId}")
    public ResponseEntity<String> deregisterModel(@PathVariable("modelId") String modelId) {
        try {
            agent.deregisterModel(modelId);
            return ResponseEntity.ok("Model removed.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    public record ModelsConfigs(List<Model.ModelConfig> configs) {
    }
}
