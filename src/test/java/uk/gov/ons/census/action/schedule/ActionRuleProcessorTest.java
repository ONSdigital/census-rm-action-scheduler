package uk.gov.ons.census.action.schedule;

import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.jpa.domain.Specification.where;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.criteria.CriteriaBuilder;
import org.assertj.core.api.Assertions;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.census.action.model.dto.instruction.ActionInstruction;
import uk.gov.ons.census.action.model.entity.ActionPlan;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;
import uk.gov.ons.census.action.model.repository.CaseRepository;

public class ActionRuleProcessorTest {
  public static final String OUTBOUND_EXCHANGE = "OUTBOUND_EXCHANGE";

  private final ActionRuleRepository actionRuleRepo = mock(ActionRuleRepository.class);
  private final CaseRepository caseRepository = mock(CaseRepository.class);
  private final ActionInstructionBuilder actionInstructionBuilder =
      mock(ActionInstructionBuilder.class);
  private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

  @Test
  public void testExecuteAllCases() {
    // Given
    ActionRule actionRule = setUpActionRule();

    List<Case> cases = getRandomCases(50);
    when(caseRepository.findByActionPlanId(actionRule.getActionPlan().getId().toString()))
        .thenReturn(cases.stream());

    doReturn(Arrays.asList(actionRule))
        .when(actionRuleRepo)
        .findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());

    when(actionInstructionBuilder.buildActionInstruction(any(Case.class), eq(actionRule)))
        .thenReturn(new ActionInstruction());

    // when
    ActionRuleProcessor actionRuleProcessor =
        new ActionRuleProcessor(
            actionRuleRepo, caseRepository, actionInstructionBuilder, rabbitTemplate);
    ReflectionTestUtils.setField(actionRuleProcessor, "outboundExchange", OUTBOUND_EXCHANGE);
    actionRuleProcessor.processActionRules();

    // then
    verify(actionInstructionBuilder, times(50))
        .buildActionInstruction(any(Case.class), eq(actionRule));
    ArgumentCaptor<ActionRule> actionRuleCaptor = ArgumentCaptor.forClass(ActionRule.class);
    verify(actionRuleRepo, times(1)).save(actionRuleCaptor.capture());
    ActionRule actualActionRule = actionRuleCaptor.getAllValues().get(0);
    actionRule.setHasTriggered(true);
    Assertions.assertThat(actualActionRule).isEqualTo(actionRule);
    verify(rabbitTemplate, times(50))
        .convertAndSend(
            eq(OUTBOUND_EXCHANGE), eq("Action.Printer.binding"), any(ActionInstruction.class));
  }

  @Test
  public void testExecuteClassifiers() {
    // Given
    ActionRule actionRule = setUpActionRule();
    Map<String, List<String>> classifiers = new HashMap<>();
    List<String> columnValues = Arrays.asList("a", "b", "c");
    classifiers.put("A_Column", columnValues);
    actionRule.setClassifiers(classifiers);

    Specification<Case> expectedSpecification = getExpectedSpecification(actionRule);

    List<Case> cases = getRandomCases(47);

    // Handrolled Fake as could not get Mockito to work with either explicit expectedSpecification
    // of Example<Case> any().
    // The Fake tests the spec is as expected
    CaseRepository fakeCaseRepository = new FakeCaseRepository(cases, expectedSpecification);

    // For some reason this works and the 'normal' when.thenReturn way doesn't, might be the JPA
    // OneToMany
    doReturn(Arrays.asList(actionRule))
        .when(actionRuleRepo)
        .findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());

    when(actionInstructionBuilder.buildActionInstruction(any(Case.class), eq(actionRule)))
        .thenReturn(new ActionInstruction());

    // when
    ActionRuleProcessor actionRuleProcessor =
        new ActionRuleProcessor(
            actionRuleRepo, fakeCaseRepository, actionInstructionBuilder, rabbitTemplate);
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
            eq(OUTBOUND_EXCHANGE), eq("Action.Printer.binding"), any(ActionInstruction.class));

  }

  @Test
  public void testExceptionInThreadCausesException() {
    // Given
    ActionRule actionRule = setUpActionRule();

    List<Case> cases = getRandomCases(50);
    when(caseRepository.findByActionPlanId(actionRule.getActionPlan().getId().toString()))
        .thenReturn(cases.stream());

    doReturn(Arrays.asList(actionRule))
        .when(actionRuleRepo)
        .findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());

    doThrow(RuntimeException.class)
        .when(actionInstructionBuilder)
        .buildActionInstruction(any(Case.class), eq(actionRule));

    // when
    ActionRuleProcessor actionRuleProcessor =
        new ActionRuleProcessor(
            actionRuleRepo, caseRepository, actionInstructionBuilder, rabbitTemplate);
    ReflectionTestUtils.setField(actionRuleProcessor, "outboundExchange", OUTBOUND_EXCHANGE);
    RuntimeException actualException = null;
    try {
      actionRuleProcessor.processActionRules();
    } catch (RuntimeException runtimeException) {
      actualException = runtimeException;
    }

    // then
    assertNotNull(actualException);
    verify(actionRuleRepo, never()).save(any(ActionRule.class));
    verify(rabbitTemplate, never())
        .convertAndSend(
            eq(OUTBOUND_EXCHANGE), eq("Action.Printer.binding"), any(ActionInstruction.class));
  }

  @Test(expected = RuntimeException.class)
  public void testRabbitBlowsUpThrowsException() {
    // Given
    ActionRule actionRule = setUpActionRule();

    List<Case> cases = getRandomCases(50);
    when(caseRepository.findByActionPlanId(actionRule.getActionPlan().getId().toString()))
        .thenReturn(cases.stream());

    doReturn(Arrays.asList(actionRule))
        .when(actionRuleRepo)
        .findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());

    when(actionInstructionBuilder.buildActionInstruction(any(Case.class), eq(actionRule)))
        .thenReturn(new ActionInstruction());

    doThrow(new RuntimeException()).when(rabbitTemplate).convertAndSend(
        eq(OUTBOUND_EXCHANGE), eq("Action.Printer.binding"), any(ActionInstruction.class));

    // when
    ActionRuleProcessor actionRuleProcessor =
        new ActionRuleProcessor(
            actionRuleRepo, caseRepository, actionInstructionBuilder, rabbitTemplate);
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
    actionRule.setClassifiers(new HashMap<>());
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

  private Specification<Case> getExpectedSpecification(ActionRule actionRule) {
    Specification<Case> specification =
        where(isActionPlanIdEqualTo(actionRule.getActionPlan().getId().toString()));

    for (Map.Entry<String, List<String>> classifier : actionRule.getClassifiers().entrySet()) {
      specification = specification.and(isClassifierIn(classifier.getKey(), classifier.getValue()));
    }

    return specification;
  }

  // refactor these for test?
  private Specification<Case> isActionPlanIdEqualTo(String actionPlanId) {
    return (Specification<Case>)
        (root, query, builder) -> builder.equal(root.get("actionPlanId"), actionPlanId);
  }

  private Specification<Case> isClassifierIn(
      final String fieldName, final List<String> inClauseValues) {
    return (Specification<Case>)
        (root, query, builder) -> {
          CriteriaBuilder.In<String> inClause = builder.in(root.get(fieldName));
          for (String inClauseValue : inClauseValues) {
            inClause.value(inClauseValue);
          }
          return inClause;
        };
  }
}
