package com.hardwareai.support.troubleshoot;
import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*; import static com.hardwareai.support.troubleshoot.TroubleshootTypes.*;
class TroubleshootStateMachineTest { private final TroubleshootStateMachine machine=new TroubleshootStateMachine();
 @Test void safetyStopsAlwaysEscalate(){assertTrue(machine.next(NodeType.OPERATION,Risk.HIGH,Reply.YES,"a","b","c",0).escalated());}
 @Test void twoFailuresEscalate(){assertTrue(machine.next(NodeType.QUESTION,Risk.LOW,Reply.NO,"a","b","c",2).escalated());}
 @Test void normalBranchUsesServerMapping(){assertEquals("b",machine.next(NodeType.QUESTION,Risk.LOW,Reply.NO,"a","b","c",0).nextNodeKey());}}
