package uk.gov.ons.census.action.schedule;

import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
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
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;
import uk.gov.ons.census.action.model.repository.CustomCaseRepository;

public class ActionRuleProcessorTest {
  private static final String OUTBOUND_EXCHANGE = "OUTBOUND_EXCHANGE";
  private static final String ACTION_TO_CASE_EXCHANGE = "ACTION_TO_CASE_EXCHANGE";

  private final ActionRuleRepository actionRuleRepo = mock(ActionRuleRepository.class);
  private final CustomCaseRepository customCaseRepository = mock(CustomCaseRepository.class);
  private final FieldworkFollowupBuilder fieldworkFollowupBuilder =
      mock(FieldworkFollowupBuilder.class);
  private final CaseSelectedBuilder caseSelectedBuilder = mock(CaseSelectedBuilder.class);

  private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
  private final PrintFileDtoBuilder printFileDtoBuilder = mock(PrintFileDtoBuilder.class);

  @Test
  public void testExecuteClassifiers() {
    // Given
    ActionRule actionRule = setUpActionRule(ActionType.ICL1E);
    Map<String, List<String>> classifiers = new HashMap<>();
    List<String> columnValues = Arrays.asList("a", "b", "c");
    classifiers.put("A_Column", columnValues);
    actionRule.setClassifiers(classifiers);

    final int expectedCaseCount = 47;

    List<Case> cases = getRandomCases(expectedCaseCount);

    when(printFileDtoBuilder.buildPrintFileDto(
            any(Case.class), any(String.class), any(UUID.class), eq(ActionType.ICL1E)))
        .thenReturn(new PrintFileDto());

    when(caseSelectedBuilder.buildPrintMessage(any(PrintFileDto.class), any(UUID.class)))
        .thenReturn(new ResponseManagementEvent());

    when(customCaseRepository.streamAll(any(Specification.class))).thenReturn(cases.stream());

    // when
    ActionRuleProcessor actionRuleProcessor =
        new ActionRuleProcessor(
            actionRuleRepo,
            fieldworkFollowupBuilder,
            printFileDtoBuilder,
            caseSelectedBuilder,
            rabbitTemplate,
            customCaseRepository);
    ReflectionTestUtils.setField(actionRuleProcessor, "outboundExchange", OUTBOUND_EXCHANGE);
    ReflectionTestUtils.setField(
        actionRuleProcessor, "actionCaseExchange", ACTION_TO_CASE_EXCHANGE);
    actionRuleProcessor.createScheduledActions(actionRule);

    // then
    ArgumentCaptor<ActionRule> actionRuleCaptor = ArgumentCaptor.forClass(ActionRule.class);
    verify(actionRuleRepo, times(1)).save(actionRuleCaptor.capture());
    ActionRule actualActionRule = actionRuleCaptor.getAllValues().get(0);
    actionRule.setHasTriggered(true);
    Assertions.assertThat(actualActionRule).isEqualTo(actionRule);
    verify(rabbitTemplate, times(expectedCaseCount))
        .convertAndSend(
            eq(OUTBOUND_EXCHANGE), eq("Action.Printer.binding"), any(PrintFileDto.class));
    verify(rabbitTemplate, times(expectedCaseCount))
        .convertAndSend(eq(ACTION_TO_CASE_EXCHANGE), eq(""), any(ResponseManagementEvent.class));
  }

  @Test
  public void testExecuteCasesField() {
    // Given
    ActionRule actionRule = setUpActionRule(ActionType.FF2QE);
    final int expectedCaseCount = 50;

    List<Case> cases = getRandomCases(expectedCaseCount);

    when(customCaseRepository.streamAll(any(Specification.class))).thenReturn(cases.stream());

    when(fieldworkFollowupBuilder.buildFieldworkFollowup(any(Case.class), eq(actionRule)))
        .thenReturn(new FieldworkFollowup());

    when(caseSelectedBuilder.buildFieldMessage(any(), any()))
        .thenReturn(new ResponseManagementEvent());

    ActionRuleProcessor actionRuleProcessor =
        new ActionRuleProcessor(
            actionRuleRepo,
            fieldworkFollowupBuilder,
            printFileDtoBuilder,
            caseSelectedBuilder,
            rabbitTemplate,
            customCaseRepository);

    // when
    ReflectionTestUtils.setField(actionRuleProcessor, "outboundExchange", OUTBOUND_EXCHANGE);
    ReflectionTestUtils.setField(
        actionRuleProcessor, "actionCaseExchange", ACTION_TO_CASE_EXCHANGE);
    actionRuleProcessor.createScheduledActions(actionRule);

    // then
    verify(fieldworkFollowupBuilder, times(expectedCaseCount))
        .buildFieldworkFollowup(any(Case.class), eq(actionRule));
    ArgumentCaptor<ActionRule> actionRuleCaptor = ArgumentCaptor.forClass(ActionRule.class);
    verify(actionRuleRepo, times(1)).save(actionRuleCaptor.capture());
    ActionRule actualActionRule = actionRuleCaptor.getAllValues().get(0);
    actionRule.setHasTriggered(true);
    Assertions.assertThat(actualActionRule).isEqualTo(actionRule);
    verify(rabbitTemplate, times(expectedCaseCount))
        .convertAndSend(
            eq(OUTBOUND_EXCHANGE), eq("Action.Field.binding"), any(FieldworkFollowup.class));
    verify(rabbitTemplate, times(expectedCaseCount))
        .convertAndSend(eq(ACTION_TO_CASE_EXCHANGE), eq(""), any(ResponseManagementEvent.class));
  }

