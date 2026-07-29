package com.hardwareai.support.knowledge;

import java.util.List;

/** Local or remote embedding adapter used by indexing and vector retrieval. */
public interface EmbeddingProvider {
    List<Double> embed(String text);
}
