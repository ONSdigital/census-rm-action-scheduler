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
import uk.gov.ons.census.action.model.dto.instruction.printer.ActionInstruction;
import uk.gov.ons.census.action.model.entity.ActionPlan;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;
import uk.gov.ons.census.action.model.repository.CaseRepository;

public class ActionRuleProcessorTest {
  private static final String OUTBOUND_EXCHANGE = "OUTBOUND_EXCHANGE";

  private final ActionRuleRepository actionRuleRepo = mock(ActionRuleRepository.class);
  private final CaseRepository caseRepository = mock(CaseRepository.class);
  private final ActionInstructionBuilder actionInstructionBuilder =
      mock(ActionInstructionBuilder.class);
  private final RabbitTemplate rabbitPrinterTemplate = mock(RabbitTemplate.class);
  private final RabbitTemplate rabbitFieldTemplate = mock(RabbitTemplate.class);

  @Test
  public void testExecuteAllCases() {
    // Given
    ActionRule actionRule = setUpActionRule();

    Specification<Case> expectedSpecification = getExpectedSpecification(actionRule);

    List<Case> cases = getRandomCases(50);

    // Handrolled Fake as could not get Mockito to work with either explicit expectedSpecification
    // of Example<Case> any().
    // The Fake tests the spec is as expected
    CaseRepository fakeCaseRepository = new FakeCaseRepository(cases, expectedSpecification);

    doReturn(Arrays.asList(actionRule))
        .when(actionRuleRepo)
        .findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());

    when(actionInstructionBuilder.buildPrinterActionInstruction(any(Case.class), eq(actionRule)))
        .thenReturn(new ActionInstruction());

    // when
    ActionRuleProcessor actionRuleProcessor =
        new ActionRuleProcessor(
            actionRuleRepo,
            fakeCaseRepository,
            actionInstructionBuilder,
            rabbitPrinterTemplate,
            null);
    ReflectionTestUtils.setField(actionRuleProcessor, "outboundExchange", OUTBOUND_EXCHANGE);
    actionRuleProcessor.processActionRules();

