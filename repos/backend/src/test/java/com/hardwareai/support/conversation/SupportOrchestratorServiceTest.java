package com.hardwareai.support.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hardwareai.support.analytics.OperationalEventService;
import com.hardwareai.support.llm.Intent;
import com.hardwareai.support.llm.ModelConfigurationService;
import com.hardwareai.support.retrieval.RetrievalService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SupportOrchestratorServiceTest {
    @Test
    void refusesToGenerateWhenNoEvidencePassesTheServerGate() {
        var retrieval = mock(RetrievalService.class);
        var messages = mock(ConversationMessageRepository.class);
        var traces = mock(AnswerTraceRepository.class);
        var citations = mock(AnswerCitationRepository.class);
        var models = mock(ModelConfigurationService.class);
        var events = mock(OperationalEventService.class);
        when(retrieval.retrieve(any())).thenReturn(new RetrievalService.RetrievalResult(List.of(), false));
        when(messages.save(any())).thenAnswer(call -> call.getArgument(0));
        when(traces.save(any())).thenAnswer(call -> call.getArgument(0));
        var service = new SupportOrchestratorService(retrieval, messages, traces, citations, models, new ObjectMapper(), events);
        var conversation = mock(Conversation.class);
        var context = mock(ConversationProductContext.class);
        var question = new ConversationMessage(UUID.randomUUID(), "How do I repair it?", null);
        when(conversation.tenantId()).thenReturn(UUID.randomUUID());
        when(conversation.id()).thenReturn(UUID.randomUUID());
        when(conversation.region()).thenReturn("EU");
        when(conversation.language()).thenReturn("en");
        when(context.productModelId()).thenReturn(UUID.randomUUID());

        var result = service.answer(conversation, context, question, Intent.CONSULTATION);

        assertEquals("NO_EVIDENCE", result.outcome());
        verifyNoInteractions(models);
    }
}
