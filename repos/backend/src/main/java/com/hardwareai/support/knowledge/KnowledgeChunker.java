package com.hardwareai.support.knowledge;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/** Splits normalized source text on paragraph boundaries, preserving deterministic order. */
@Service
class KnowledgeChunker {
    private static final int MAX_CHARS = 1200;
    List<KnowledgeChunk> split(KnowledgeRevision revision, KnowledgeDocument document) {
        String[] paragraphs = revision.extractedText().replace("\r\n", "\n").split("\\n\\s*\\n");
        List<KnowledgeChunk> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            String clean = paragraph.strip(); if (clean.isEmpty()) continue;
            if (current.length() > 0 && current.length() + clean.length() + 2 > MAX_CHARS) {
                chunks.add(new KnowledgeChunk(revision.id(), chunks.size(), current.toString(), document.title())); current.setLength(0);
            }
            if (clean.length() > MAX_CHARS && current.length() == 0) {
                for (int start = 0; start < clean.length(); start += MAX_CHARS) {
                    chunks.add(new KnowledgeChunk(revision.id(), chunks.size(), clean.substring(start, Math.min(clean.length(), start + MAX_CHARS)), document.title()));
                }
            } else { if (current.length() > 0) current.append("\n\n"); current.append(clean); }
        }
        if (current.length() > 0) chunks.add(new KnowledgeChunk(revision.id(), chunks.size(), current.toString(), document.title()));
        if (chunks.isEmpty()) throw new IllegalStateException("No searchable text was extracted from the document");
        return chunks;
    }
}
