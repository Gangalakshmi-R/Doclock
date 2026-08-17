package com.doclock.backend.config;

import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

    @Bean
    public TransformersEmbeddingModel embeddingModel() {

        TransformersEmbeddingModel model =
                new TransformersEmbeddingModel();

        return model;
    }
}