package uk.gov.ons.census.action.schedule;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

public class ActionRequestSenderTest {
  public static final String OUTBOUND_EXCHANGE = "OUTBOUND_EXCHANGE";
  private final UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
  private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

  @Test
  public void testHappyPath() {
    // Given
    ActionRequestSender underTest = new ActionRequestSender(uacQidLinkRepository, rabbitTemplate);
    ReflectionTestUtils.setField(underTest, "outboundExchange", OUTBOUND_EXCHANGE);
    EasyRandom easyRandom = new EasyRandom();
    Case caze = easyRandom.nextObject(Case.class);
    ActionPlan actionPlan = easyRandom.nextObject(ActionPlan.class);
    ActionRule actionRule = new ActionRule();
    actionRule.setActionPlan(actionPlan);
    actionRule.setActionType(ActionType.ICL1E);
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setUac(caze.getCaseId().toString() + "uac");
    when(uacQidLinkRepository.findByCaseId(eq(caze.getCaseId().toString())))
        .thenReturn(Collections.singletonList(uacQidLink));

    // When
    underTest.createAndSendActionRequest(caze, actionRule);

    // Then
    ActionInstruction actualActionInstruction = getActualActionInstruction();
    ActionInstruction expectedActionInstruction =
        getExpectedActionInstructionWithActualActionIdUUID(
            caze, actionRule, actualActionInstruction);
    assertThat(actualActionInstruction)
        .isEqualToComparingFieldByFieldRecursively(expectedActionInstruction);
  }

  private ActionInstruction getActualActionInstruction() {
    ArgumentCaptor<ActionInstruction> actionInstructionCaptor =
        ArgumentCaptor.forClass(ActionInstruction.class);

    verify(rabbitTemplate)
        .convertAndSend(
            eq(OUTBOUND_EXCHANGE), eq("Action.Printer.binding"), actionInstructionCaptor.capture());

    return actionInstructionCaptor.getValue();
  }

  private ActionInstruction getExpectedActionInstructionWithActualActionIdUUID(
      Case caze, ActionRule actionRule, ActionInstruction actionInstruction) {
    String caseId = actionInstruction.getActionRequest().getCaseId();
    String uac = caseId + "uac";
    return getExpectedActionInstruction(
        caze, uac, actionRule, actionInstruction.getActionRequest().getActionId());
  }

  private ActionInstruction getExpectedActionInstruction(
      Case caze, String uac, ActionRule actionRule, String actionId) {

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
