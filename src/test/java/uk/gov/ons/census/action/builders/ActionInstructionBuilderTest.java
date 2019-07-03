package uk.gov.ons.census.action.builders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jeasy.random.EasyRandom;
import org.junit.Test;
import uk.gov.ons.census.action.model.UacQidTuple;
import uk.gov.ons.census.action.model.dto.instruction.field.ActionAddress;
import uk.gov.ons.census.action.model.dto.instruction.field.ActionEvent;
import uk.gov.ons.census.action.model.dto.instruction.field.ActionInstruction;
import uk.gov.ons.census.action.model.dto.instruction.field.ActionRequest;
import uk.gov.ons.census.action.model.dto.instruction.field.Priority;
import uk.gov.ons.census.action.model.entity.ActionPlan;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.UacQidLink;

public class ActionInstructionBuilderTest {
  private final QidUacBuilder qidUacBuilder = mock(QidUacBuilder.class);

  @Test(expected = RuntimeException.class)
  public void testQidLinksNull() {
    // Given
    EasyRandom easyRandom = new EasyRandom();
    ActionPlan actionPlan = easyRandom.nextObject(ActionPlan.class);
    ActionRule actionRule = new ActionRule();
    actionRule.setActionPlan(actionPlan);
    actionRule.setActionType(ActionType.ICL1E);
    Case testCase = easyRandom.nextObject(Case.class);
    testCase.setTreatmentCode("HH_QF2R1W");

    when(qidUacBuilder.getUacQidLinks(testCase)).thenReturn(null);

    // When
    ActionInstructionBuilder underTest = new ActionInstructionBuilder(qidUacBuilder);
    underTest.buildFieldActionInstruction(testCase, actionRule);

    // Then
    // Expect exception to be thrown
  }

  @Test
  public void testLatAndLongCopiedToAddress() {
    // Given
    EasyRandom easyRandom = new EasyRandom();
    Case caze = easyRandom.nextObject(Case.class);
    caze.setLatitude("1.123456789999");
    caze.setLongitude("-9.987654321111");
    caze.setCeExpectedCapacity("500");

    ActionPlan actionPlan = easyRandom.nextObject(ActionPlan.class);
    ActionRule actionRule = new ActionRule();
    actionRule.setActionPlan(actionPlan);
    actionRule.setActionType(ActionType.ICL1E);
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setUac(caze.getCaseId().toString() + "uac");

    UacQidTuple uacQidTuple = new UacQidTuple();
    uacQidTuple.setUacQidLink(uacQidLink);

    when(qidUacBuilder.getUacQidLinks(caze)).thenReturn(uacQidTuple);

    // When
    ActionInstructionBuilder underTest = new ActionInstructionBuilder(qidUacBuilder);
    uk.gov.ons.census.action.model.dto.instruction.field.ActionInstruction actualResult =
        underTest.buildFieldActionInstruction(caze, actionRule);

    // Then
    assertThat(caze.getLatitude())
        .isEqualTo(actualResult.getActionRequest().getAddress().getLatitude().toString());
    assertThat(caze.getLongitude())
        .isEqualTo(actualResult.getActionRequest().getAddress().getLongitude().toString());
  }

  private ActionInstruction getExpectedActionInstructionWithActualActionIdUUID(
      Case caze, ActionRule actionRule, ActionInstruction actionInstruction) {
    String caseId = actionInstruction.getActionRequest().getCaseId();
    String uac = caseId + "uac";
    return getExpectedActionInstruction(
        caze,
        uac,
        null,
        null,
        null,
        actionRule,
        actionInstruction.getActionRequest().getActionId());
  }

  private ActionInstruction getExpectedActionInstruction(
      Case caze,
      String uac,
      String uacWales,
      String qid,
      String qidWales,
      ActionRule actionRule,
      String actionId) {

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
