package com.talentgrid.demand.ai;

public interface AiEmbeddingProvider {

    String name();

    float[] embedContent(String text);
}
