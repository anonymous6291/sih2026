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

import java.net.URI;

public class ModelConfigValidator {
    private static final int MAX_TOKENS_MIN_LIMIT = 1024;
    private static final int MAX_TOKENS_MAX_LIMIT = 81920;
    private static final float MIN_TEMPERATURE = 0.0F;
    private static final float MAX_TEMPERATURE = 1.0F;

    public void assertValidConfig(Model.ModelConfig modelConfig) {
        if (modelConfig.max_tokens() < MAX_TOKENS_MIN_LIMIT || modelConfig.max_tokens() > MAX_TOKENS_MAX_LIMIT) {
            throw new IllegalArgumentException("Invalid configuration. \"max_tokens\" must be in range [" + MAX_TOKENS_MIN_LIMIT + ", " + MAX_TOKENS_MAX_LIMIT + "] .");
        }
        if (modelConfig.temperature() < MIN_TEMPERATURE || modelConfig.temperature() > MAX_TEMPERATURE) {
            throw new IllegalArgumentException("Invalid configuration. \"temperature\" must be in range [" + MIN_TEMPERATURE + ", " + MAX_TEMPERATURE + "] .");
        }
        try {
            URI.create(modelConfig.model_url());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid configuration. \"model_url\" is not a valid URL.");
        }
    }
}
