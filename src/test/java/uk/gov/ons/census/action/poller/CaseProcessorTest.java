package uk.gov.ons.census.action.poller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.ons.census.action.builders.CaseSelectedBuilder;
import uk.gov.ons.census.action.builders.FieldworkFollowupBuilder;
import uk.gov.ons.census.action.builders.PrintFileDtoBuilder;
import uk.gov.ons.census.action.model.dto.FieldworkFollowup;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.entity.ActionPlan;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.CaseToProcess;

@RunWith(MockitoJUnitRunner.class)
public class CaseProcessorTest {
  @Mock private FieldworkFollowupBuilder fieldworkFollowupBuilder;

  @Mock private PrintFileDtoBuilder printFileDtoBuilder;

  @Mock private CaseSelectedBuilder caseSelectedBuilder;

  @Mock private RabbitTemplate rabbitTemplate;

  @InjectMocks private CaseProcessor underTest;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  @Value("${queueconfig.action-case-exchange}")
  private String actionCaseExchange;

  @Value("${scheduler.frequency}")
  private int chunkSize;

  @Test
  public void testProcessQueuedCasesForPrinter() {
    // Given
    ActionRule actionRule = new ActionRule();
    actionRule.setId(UUID.randomUUID());
    actionRule.setActionType(ActionType.ICL1E);
    Case caze = new Case();
    CaseToProcess caseToProcess = new CaseToProcess();
    caseToProcess.setActionRule(actionRule);
    caseToProcess.setCaze(caze);
    caseToProcess.setBatchId(UUID.randomUUID());
    caseToProcess.setBatchQuantity(666);

    PrintFileDto printFileDto = new PrintFileDto();
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();

    when(printFileDtoBuilder.buildPrintFileDto(any(), any(), any(), any()))
        .thenReturn(printFileDto);
    when(caseSelectedBuilder.buildPrintMessage(any(), any())).thenReturn(responseManagementEvent);

    // When
    underTest.process(caseToProcess);

    // Then
    verify(printFileDtoBuilder)
        .buildPrintFileDto(
            eq(caze), eq("P_IC_ICL1"), eq(caseToProcess.getBatchId()), eq(ActionType.ICL1E));
    verify(caseSelectedBuilder)
        .buildPrintMessage(eq(printFileDto), eq(actionRule.getId().toString()));
    verify(rabbitTemplate)
        .convertAndSend(
            eq(outboundExchange),
            eq(ActionType.ICL1E.getHandler().getRoutingKey()),
            eq(printFileDto));
    verify(rabbitTemplate)
        .convertAndSend(eq(actionCaseExchange), eq(""), eq(responseManagementEvent));
  }

  @Test
  public void testProcessQueuedCasesForField() {
    // Given
    ActionPlan actionPlan = new ActionPlan();
    actionPlan.setId(UUID.randomUUID());
    ActionRule actionRule = new ActionRule();
    actionRule.setId(UUID.randomUUID());
    actionRule.setActionType(ActionType.FIELD);
    actionRule.setActionPlan(actionPlan);
    Case caze = new Case();
    CaseToProcess caseToProcess = new CaseToProcess();
    caseToProcess.setActionRule(actionRule);
    caseToProcess.setCaze(caze);
    caseToProcess.setBatchId(UUID.randomUUID());
    caseToProcess.setBatchQuantity(666);

    List<CaseToProcess> caseToProcessList = new LinkedList<>();
    caseToProcessList.add(caseToProcess);

    FieldworkFollowup fieldworkFollowup = new FieldworkFollowup();
    fieldworkFollowup.setCaseRef("123");
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();

    when(fieldworkFollowupBuilder.buildFieldworkFollowup(any(), any(), any()))
        .thenReturn(fieldworkFollowup);
    when(caseSelectedBuilder.buildFieldMessage(any(), any())).thenReturn(responseManagementEvent);

    // When
    underTest.process(caseToProcess);

    // Then
    verify(fieldworkFollowupBuilder)
        .buildFieldworkFollowup(
            eq(caze), eq(actionPlan.getId().toString()), eq(ActionType.FIELD.name()));
    verify(caseSelectedBuilder).buildFieldMessage(eq("123"), eq(actionRule.getId()));
    verify(rabbitTemplate)
        .convertAndSend(
            eq(outboundExchange),
            eq(ActionType.FIELD.getHandler().getRoutingKey()),
            eq(fieldworkFollowup));
    verify(rabbitTemplate)
        .convertAndSend(eq(actionCaseExchange), eq(""), eq(responseManagementEvent));
  }
}