  @Test
  public void testExceptionInThreadCausesException() {
    // Given
    ActionRule actionRule = setUpActionRule(ActionType.ICL1E);

    List<Case> cases = getRandomCases(50);
    // when
    when(customCaseRepository.streamAll(any(Specification.class))).thenReturn(cases.stream());

    when(caseSelectedBuilder.buildPrintMessage(any(PrintFileDto.class), any(UUID.class)))
        .thenReturn(new ResponseManagementEvent());

    doThrow(RuntimeException.class)
        .when(printFileDtoBuilder)
        .buildPrintFileDto(
            any(Case.class), any(String.class), any(UUID.class), eq(ActionType.ICL1E));

    // when
    ActionRuleProcessor actionRuleProcessor =
        new ActionRuleProcessor(
            actionRuleRepo,
            fieldworkFollowupBuilder,
            printFileDtoBuilder,
            caseSelectedBuilder,
            rabbitTemplate,
            customCaseRepository);
    ReflectionTestUtils.setField(actionRuleProcessor, "outboundExchange", OUTBOUND_EXCHANGE);
    RuntimeException actualException = null;
    try {
      actionRuleProcessor.createScheduledActions(actionRule);
    } catch (RuntimeException runtimeException) {
      actualException = runtimeException;
    }

    // then

    verify(rabbitTemplate, never())
        .convertAndSend(
            eq(OUTBOUND_EXCHANGE), eq("Action.Printer.binding"), any(PrintFileDto.class));

    assertNotNull(actualException);
    verify(actionRuleRepo, never()).save(any(ActionRule.class));
    verify(rabbitTemplate, never())
        .convertAndSend(
            eq(OUTBOUND_EXCHANGE), eq("Action.Printer.binding"), any(PrintFileDto.class));
  }

  @Test(expected = RuntimeException.class)
  public void testRabbitBlowsUpThrowsException() {
    // Given
    ActionRule actionRule = setUpActionRule(ActionType.ICL1E);

    List<Case> cases = getRandomCases(50);
    // when
    when(customCaseRepository.streamAll(any(Specification.class))).thenReturn(cases.stream());

    when(printFileDtoBuilder.buildPrintFileDto(
            any(Case.class), any(String.class), any(UUID.class), eq(ActionType.ICL1E)))
        .thenReturn(new PrintFileDto());

    when(caseSelectedBuilder.buildPrintMessage(any(PrintFileDto.class), any(UUID.class)))
        .thenReturn(new ResponseManagementEvent());

    doThrow(new RuntimeException())
        .when(rabbitTemplate)
        .convertAndSend(
            eq(OUTBOUND_EXCHANGE), eq("Action.Printer.binding"), any(PrintFileDto.class));

    // when
    ActionRuleProcessor actionRuleProcessor =
        new ActionRuleProcessor(
            actionRuleRepo,
            fieldworkFollowupBuilder,
            printFileDtoBuilder,
            caseSelectedBuilder,
            rabbitTemplate,
            customCaseRepository);
    ReflectionTestUtils.setField(actionRuleProcessor, "outboundExchange", OUTBOUND_EXCHANGE);
    actionRuleProcessor.createScheduledActions(actionRule);

    // then
    // exception thrown
  }

  private ActionRule setUpActionRule(ActionType actionType) {
    ActionRule actionRule = new ActionRule();
    UUID actionRuleId = UUID.randomUUID();
    actionRule.setId(actionRuleId);
    actionRule.setTriggerDateTime(OffsetDateTime.now());
    actionRule.setHasTriggered(false);

    Map<String, List<String>> classifiers = new HashMap<>();
    classifiers.put("A Key", new ArrayList<>());

    actionRule.setClassifiers(classifiers);
    actionRule.setActionType(actionType);

    ActionPlan actionPlan = new ActionPlan();
    actionPlan.setId(UUID.randomUUID());

    actionRule.setActionPlan(actionPlan);

    return actionRule;
  }

  private List<Case> getRandomCases(int count) {
    List<Case> cases = new ArrayList<>();

    EasyRandom easyRandom = new EasyRandom();

    for (int i = 0; i < count; i++) {
      cases.add(easyRandom.nextObject(Case.class));
    }

    return cases;
  }
}
