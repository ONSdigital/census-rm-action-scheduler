package uk.gov.ons.census.action.schedule;

import org.assertj.core.api.Assertions;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.census.action.model.dto.instruction.ActionAddress;
import uk.gov.ons.census.action.model.dto.instruction.ActionEvent;
import uk.gov.ons.census.action.model.dto.instruction.ActionInstruction;
import uk.gov.ons.census.action.model.dto.instruction.ActionRequest;
import uk.gov.ons.census.action.model.dto.instruction.Priority;
import uk.gov.ons.census.action.model.entity.ActionPlan;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

import javax.persistence.criteria.CriteriaBuilder;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.jpa.domain.Specification.where;

public class ActionRuleProcessorTest {
    public static final String AN_OUTBOUND_QUEUE_NAME = "AnOutboundQueueName";
    private final ActionRuleRepository actionRuleRepo = mock(ActionRuleRepository.class);
    private final CaseRepository caseRepository = mock(CaseRepository.class);
    private final UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    @Test
    public void testExecuteAllCases() {
        //Given
        ActionRule actionRule = setUpActionPlan();
        actionRule.setActionPlan(actionRule.getActionPlan());

        List<Case> cases = getRandomCases(50);
        when(caseRepository.findByActionPlanId(actionRule.getActionPlan().getId().toString())).thenReturn(cases.stream());

        setUpQidLinksForCases(cases);
        doReturn(Arrays.asList(actionRule)).when(actionRuleRepo).findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());

        //when
        ActionRuleProcessor actionRuleProcessor = new ActionRuleProcessor(actionRuleRepo, caseRepository,
                uacQidLinkRepository, rabbitTemplate);
        ReflectionTestUtils.setField(actionRuleProcessor, "outboundQueue", AN_OUTBOUND_QUEUE_NAME);
        actionRuleProcessor.processActionRules();

        //then
        List<ActionInstruction> actualActionInstructions = getActualActionInstructions(cases);
        List<ActionInstruction> expectedActionInstructions =
                getExpectedActionInstructionsWithActualActionIdUUIDs(cases, actionRule, actualActionInstructions);
        assertThat(actualActionInstructions).isEqualToComparingFieldByFieldRecursively(expectedActionInstructions);

