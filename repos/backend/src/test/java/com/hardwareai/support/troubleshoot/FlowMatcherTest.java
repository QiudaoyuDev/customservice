package com.hardwareai.support.troubleshoot;

import org.junit.jupiter.api.Test;

import com.hardwareai.support.llm.Intent;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlowMatcherTest {

    @Test
    void comparesNumericFirmwareSegmentsRatherThanLexicalStrings() {
        assertTrue(FlowMatcher.withinFirmwareRange("1.10.0", "1.2.0", "1.12.0"));
        assertFalse(FlowMatcher.withinFirmwareRange("1.1.0", "1.2.0", null));
        assertFalse(FlowMatcher.withinFirmwareRange("unparseable", "1.2.0", "1.3.0"));
    }

    @Test
    void doesNotRequireFirmwareWhenFlowHasNoFirmwareConstraint() {
        assertTrue(FlowMatcher.withinFirmwareRange(null, null, null));
        assertFalse(FlowMatcher.withinFirmwareRange(null, "1.0.0", null));
    }

    @Test
    void choosesTheMostSpecificApplicablePublishedFlow() {
        var repository = mock(TroubleshootFlowRepository.class);
        var matcher = new FlowMatcher(repository);
        var tenant = UUID.randomUUID();
        var product = UUID.randomUUID();
        var variant = UUID.randomUUID();
        var generic = published(tenant, product, null, null, null, null, 0);
        var specific = published(tenant, product, variant, "R2", "1.2.0", "E42", 5);
        when(repository.findPublishedCandidates(tenant, product, "US", "en", Intent.ERROR_CODE))
                .thenReturn(List.of(generic, specific));

        var selected = matcher.match(tenant, new FlowMatchScope(product, variant, "R2", "1.10.0"), "US", "en",
                Intent.ERROR_CODE, "The display shows E42", null);

        assertEquals(specific.id(), selected.orElseThrow().id());
    }

    private TroubleshootFlow published(UUID tenant, UUID product, UUID variant, String revision, String firmwareMin,
                                       String triggerPhrase, int priority) {
        var flow = new TroubleshootFlow(tenant, "Flow", Intent.ERROR_CODE, product, "US", "en");
        flow.update("Flow", Intent.ERROR_CODE, product, variant, revision, "US", "en", firmwareMin, null, triggerPhrase, priority);
        flow.submit();
        flow.approve(UUID.randomUUID());
        flow.publish(UUID.randomUUID());
        return flow;
    }
}
