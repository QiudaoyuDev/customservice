package com.hardwareai.support.troubleshoot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlowVersionServiceTest {

    @Test
    void loadsAndOrdersNodesFromImmutableSnapshot() {
        var repository = mock(TroubleshootFlowVersionSnapshotRepository.class);
        var service = new FlowVersionService(repository, new ObjectMapper());
        var flowId = UUID.randomUUID();
        var snapshot = new TroubleshootFlowVersionSnapshot(flowId, 2, """
                {"nodes":[
                  {"nodeKey":"end","nodeType":"END","prompt":"Done","risk":"LOW","expectedInput":"none","safetyStop":false,"sourceRefs":[],"orderIndex":1},
                  {"nodeKey":"start","nodeType":"QUESTION","prompt":"Power on?","risk":"LOW","expectedInput":"yes_no_unknown","branchYes":"end","safetyStop":false,"sourceRefs":["K1"],"orderIndex":0}
                ]}
                """);
        when(repository.findAllByFlowIdOrderByVersionNoDesc(flowId)).thenReturn(List.of(snapshot));

        var definition = service.latest(flowId);

        assertEquals(snapshot.id(), definition.snapshotId());
        assertEquals(flowId, definition.flowId());
        assertEquals("start", definition.start().nodeKey());
        assertEquals(List.of("start", "end"), definition.nodes().stream().map(FlowVersionService.Node::nodeKey).toList());
    }

    @Test
    void rejectsMalformedSnapshotBeforeItCanDriveConversationState() {
        var repository = mock(TroubleshootFlowVersionSnapshotRepository.class);
        var service = new FlowVersionService(repository, new ObjectMapper());
        var flowId = UUID.randomUUID();
        when(repository.findAllByFlowIdOrderByVersionNoDesc(flowId))
                .thenReturn(List.of(new TroubleshootFlowVersionSnapshot(flowId, 1, "not-json")));

        assertThrows(IllegalStateException.class, () -> service.latest(flowId));
    }
}