        ArgumentCaptor<ActionRule> actionRuleCaptor = ArgumentCaptor.forClass(ActionRule.class);
        verify(actionRuleRepo, times(1)).save(actionRuleCaptor.capture());
        ActionRule actualActionRule = actionRuleCaptor.getAllValues().get(0);
        actionRule.setHasTriggered(true);
        Assertions.assertThat(actualActionRule).isEqualTo(actionRule);
    }

    @Test
    public void testExecuteClassifiers() {
        //Given
        ActionRule actionRule = setUpActionPlan();

        Map<String, List<String>> classifiers = new HashMap<>();
        List<String> columnValues = Arrays.asList("a", "b", "c");
        classifiers.put("A_Column", columnValues);
        actionRule.setClassifiers(classifiers);

        Specification<Case> expectedSpecification = getExpectedSpecification(actionRule);

        List<Case> cases = getRandomCases(47);
        setUpQidLinksForCases(cases);

        //Handrolled Fake as could not get Mockito to work with either explicit expectedSpecification of Example<Case> any().
        // The Fake tests the spec is as expected
        CaseRepository fakeCaseRepository = new FakeCaseRepository(cases, expectedSpecification);

        //For some reason this works and the 'normal' when.thenReturn way doesn't, might be the JPA OneToMany
        doReturn(Arrays.asList(actionRule)).when(actionRuleRepo).findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());

        //when
        ActionRuleProcessor actionRuleProcessor = new ActionRuleProcessor(actionRuleRepo, fakeCaseRepository,
                uacQidLinkRepository, rabbitTemplate);
        ReflectionTestUtils.setField(actionRuleProcessor, "outboundQueue", AN_OUTBOUND_QUEUE_NAME);
        actionRuleProcessor.processActionRules();

        //then
        List<ActionInstruction> actualActionInstructions = getActualActionInstructions(cases);
        List<ActionInstruction> expectedActionInstructions =
                getExpectedActionInstructionsWithActualActionIdUUIDs(cases, actionRule, actualActionInstructions);
        assertThat(actualActionInstructions).isEqualToComparingFieldByFieldRecursively(expectedActionInstructions);

        ArgumentCaptor<ActionRule> actionRuleCaptor = ArgumentCaptor.forClass(ActionRule.class);
        verify(actionRuleRepo, times(1)).save(actionRuleCaptor.capture());
        ActionRule actualActionRule = actionRuleCaptor.getAllValues().get(0);
        actionRule.setHasTriggered(true);
        Assertions.assertThat(actualActionRule).isEqualTo(actionRule);
    }

    @Test
    public void testQidLinksNull() {
        //Rewrite this test, check that at the end they're untriggered, then add Links and try again?
        // The RunTimeExceptions are swallowed

        //Given
        ActionRule actionRule = setUpActionPlan();

        Map<String, List<String>> classifiers = new HashMap<>();
        List<String> columnValues = Arrays.asList("a", "b", "c");
        classifiers.put("A_Column", columnValues);
        actionRule.setClassifiers(classifiers);

        Specification<Case> expectedSpecification = getExpectedSpecification(actionRule);

        List<Case> cases = getRandomCases(51);
        CaseRepository fakeCaseRepository = new FakeCaseRepository(cases, expectedSpecification);
        doReturn(Arrays.asList(actionRule)).when(actionRuleRepo).findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());

        //when
        ActionRuleProcessor actionRuleProcessor = new ActionRuleProcessor(actionRuleRepo, fakeCaseRepository,
                uacQidLinkRepository, rabbitTemplate);

        try {
            actionRuleProcessor.processActionRules();
        }
        catch( RuntimeException e) {
            assertTrue(true);
            return;
        }

        assertFalse("Expecting RunTimeException", true);

        //then
        //Exception Expected
    }

    private List<ActionInstruction> getActualActionInstructions(List<Case> cases) {
        ArgumentCaptor<String> outboundQueueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ActionInstruction> actionInstructionCaptor = ArgumentCaptor.forClass(ActionInstruction.class);
        
        verify(rabbitTemplate,
                times(cases.size()))
                .convertAndSend(outboundQueueCaptor.capture(), routingKeyCaptor.capture(), 
                        actionInstructionCaptor.capture());

        //To test this or not, suppose we should
        outboundQueueCaptor.getAllValues().forEach(actualQueueName ->
                assertThat(actualQueueName).isEqualTo(AN_OUTBOUND_QUEUE_NAME));

        routingKeyCaptor.getAllValues().forEach(actualRoutingKey -> {
            assertThat(actualRoutingKey).isEqualTo("");
        });

        return actionInstructionCaptor.getAllValues();
    }

    private ActionRule setUpActionPlan() {
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

    private void setUpQidLinksForCases(List<Case> cases) {
        cases.forEach(caze -> {
            String uac = caze.getCaseId().toString() + "uac";
            UacQidLink uacQidLink = new UacQidLink();
            uacQidLink.setUac(uac);

            when(uacQidLinkRepository.findByCaseId(caze.getCaseId().toString())).thenReturn(Arrays.asList(uacQidLink));
        });
    }

    private Specification<Case> getExpectedSpecification(ActionRule actionRule) {
        Specification<Case> specification = where(isActionPlanIdEqualTo(actionRule.getActionPlan().getId().toString()));

        for (Map.Entry<String, List<String>> classifier : actionRule.getClassifiers().entrySet()) {
            specification = specification.and(isClassifierIn(classifier.getKey(), classifier.getValue()));
        }

        return specification;
    }

    //refactor these for test?
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

    private List<ActionInstruction> getExpectedActionInstructionsWithActualActionIdUUIDs(List<Case> cases,
                                                                                         ActionRule actionRule,
                                                                                         List<ActionInstruction>
                                                                                                 actionInstructions) {
        Map<String, Case> caseMap = new HashMap<>();
        cases.forEach(caze -> {
            String caseId = caze.getCaseId().toString();
            caseMap.put(caze.getCaseId().toString(), caze);
        });

        List<ActionInstruction> expectedActionInstructions = new ArrayList<>();

        actionInstructions.forEach(actionInstruction -> {
            String caseId = actionInstruction.getActionRequest().getCaseId();
            String uac = caseId + "uac";
            expectedActionInstructions.add(getExpectedActionInstruction(caseMap.get(caseId), uac,
                    actionRule, actionInstruction.getActionRequest().getActionId()));
        });

        return expectedActionInstructions;
    }

    private ActionInstruction getExpectedActionInstruction(Case caze, String uac, ActionRule actionRule, String actionId) {

        ActionEvent actionEvent = new ActionEvent();
        actionEvent
                .getEvents()
                .add("CASE_CREATED : null : SYSTEM : Case created when Initial creation of case");
        ActionAddress actionAddress = new ActionAddress();
        actionAddress.setLine1(caze.getAddressLine1());
        actionAddress.setLine2(caze.getAddressLine2());
        actionAddress.setLine3(caze.getAddressLine3());
        actionAddress.setTownName(caze.getTownName());
        actionAddress.setPostcode(caze.getPostcode());
        actionAddress.setOrganisationName(caze.getOrganisationName());
        ActionRequest actionRequest = new ActionRequest();
        actionRequest.setActionId(actionId);
        actionRequest.setResponseRequired(false);
        actionRequest.setActionPlan(actionRule.getActionPlan().getId().toString());
        actionRequest.setActionType(actionRule.getActionType().toString());
        actionRequest.setAddress(actionAddress);
        actionRequest.setLegalBasis("Statistics of Trade Act 1947");
        actionRequest.setCaseGroupStatus("NOTSTARTED");
        actionRequest.setCaseId(caze.getCaseId().toString());
        actionRequest.setPriority(Priority.MEDIUM);
        actionRequest.setCaseRef(Long.toString(caze.getCaseRef()));
        actionRequest.setIac(uac);
        actionRequest.setEvents(actionEvent);
        actionRequest.setExerciseRef("201904");
        actionRequest.setUserDescription("Census-FNSM580JQE3M4");
        actionRequest.setSurveyName("Census-FNSM580JQE3M4");
        actionRequest.setSurveyRef("Census-FNSM580JQE3M4");
        actionRequest.setReturnByDate("27/04");
        actionRequest.setSampleUnitRef("DDR190314000000516472");
        ActionInstruction actionInstruction = new ActionInstruction();
        actionInstruction.setActionRequest(actionRequest);

        return actionInstruction;
    }

}