    // then
    verify(actionInstructionBuilder, times(50))
        .buildPrinterActionInstruction(any(Case.class), eq(actionRule));
    ArgumentCaptor<ActionRule> actionRuleCaptor = ArgumentCaptor.forClass(ActionRule.class);
    verify(actionRuleRepo, times(1)).save(actionRuleCaptor.capture());
    ActionRule actualActionRule = actionRuleCaptor.getAllValues().get(0);
    actionRule.setHasTriggered(true);
    Assertions.assertThat(actualActionRule).isEqualTo(actionRule);
    verify(rabbitPrinterTemplate, times(50))
        .convertAndSend(
            eq(OUTBOUND_EXCHANGE), eq("Action.Printer.binding"), any(ActionInstruction.class));
  }

  @Test
  public void testExecuteAllCasesField() {
    // Given
    ActionRule actionRule = setUpActionRuleField();

    Specification<Case> expectedSpecification = getExpectedSpecification(actionRule);

    List<Case> cases = getRandomCases(50);

    // Handrolled Fake as could not get Mockito to work with either explicit expectedSpecification
    // of Example<Case> any().
    // The Fake tests the spec is as expected
    CaseRepository fakeCaseRepository = new FakeCaseRepository(cases, expectedSpecification);

    doReturn(Arrays.asList(actionRule))
        .when(actionRuleRepo)
        .findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());

    when(actionInstructionBuilder.buildFieldActionInstruction(any(Case.class), eq(actionRule)))
        .thenReturn(new uk.gov.ons.census.action.model.dto.instruction.field.ActionInstruction());

    // when
    ActionRuleProcessor actionRuleProcessor =
        new ActionRuleProcessor(
            actionRuleRepo,
            fakeCaseRepository,
            actionInstructionBuilder,
            null,
            rabbitFieldTemplate);
    ReflectionTestUtils.setField(actionRuleProcessor, "outboundExchange", OUTBOUND_EXCHANGE);
    actionRuleProcessor.processActionRules();

    // then
    verify(actionInstructionBuilder, times(50))
        .buildFieldActionInstruction(any(Case.class), eq(actionRule));
    ArgumentCaptor<ActionRule> actionRuleCaptor = ArgumentCaptor.forClass(ActionRule.class);
    verify(actionRuleRepo, times(1)).save(actionRuleCaptor.capture());
    ActionRule actualActionRule = actionRuleCaptor.getAllValues().get(0);
    actionRule.setHasTriggered(true);
    Assertions.assertThat(actualActionRule).isEqualTo(actionRule);
    verify(rabbitFieldTemplate, times(50))
        .convertAndSend(
            eq(OUTBOUND_EXCHANGE),
            eq("Action.Field.binding"),
            any(uk.gov.ons.census.action.model.dto.instruction.field.ActionInstruction.class));
  }

  @Test
  public void testExecuteClassifiers() {
    // Given
    ActionRule actionRule = setUpActionRule();
    Map<String, List<String>> classifiers = new HashMap<>();
    List<String> columnValues = Arrays.asList("a", "b", "c");
    classifiers.put("A_Column", columnValues);
    actionRule.setClassifiers(classifiers);

    Specification<Case> expectedSpecification = getExpectedClassifiersSpecification(actionRule);

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

    when(actionInstructionBuilder.buildPrinterActionInstruction(any(Case.class), eq(actionRule)))
        .thenReturn(new ActionInstruction());

    // when
    ActionRuleProcessor actionRuleProcessor =
        new ActionRuleProcessor(
            actionRuleRepo,
            fakeCaseRepository,
            actionInstructionBuilder,
            rabbitPrinterTemplate,
            null);
    ReflectionTestUtils.setField(actionRuleProcessor, "outboundExchange", OUTBOUND_EXCHANGE);
    actionRuleProcessor.processActionRules();

    // then
    ArgumentCaptor<ActionRule> actionRuleCaptor = ArgumentCaptor.forClass(ActionRule.class);
    verify(actionRuleRepo, times(1)).save(actionRuleCaptor.capture());
    ActionRule actualActionRule = actionRuleCaptor.getAllValues().get(0);
    actionRule.setHasTriggered(true);
    Assertions.assertThat(actualActionRule).isEqualTo(actionRule);
    verify(rabbitPrinterTemplate, times(47))
        .convertAndSend(
            eq(OUTBOUND_EXCHANGE), eq("Action.Printer.binding"), any(ActionInstruction.class));
  }

  @Test
  public void testExceptionInThreadCausesException() {
    // Given
    ActionRule actionRule = setUpActionRule();

    List<Case> cases = getRandomCases(50);
    when(caseRepository.findAll(any(Specification.class))).thenReturn(cases);

    doReturn(Arrays.asList(actionRule))
        .when(actionRuleRepo)
        .findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());

    doThrow(RuntimeException.class)
        .when(actionInstructionBuilder)
        .buildPrinterActionInstruction(any(Case.class), eq(actionRule));

    // when
    ActionRuleProcessor actionRuleProcessor =
        new ActionRuleProcessor(
            actionRuleRepo, caseRepository, actionInstructionBuilder, rabbitPrinterTemplate, null);
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
    verify(rabbitPrinterTemplate, never())
        .convertAndSend(
            eq(OUTBOUND_EXCHANGE), eq("Action.Printer.binding"), any(ActionInstruction.class));
  }

  @Test(expected = RuntimeException.class)
  public void testRabbitBlowsUpThrowsException() {
    // Given
    ActionRule actionRule = setUpActionRule();

    List<Case> cases = getRandomCases(50);
    when(caseRepository.findAll(any(Specification.class))).thenReturn(cases);

    doReturn(Arrays.asList(actionRule))
        .when(actionRuleRepo)
        .findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());

    when(actionInstructionBuilder.buildPrinterActionInstruction(any(Case.class), eq(actionRule)))
        .thenReturn(new ActionInstruction());

    doThrow(new RuntimeException())
        .when(rabbitPrinterTemplate)
        .convertAndSend(
            eq(OUTBOUND_EXCHANGE), eq("Action.Printer.binding"), any(ActionInstruction.class));

    // when
    ActionRuleProcessor actionRuleProcessor =
        new ActionRuleProcessor(
            actionRuleRepo, caseRepository, actionInstructionBuilder, rabbitPrinterTemplate, null);
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

  private ActionRule setUpActionRuleField() {
    ActionRule actionRule = new ActionRule();
    UUID actionRuleId = UUID.randomUUID();
    actionRule.setId(actionRuleId);
    actionRule.setTriggerDateTime(OffsetDateTime.now());
    actionRule.setHasTriggered(false);
    actionRule.setClassifiers(new HashMap<>());
    actionRule.setActionType(ActionType.FF2QE);

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
    String actionPlanId = actionRule.getActionPlan().getId().toString();

    return createSpecificationForUnreceiptedCases(actionPlanId);
  }

  private Specification<Case> getExpectedClassifiersSpecification(ActionRule actionRule) {
    String actionPlanId = actionRule.getActionPlan().getId().toString();

    Specification<Case> specification = createSpecificationForUnreceiptedCases(actionPlanId);

    for (Map.Entry<String, List<String>> classifier : actionRule.getClassifiers().entrySet()) {
      specification = specification.and(isClassifierIn(classifier.getKey(), classifier.getValue()));
    }

    return specification;
  }

  private Specification<Case> createSpecificationForUnreceiptedCases(String actionPlanId) {
    return where(isActionPlanIdEqualTo(actionPlanId)).and(excludeReceiptedCases());
  }

  private Specification<Case> isActionPlanIdEqualTo(String actionPlanId) {
    return (Specification<Case>)
        (root, query, builder) -> builder.equal(root.get("actionPlanId"), actionPlanId);
  }

  private Specification<Case> excludeReceiptedCases() {
    return (Specification<Case>)
        (root, query, builder) -> builder.equal(root.get("receiptReceived"), false);
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
