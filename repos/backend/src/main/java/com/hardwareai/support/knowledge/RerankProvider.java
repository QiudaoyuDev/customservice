package com.hardwareai.support.knowledge;

import java.util.List;

/** Reranks evidence candidates without owning retrieval scope or policy. */
public interface RerankProvider {
    List<Integer> rank(String query, List<String> passages);
}
