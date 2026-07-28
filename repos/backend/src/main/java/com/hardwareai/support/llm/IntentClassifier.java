package com.hardwareai.support.llm;

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Deterministic guard precedes any model call; model classification may only refine safe traffic.
 */
@Component
public class IntentClassifier {
    public Intent classify(String text) {
        String value = text.toLowerCase(Locale.ROOT);
        if (contains(value, "fire", "smoke", "burn", "shock", "电击", "起火", "冒烟", "燃烧"))
            return Intent.SAFETY_RISK;
        if (contains(value, "human", "agent", "representative", "人工", "客服")) return Intent.HUMAN_REQUEST;
        if (contains(value, "warranty", "保修")) return Intent.WARRANTY;
        if (contains(value, "complaint", "投诉")) return Intent.COMPLAINT;
        if (contains(value, "error", "code", "错误码")) return Intent.ERROR_CODE;
        if (contains(value, "install", "setup", "安装", "配置")) return Intent.INSTALLATION;
        if (contains(value, "broken", "fault", "故障", "无法")) return Intent.TROUBLESHOOTING;
        return Intent.CONSULTATION;
    }

    private boolean contains(String value, String... words) {
        for (String word : words) if (value.contains(word)) return true;
        return false;
    }
}
