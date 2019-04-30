package uk.gov.ons.census.action.schedule;

import org.assertj.core.api.Assertions;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ActionRuleProcessorTest {
    private final ActionRuleRepository actionRuleRepo = mock(ActionRuleRepository.class);
    private final CaseRepository caseRepository = mock(CaseRepository.class);
    private final UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

    @Test
    public void testExecuteAllCases() {
        //Given
        ActionRuleProcessor actionRuleProcessor = new ActionRuleProcessor(actionRuleRepo, caseRepository,
                                                    uacQidLinkRepository, rabbitTemplate);

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

        EasyRandom easyRandom = new EasyRandom();
        Case[] cases = easyRandom.nextObject(Case[].class);
        when(caseRepository.findByActionPlanId(actionPlan.getId().toString())).thenReturn(Arrays.stream(cases));

        for( Case caze : cases ) {
            String uac = caze.getCaseId().toString() + "uac";
            UacQidLink uacQidLink = new UacQidLink();
            uacQidLink.setUac(uac);
            when( uacQidLinkRepository.findByCaseId(caze.getCaseId().toString())).thenReturn(Arrays.asList(uacQidLink));
        }

        doReturn(Arrays.asList(actionRule)).when(actionRuleRepo).findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(any());

        //when
        actionRuleProcessor.processActionRules();

        //then
        ArgumentCaptor<String>  outboundQueueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ActionInstruction> actionInstructionCaptor = ArgumentCaptor.forClass(ActionInstruction.class);

        verify(rabbitTemplate,
                times(cases.length))
                .convertAndSend(outboundQueueCaptor.capture(), routingKeyCaptor.capture(), actionInstructionCaptor.capture());
        List<ActionInstruction> actualActionInstructions = actionInstructionCaptor.getAllValues();
        List<ActionInstruction> expectedActionInstructions =
                getExpectedActionInstructions(cases, actionRule, actualActionInstructions);

        assertThat(actualActionInstructions).isEqualToComparingFieldByFieldRecursively(expectedActionInstructions);


        ArgumentCaptor<ActionRule> actionRuleCaptor = ArgumentCaptor.forClass(ActionRule.class);
        verify(actionRuleRepo, times(1)).save(actionRuleCaptor.capture());
        ActionRule actualActionRule = actionRuleCaptor.getAllValues().get(0);

        actionRule.setHasTriggered(true);

        Assertions.assertThat(actualActionRule).isEqualTo(actionRule);
    }

    private List<ActionInstruction> getExpectedActionInstructions(Case[] cases, ActionRule actionRule,
                                                                  List<ActionInstruction> actionInstructions) {
        List<ActionInstruction> expectedActionInstructions = new ArrayList<>();

        for( int i = 0; i < cases.length; i++ ) {
            expectedActionInstructions.add(getExpectedActionInstruction(cases[i],
                    cases[i].getCaseId().toString() + "uac", actionRule,
                    actionInstructions.get(i).getActionRequest().getActionId()));
        }

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