package com.hardwareai.support.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntentClassifierTest {
    private final IntentClassifier classifier = new IntentClassifier();

    @Test
    void blocksSafetyBeforeAnyGeneration() {
        assertEquals(Intent.SAFETY_RISK, classifier.classify("There is smoke and a fire smell"));
    }

    @Test
    void recognizesHumanRequest() {
        assertEquals(Intent.HUMAN_REQUEST, classifier.classify("请转人工客服"));
    }

    @Test
    void classifiesWaterIngressAsSafetyAndAmbiguousTextAsUnknown() {
        assertEquals(Intent.SAFETY_RISK, classifier.classify("设备进水后还能使用吗"));
        assertEquals(Intent.UNKNOWN, classifier.classify("asdf qwerty"));
    }
}
