//package uk.gov.ons.census.action.schedule;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//import static org.junit.Assert.assertEquals;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.doReturn;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//import static org.springframework.data.jpa.domain.Specification.where;
//
//import java.time.OffsetDateTime;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//import javax.persistence.criteria.CriteriaBuilder;
//import org.assertj.core.api.Assertions;
//import org.jeasy.random.EasyRandom;
//import org.junit.Test;
//import org.mockito.ArgumentCaptor;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.test.util.ReflectionTestUtils;
//import uk.gov.ons.census.action.model.dto.instruction.ActionAddress;
//import uk.gov.ons.census.action.model.dto.instruction.ActionEvent;
//import uk.gov.ons.census.action.model.dto.instruction.ActionInstruction;
//import uk.gov.ons.census.action.model.dto.instruction.ActionRequest;
//import uk.gov.ons.census.action.model.dto.instruction.Priority;
//import uk.gov.ons.census.action.model.entity.ActionPlan;
//import uk.gov.ons.census.action.model.entity.ActionRule;
//import uk.gov.ons.census.action.model.entity.ActionType;
//import uk.gov.ons.census.action.model.entity.Case;
//import uk.gov.ons.census.action.model.entity.UacQidLink;
//import uk.gov.ons.census.action.model.repository.ActionRuleRepository;
//import uk.gov.ons.census.action.model.repository.CaseRepository;
//import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;
//
//public class ActionRuleProcessorTest {
//  public static final String OUTBOUND_EXCHNAGE = "OUTBOUND_EXCHNAGE";
//  private final ActionRuleRepository actionRuleRepo = mock(ActionRuleRepository.class);
//  private final CaseRepository caseRepository = mock(CaseRepository.class);
//  private final UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
//  private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
//
//  @Test
//  public void testExecuteAllCases() {
//    // Given
//    ActionRule actionRule = setUpActionRule();
//
//    List<Case> cases = getRandomCases(50);
//    when(caseRepository.findByActionPlanId(actionRule.getActionPlan().getId().toString()))
//        .thenReturn(cases.stream());
//
//    setUpQidLinksForCases(cases);
//    doReturn(Arrays.asList(actionRule))
//        .when(actionRuleRepo)
//        .findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());
//
//    // when
//    ActionRuleProcessor actionRuleProcessor =
//        new ActionRuleProcessor(
//            actionRuleRepo, caseRepository, uacQidLinkRepository, rabbitTemplate);
//    ReflectionTestUtils.setField(actionRuleProcessor, "outboundExchange", OUTBOUND_EXCHNAGE);
//    actionRuleProcessor.processActionRules();
//
//    // then
//    List<ActionInstruction> actualActionInstructions = getActualActionInstructions(cases);
//    List<ActionInstruction> expectedActionInstructions =
//        getExpectedActionInstructionsWithActualActionIdUUIDs(
//            cases, actionRule, actualActionInstructions);
//    assertThat(actualActionInstructions)
//        .isEqualToComparingFieldByFieldRecursively(expectedActionInstructions);
//
//    ArgumentCaptor<ActionRule> actionRuleCaptor = ArgumentCaptor.forClass(ActionRule.class);
//    verify(actionRuleRepo, times(1)).save(actionRuleCaptor.capture());
//    ActionRule actualActionRule = actionRuleCaptor.getAllValues().get(0);
//    actionRule.setHasTriggered(true);
//    Assertions.assertThat(actualActionRule).isEqualTo(actionRule);
//  }
//
//  @Test
//  public void testWalesQuestionnaireWithTwoQidUacs() {
//    // Given
//    ActionRule actionRule = setUpActionRule();
//
//    EasyRandom easyRandom = new EasyRandom();
//    Case testCase = easyRandom.nextObject(Case.class);
//    testCase.setTreatmentCode("HH_QF2R1W");
//    String uacEng = easyRandom.nextObject(String.class);
//    String uacWal = easyRandom.nextObject(String.class);
//    String qidEng = "0220000010732199";
//    String qidWal = "0320000002861455";
//
//    List<Case> cases = Collections.singletonList(testCase);
//    when(caseRepository.findByActionPlanId(actionRule.getActionPlan().getId().toString()))
//        .thenReturn(cases.stream());
//
//    List<UacQidLink> uacQidLinks = new LinkedList<>();
//
//    UacQidLink uacQidLink = new UacQidLink();
//    uacQidLink.setUac(uacEng);
//    uacQidLink.setQid(qidEng);
//    uacQidLinks.add(uacQidLink);
//
//    uacQidLink = new UacQidLink();
//    uacQidLink.setUac(uacWal);
//    uacQidLink.setQid(qidWal);
//    uacQidLinks.add(uacQidLink);
//
//    when(uacQidLinkRepository.findByCaseId(testCase.getCaseId().toString()))
//        .thenReturn(uacQidLinks);
//
//    doReturn(Arrays.asList(actionRule))
//        .when(actionRuleRepo)
//        .findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());
//
//    // when
//    ActionRuleProcessor actionRuleProcessor =
//        new ActionRuleProcessor(
//            actionRuleRepo, caseRepository, uacQidLinkRepository, rabbitTemplate);
//    ReflectionTestUtils.setField(actionRuleProcessor, "outboundExchange", OUTBOUND_EXCHNAGE);
//    actionRuleProcessor.processActionRules();
//
//    // then
//    List<ActionInstruction> actualActionInstructions = getActualActionInstructions(cases);
//    assertEquals(1, actualActionInstructions.size());
//    ActionInstruction actualActionInstruction = actualActionInstructions.get(0);
//    ActionInstruction expectedActionInstruction =
//        getExpectedActionInstruction(
//            testCase,
//            uacEng,
//            uacWal,
//            qidEng,
//            qidWal,
//            actionRule,
//            actualActionInstruction.getActionRequest().getActionId());
//
//    assertThat(actualActionInstruction)
//        .isEqualToComparingFieldByFieldRecursively(expectedActionInstruction);
//
//    ArgumentCaptor<ActionRule> actionRuleCaptor = ArgumentCaptor.forClass(ActionRule.class);
//    verify(actionRuleRepo, times(1)).save(actionRuleCaptor.capture());
//    ActionRule actualActionRule = actionRuleCaptor.getAllValues().get(0);
//    actionRule.setHasTriggered(true);
//    Assertions.assertThat(actualActionRule).isEqualTo(actionRule);
//  }
//
//  @Test(expected = RuntimeException.class)
//  public void testWalesQuestionnaireWithMissingQidUac() {
//    // Given
//    ActionRule actionRule = setUpActionRule();
//
//    EasyRandom easyRandom = new EasyRandom();
//    Case testCase = easyRandom.nextObject(Case.class);
//    testCase.setTreatmentCode("HH_QF2R1W");
//    String uacEng = easyRandom.nextObject(String.class);
//    String uacWal = easyRandom.nextObject(String.class);
//    String qidEng = "0220000010732199";
//    String qidWal = "0320000002861455";
//
//    List<Case> cases = Collections.singletonList(testCase);
//    when(caseRepository.findByActionPlanId(actionRule.getActionPlan().getId().toString()))
//        .thenReturn(cases.stream());
//
//    List<UacQidLink> uacQidLinks = new LinkedList<>();
//
//    UacQidLink uacQidLink = new UacQidLink();
//    uacQidLink.setUac(uacEng);
//    uacQidLink.setQid(qidEng);
//    uacQidLinks.add(uacQidLink);
//
//    when(uacQidLinkRepository.findByCaseId(testCase.getCaseId().toString()))
//        .thenReturn(uacQidLinks);
//
//    doReturn(Arrays.asList(actionRule))
//        .when(actionRuleRepo)
//        .findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());
//
//    // when
//    ActionRuleProcessor actionRuleProcessor =
//        new ActionRuleProcessor(
//            actionRuleRepo, caseRepository, uacQidLinkRepository, rabbitTemplate);
//    ReflectionTestUtils.setField(actionRuleProcessor, "outboundExchange", OUTBOUND_EXCHNAGE);
//    actionRuleProcessor.processActionRules();
//
//    // then
//    List<ActionInstruction> actualActionInstructions = getActualActionInstructions(cases);
//    assertEquals(1, actualActionInstructions.size());
//    ActionInstruction actualActionInstruction = actualActionInstructions.get(0);
//    ActionInstruction expectedActionInstruction =
//        getExpectedActionInstruction(
//            testCase,
//            uacEng,
//            uacWal,
//            qidEng,
//            qidWal,
//            actionRule,
//            actualActionInstruction.getActionRequest().getActionId());
//
//    assertThat(actualActionInstruction)
//        .isEqualToComparingFieldByFieldRecursively(expectedActionInstruction);
//
//    ArgumentCaptor<ActionRule> actionRuleCaptor = ArgumentCaptor.forClass(ActionRule.class);
//    verify(actionRuleRepo, times(1)).save(actionRuleCaptor.capture());
//    ActionRule actualActionRule = actionRuleCaptor.getAllValues().get(0);
//    actionRule.setHasTriggered(true);
//    Assertions.assertThat(actualActionRule).isEqualTo(actionRule);
//  }
//
//  @Test
//  public void testExecuteClassifiers() {
//    // Given
//    ActionRule actionRule = setUpActionRule();
//    Map<String, List<String>> classifiers = new HashMap<>();
//    List<String> columnValues = Arrays.asList("a", "b", "c");
//    classifiers.put("A_Column", columnValues);
//    actionRule.setClassifiers(classifiers);
//
//    Specification<Case> expectedSpecification = getExpectedSpecification(actionRule);
//
//    List<Case> cases = getRandomCases(47);
//    setUpQidLinksForCases(cases);
//
//    // Handrolled Fake as could not get Mockito to work with either explicit expectedSpecification
//    // of Example<Case> any().
//    // The Fake tests the spec is as expected
//    CaseRepository fakeCaseRepository = new FakeCaseRepository(cases, expectedSpecification);
//
//    // For some reason this works and the 'normal' when.thenReturn way doesn't, might be the JPA
//    // OneToMany
//    doReturn(Arrays.asList(actionRule))
//        .when(actionRuleRepo)
//        .findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());
//
//    // when
//    ActionRuleProcessor actionRuleProcessor =
//        new ActionRuleProcessor(
//            actionRuleRepo, fakeCaseRepository, uacQidLinkRepository, rabbitTemplate);
//    ReflectionTestUtils.setField(actionRuleProcessor, "outboundExchange", OUTBOUND_EXCHNAGE);
//    actionRuleProcessor.processActionRules();
//
//    // then
//    List<ActionInstruction> actualActionInstructions = getActualActionInstructions(cases);
//    List<ActionInstruction> expectedActionInstructions =
//        getExpectedActionInstructionsWithActualActionIdUUIDs(
//            cases, actionRule, actualActionInstructions);
//    assertThat(actualActionInstructions)
//        .isEqualToComparingFieldByFieldRecursively(expectedActionInstructions);
//
//    ArgumentCaptor<ActionRule> actionRuleCaptor = ArgumentCaptor.forClass(ActionRule.class);
//    verify(actionRuleRepo, times(1)).save(actionRuleCaptor.capture());
//    ActionRule actualActionRule = actionRuleCaptor.getAllValues().get(0);
//    actionRule.setHasTriggered(true);
//    Assertions.assertThat(actualActionRule).isEqualTo(actionRule);
//  }
//
//  @Test(expected = RuntimeException.class)
//  public void testQidLinksNullDoesNotSendAnything() {
//    // Given
//    ActionRule actionRule = setUpActionRule();
//
//    Map<String, List<String>> classifiers = new HashMap<>();
//    List<String> columnValues = Arrays.asList("a", "b", "c");
//    classifiers.put("A_Column", columnValues);
//    actionRule.setClassifiers(classifiers);
//
//    Specification<Case> expectedSpecification = getExpectedSpecification(actionRule);
//
//    List<Case> cases = getRandomCases(51);
//    CaseRepository fakeCaseRepository = new FakeCaseRepository(cases, expectedSpecification);
//    doReturn(Arrays.asList(actionRule))
//        .when(actionRuleRepo)
//        .findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());
//
//    // when
//    ActionRuleProcessor actionRuleProcessor =
//        new ActionRuleProcessor(
//            actionRuleRepo, fakeCaseRepository, uacQidLinkRepository, rabbitTemplate);
//    actionRuleProcessor.processActionRules();
//
//    // then
//    ArgumentCaptor<String> outboundQueueCaptor = ArgumentCaptor.forClass(String.class);
//    ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
//    ArgumentCaptor<ActionInstruction> actionInstructionCaptor =
//        ArgumentCaptor.forClass(ActionInstruction.class);
//
//    // There should be no messages sent
//    verify(rabbitTemplate, times(0))
//        .convertAndSend(
//            outboundQueueCaptor.capture(),
//            routingKeyCaptor.capture(),
//            actionInstructionCaptor.capture());
//  }
//
//  @Test(expected = RuntimeException.class)
//  public void testMulitpleQidLinkslDoesNotSendAnything() {
//    // Given
//    ActionRule actionRule = setUpActionRule();
//
//    Map<String, List<String>> classifiers = new HashMap<>();
//    List<String> columnValues = Arrays.asList("a", "b", "c");
//    classifiers.put("A_Column", columnValues);
//    actionRule.setClassifiers(classifiers);
//
//    Specification<Case> expectedSpecification = getExpectedSpecification(actionRule);
//
//    List<Case> cases = getRandomCases(51);
//    CaseRepository fakeCaseRepository = new FakeCaseRepository(cases, expectedSpecification);
//    doReturn(Arrays.asList(actionRule))
//        .when(actionRuleRepo)
//        .findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());
//
//    setUpMulitpleQidLinksForCases(cases);
//
//    // when
//    ActionRuleProcessor actionRuleProcessor =
//        new ActionRuleProcessor(
//            actionRuleRepo, fakeCaseRepository, uacQidLinkRepository, rabbitTemplate);
//    actionRuleProcessor.processActionRules();
//
//    // then
//    ArgumentCaptor<String> outboundQueueCaptor = ArgumentCaptor.forClass(String.class);
//    ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
//    ArgumentCaptor<ActionInstruction> actionInstructionCaptor =
//        ArgumentCaptor.forClass(ActionInstruction.class);
//
//    // There should be no messages sent
//    verify(rabbitTemplate, times(0))
//        .convertAndSend(
//            outboundQueueCaptor.capture(),
//            routingKeyCaptor.capture(),
//            actionInstructionCaptor.capture());
//  }
//
//  private List<ActionInstruction> getActualActionInstructions(List<Case> cases) {
//    ArgumentCaptor<String> outboundQueueCaptor = ArgumentCaptor.forClass(String.class);
//    ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
//    ArgumentCaptor<ActionInstruction> actionInstructionCaptor =
//        ArgumentCaptor.forClass(ActionInstruction.class);
//
//    verify(rabbitTemplate, times(cases.size()))
//        .convertAndSend(
//            outboundQueueCaptor.capture(),
//            routingKeyCaptor.capture(),
//            actionInstructionCaptor.capture());
//
//    // To test this or not, suppose we should
//    outboundQueueCaptor
//        .getAllValues()
//        .forEach(actualExchangeName -> assertThat(actualExchangeName).isEqualTo(OUTBOUND_EXCHNAGE));
//
//    routingKeyCaptor
//        .getAllValues()
//        .forEach(
//            actualRoutingKey -> {
//              assertThat(actualRoutingKey).isEqualTo("Action.Printer.binding");
//            });
//
//    return actionInstructionCaptor.getAllValues();
//  }
//
//  private ActionRule setUpActionRule() {
//    ActionRule actionRule = new ActionRule();
//    UUID actionRuleId = UUID.randomUUID();
//    actionRule.setId(actionRuleId);
//    actionRule.setTriggerDateTime(OffsetDateTime.now());
//    actionRule.setHasTriggered(false);
//    actionRule.setClassifiers(new HashMap<>());
//    actionRule.setActionType(ActionType.ICL1E);
//
//    ActionPlan actionPlan = new ActionPlan();
//    actionPlan.setId(UUID.randomUUID());
//
//    actionRule.setActionPlan(actionPlan);
//
//    return actionRule;
//  }
//
//  private List<Case> getRandomCases(int count) {
//    List<Case> cases = new ArrayList<>();
//
//    EasyRandom easyRandom = new EasyRandom();
//
//    for (int i = 0; i < count; i++) {
//      cases.add(easyRandom.nextObject(Case.class));
//    }
//
//    return cases;
//  }
//
//  private void setUpMulitpleQidLinksForCases(List<Case> cases) {
//    cases.forEach(
//        caze -> {
//          String uac = caze.getCaseId().toString() + "uac";
//
//          List<UacQidLink> uacQuidLinks = new ArrayList<>();
//          UacQidLink uacQidLink = new UacQidLink();
//          uacQidLink.setUac(uac);
//
//          uacQuidLinks.add(uacQidLink);
//          uacQuidLinks.add(uacQidLink);
//
//          when(uacQidLinkRepository.findByCaseId(caze.getCaseId().toString()))
//              .thenReturn(uacQuidLinks);
//        });
//  }
//
//  private void setUpQidLinksForCases(List<Case> cases) {
//    cases.forEach(
//        caze -> {
//          String uac = caze.getCaseId().toString() + "uac";
//          UacQidLink uacQidLink = new UacQidLink();
//          uacQidLink.setUac(uac);
//
//          when(uacQidLinkRepository.findByCaseId(caze.getCaseId().toString()))
//              .thenReturn(Arrays.asList(uacQidLink));
//        });
//  }
//
//  private Specification<Case> getExpectedSpecification(ActionRule actionRule) {
//    Specification<Case> specification =
//        where(isActionPlanIdEqualTo(actionRule.getActionPlan().getId().toString()));
//
//    for (Map.Entry<String, List<String>> classifier : actionRule.getClassifiers().entrySet()) {
//      specification = specification.and(isClassifierIn(classifier.getKey(), classifier.getValue()));
//    }
//
//    return specification;
//  }
//
//  // refactor these for test?
//  private Specification<Case> isActionPlanIdEqualTo(String actionPlanId) {
//    return (Specification<Case>)
//        (root, query, builder) -> builder.equal(root.get("actionPlanId"), actionPlanId);
//  }
//
//  private Specification<Case> isClassifierIn(
//      final String fieldName, final List<String> inClauseValues) {
//    return (Specification<Case>)
//        (root, query, builder) -> {
//          CriteriaBuilder.In<String> inClause = builder.in(root.get(fieldName));
//          for (String inClauseValue : inClauseValues) {
//            inClause.value(inClauseValue);
//          }
//          return inClause;
//        };
//  }
//
//  private List<ActionInstruction> getExpectedActionInstructionsWithActualActionIdUUIDs(
//      List<Case> cases, ActionRule actionRule, List<ActionInstruction> actionInstructions) {
//    Map<String, Case> caseMap = new HashMap<>();
//    cases.forEach(
//        caze -> {
//          String caseId = caze.getCaseId().toString();
//          caseMap.put(caze.getCaseId().toString(), caze);
//        });
//
//    List<ActionInstruction> expectedActionInstructions = new ArrayList<>();
//
//    actionInstructions.forEach(
//        actionInstruction -> {
//          String caseId = actionInstruction.getActionRequest().getCaseId();
//          String uac = caseId + "uac";
//          expectedActionInstructions.add(
//              getExpectedActionInstruction(
//                  caseMap.get(caseId),
//                  uac,
//                  actionRule,
//                  actionInstruction.getActionRequest().getActionId()));
//        });
//
//    return expectedActionInstructions;
//  }
//
//  private ActionInstruction getExpectedActionInstruction(
//      Case caze, String uac, ActionRule actionRule, String actionId) {
//
//    ActionEvent actionEvent = new ActionEvent();
//    actionEvent
//        .getEvents()
//        .add("CASE_CREATED : null : SYSTEM : Case created when Initial creation of case");
//    ActionAddress actionAddress = new ActionAddress();
//    actionAddress.setLine1(caze.getAddressLine1());
//    actionAddress.setLine2(caze.getAddressLine2());
//    actionAddress.setLine3(caze.getAddressLine3());
//    actionAddress.setTownName(caze.getTownName());
//    actionAddress.setPostcode(caze.getPostcode());
//    actionAddress.setOrganisationName(caze.getOrganisationName());
//    ActionRequest actionRequest = new ActionRequest();
//    actionRequest.setActionId(actionId);
//    actionRequest.setResponseRequired(false);
//    actionRequest.setActionPlan(actionRule.getActionPlan().getId().toString());
//    actionRequest.setActionType(actionRule.getActionType().toString());
//    actionRequest.setAddress(actionAddress);
//    actionRequest.setLegalBasis("Statistics of Trade Act 1947");
//    actionRequest.setCaseGroupStatus("NOTSTARTED");
//    actionRequest.setCaseId(caze.getCaseId().toString());
//    actionRequest.setPriority(Priority.MEDIUM);
//    actionRequest.setCaseRef(Long.toString(caze.getCaseRef()));
//    actionRequest.setIac(uac);
//    actionRequest.setEvents(actionEvent);
//    actionRequest.setExerciseRef("201904");
//    actionRequest.setUserDescription("Census-FNSM580JQE3M4");
//    actionRequest.setSurveyName("Census-FNSM580JQE3M4");
//    actionRequest.setSurveyRef("Census-FNSM580JQE3M4");
//    actionRequest.setReturnByDate("27/04");
//    actionRequest.setSampleUnitRef("DDR190314000000516472");
//    ActionInstruction actionInstruction = new ActionInstruction();
//    actionInstruction.setActionRequest(actionRequest);
//
//    return actionInstruction;
//  }
//
//  private ActionInstruction getExpectedActionInstruction(
//      Case caze,
//      String uac,
//      String uacWales,
//      String qid,
//      String qidWales,
//      ActionRule actionRule,
//      String actionId) {
//
//    ActionInstruction actionInstruction =
//        getExpectedActionInstruction(caze, uac, actionRule, actionId);
//    actionInstruction.getActionRequest().setIacWales(uacWales);
//    actionInstruction.getActionRequest().setQid(qid);
//    actionInstruction.getActionRequest().setQidWales(qidWales);
//
//    return actionInstruction;
//  }
//}
