package com.hardwareai.support.knowledge;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits paragraph, heading and table boundaries while preserving source title path and PDF pages.
 */
@Service
class KnowledgeChunker {
    private static final int MAX_CHARS = 1200;

    List<KnowledgeChunk> split(KnowledgeRevision revision, KnowledgeDocument document) {
        String[] paragraphs = revision.extractedText().replace("\r\n", "\n").split("\\n\\s*\\n");
        List<KnowledgeChunk> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int page = 1;
        String titlePath = document.title();
        for (String paragraph : paragraphs) {
            String raw = paragraph.replace("\r", "");
            if (raw.startsWith("\fPAGE:")) {
                int newline = raw.indexOf('\n');
                String pageToken = newline < 0 ? raw.substring(6) : raw.substring(6, newline).strip();
                try { page = Integer.parseInt(pageToken); } catch (NumberFormatException ignored) { }
                raw = newline < 0 ? "" : raw.substring(newline + 1);
            }
            String clean = raw.strip();
            if (clean.isEmpty()) continue;
            if (clean.startsWith("# ")) {
                if (current.length() > 0) {
                    chunks.add(new KnowledgeChunk(revision.id(), chunks.size(), current.toString(), document.title(), titlePath, page, page));
                    current.setLength(0);
                }
                titlePath = document.title() + " / " + clean.substring(2).strip();
                continue;
            }
            if (current.length() > 0 && current.length() + clean.length() + 2 > MAX_CHARS) {
                chunks.add(new KnowledgeChunk(revision.id(), chunks.size(), current.toString(), document.title(), titlePath, page, page));
                current.setLength(0);
            }
            if (clean.length() > MAX_CHARS && current.length() == 0) {
                for (int start = 0; start < clean.length(); start += MAX_CHARS) {
                    chunks.add(new KnowledgeChunk(revision.id(), chunks.size(), clean.substring(start, Math.min(clean.length(), start + MAX_CHARS)), document.title(), titlePath, page, page));
                }
            } else {
                if (current.length() > 0) current.append("\n\n");
                current.append(clean);
            }
        }
        if (current.length() > 0)
            chunks.add(new KnowledgeChunk(revision.id(), chunks.size(), current.toString(), document.title(), titlePath, page, page));
        if (chunks.isEmpty()) throw new IllegalStateException("No searchable text was extracted from the document");
        return chunks;
    }
}
