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
import uk.gov.ons.census.action.builders.ActionInstructionBuilder;
import uk.gov.ons.census.action.builders.PrintFileDtoBuilder;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.entity.ActionPlan;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.CustomCaseRepository;

public class ActionRuleProcessorTest {
  private static final String OUTBOUND_EXCHANGE = "OUTBOUND_EXCHANGE";

  private final ActionRuleRepository actionRuleRepo = mock(ActionRuleRepository.class);
  private final CaseRepository caseRepository = mock(CaseRepository.class);
  private final CustomCaseRepository customCaseRepository = mock(CustomCaseRepository.class);
  private final ActionInstructionBuilder actionInstructionBuilder =
      mock(ActionInstructionBuilder.class);
  private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
  private final RabbitTemplate rabbitFieldTemplate = mock(RabbitTemplate.class);
  private final PrintFileDtoBuilder printFileDtoBuilder = mock(PrintFileDtoBuilder.class);

  @Test
  public void testExecuteClassifiers() {
    // Given
    ActionRule actionRule = setUpActionRule();
    Map<String, List<String>> classifiers = new HashMap<>();
    List<String> columnValues = Arrays.asList("a", "b", "c");
    classifiers.put("A_Column", columnValues);
    actionRule.setClassifiers(classifiers);

    List<Case> cases = getRandomCases(47);

    // For some reason this works and the 'normal' when.thenReturn way doesn't, might be the JPA
    // OneToMany
    doReturn(Arrays.asList(actionRule))
        .when(actionRuleRepo)
        .findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());

    when(printFileDtoBuilder.buildPrintFileDto(any(Case.class), any(String.class), any(UUID.class)))
        .thenReturn(new PrintFileDto());

    when(customCaseRepository.streamAll(any(Specification.class))).thenReturn(cases.stream());

    // when
    ActionRuleProcessor actionRuleProcessor =
        new ActionRuleProcessor(
            actionRuleRepo,
            actionInstructionBuilder,
            printFileDtoBuilder,
            rabbitTemplate,
            customCaseRepository,
            null);
    ReflectionTestUtils.setField(actionRuleProcessor, "outboundExchange", OUTBOUND_EXCHANGE);
    actionRuleProcessor.processActionRules();

    // then
    ArgumentCaptor<ActionRule> actionRuleCaptor = ArgumentCaptor.forClass(ActionRule.class);
    verify(actionRuleRepo, times(1)).save(actionRuleCaptor.capture());
    ActionRule actualActionRule = actionRuleCaptor.getAllValues().get(0);
    actionRule.setHasTriggered(true);
    Assertions.assertThat(actualActionRule).isEqualTo(actionRule);
    verify(rabbitTemplate, times(47))
        .convertAndSend(
            eq(OUTBOUND_EXCHANGE), eq("Action.Printer.binding"), any(PrintFileDto.class));
  }

  @Test
  public void testExceptionInThreadCausesException() {
    // Given
    ActionRule actionRule = setUpActionRule();

    List<Case> cases = getRandomCases(50);
    // when
    when(customCaseRepository.streamAll(any(Specification.class))).thenReturn(cases.stream());

    doReturn(Arrays.asList(actionRule))
        .when(actionRuleRepo)
        .findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());

    doThrow(RuntimeException.class)
        .when(printFileDtoBuilder)
        .buildPrintFileDto(any(Case.class), any(String.class), any(UUID.class));

    // when
    ActionRuleProcessor actionRuleProcessor =
        new ActionRuleProcessor(
            actionRuleRepo,
            actionInstructionBuilder,
            printFileDtoBuilder,
            rabbitTemplate,
            customCaseRepository,
            null);
    ReflectionTestUtils.setField(actionRuleProcessor, "outboundExchange", OUTBOUND_EXCHANGE);
    RuntimeException actualException = null;
    try {
      actionRuleProcessor.processActionRules();
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
    ActionRule actionRule = setUpActionRule();

    List<Case> cases = getRandomCases(50);
    // when
    when(customCaseRepository.streamAll(any(Specification.class))).thenReturn(cases.stream());

    doReturn(Arrays.asList(actionRule))
        .when(actionRuleRepo)
        .findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());

    when(printFileDtoBuilder.buildPrintFileDto(any(Case.class), any(String.class), any(UUID.class)))
        .thenReturn(new PrintFileDto());

    doThrow(new RuntimeException())
        .when(rabbitTemplate)
        .convertAndSend(
            eq(OUTBOUND_EXCHANGE), eq("Action.Printer.binding"), any(PrintFileDto.class));

    // when
    ActionRuleProcessor actionRuleProcessor =
        new ActionRuleProcessor(
            actionRuleRepo,
            actionInstructionBuilder,
            printFileDtoBuilder,
            rabbitTemplate,
            customCaseRepository,
            null);
    ReflectionTestUtils.setField(actionRuleProcessor, "outboundExchange", OUTBOUND_EXCHANGE);
    actionRuleProcessor.processActionRules();

    // then
    // exception thrown
  }

  private ActionRule setUpActionRule() {
    ActionRule actionRule = new ActionRule();
    UUID actionRuleId = UUID.randomUUID();
    actionRule.setId(actionRuleId);
    actionRule.setTriggerDateTime(OffsetDateTime.now());
    actionRule.setHasTriggered(false);

    Map<String, List<String>> classifiers = new HashMap<>();
    classifiers.put("A Key", new ArrayList<>());

    actionRule.setClassifiers(classifiers);
    actionRule.setActionType(ActionType.ICL1E);

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
