package uk.gov.ons.census.action.schedule;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import uk.gov.ons.census.action.model.dto.instruction.printer.ActionAddress;
import uk.gov.ons.census.action.model.dto.instruction.printer.ActionEvent;
import uk.gov.ons.census.action.model.dto.instruction.printer.ActionInstruction;
import uk.gov.ons.census.action.model.dto.instruction.printer.ActionRequest;
import uk.gov.ons.census.action.model.dto.instruction.printer.Priority;
import uk.gov.ons.census.action.model.entity.ActionPlan;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

public class ActionInstructionBuilderTest {
  private final UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);

  @Test
  public void testHappyPath() {
    // Given
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
    ActionInstructionBuilder underTest = new ActionInstructionBuilder(uacQidLinkRepository);
    ActionInstruction actualActionInstruction =
        underTest.buildPrinterActionInstruction(caze, actionRule);

    // Then
    ActionInstruction expectedActionInstruction =
        getExpectedActionInstructionWithActualActionIdUUID(
            caze, actionRule, actualActionInstruction);
    assertThat(actualActionInstruction)
        .isEqualToComparingFieldByFieldRecursively(expectedActionInstruction);
  }

  @Test
  public void testWalesQuestionnaireWithTwoQidUacs() {
    // Given
    EasyRandom easyRandom = new EasyRandom();
    ActionPlan actionPlan = easyRandom.nextObject(ActionPlan.class);
    ActionRule actionRule = new ActionRule();
    actionRule.setActionPlan(actionPlan);
    actionRule.setActionType(ActionType.ICL1E);
    Case testCase = easyRandom.nextObject(Case.class);
    testCase.setTreatmentCode("HH_QF2R1W");
    String uacEng = easyRandom.nextObject(String.class);
    String uacWal = easyRandom.nextObject(String.class);
    String qidEng = "0220000010732199";
    String qidWal = "0320000002861455";

    List<UacQidLink> uacQidLinks = new LinkedList<>();

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setUac(uacEng);
    uacQidLink.setQid(qidEng);
    uacQidLinks.add(uacQidLink);

    uacQidLink = new UacQidLink();
    uacQidLink.setUac(uacWal);
    uacQidLink.setQid(qidWal);
    uacQidLinks.add(uacQidLink);

    when(uacQidLinkRepository.findByCaseId(eq(testCase.getCaseId().toString())))
        .thenReturn(uacQidLinks);

    // When
    ActionInstructionBuilder underTest = new ActionInstructionBuilder(uacQidLinkRepository);
    ActionInstruction actualActionInstruction =
        underTest.buildPrinterActionInstruction(testCase, actionRule);

    // Then
    ActionInstruction expectedActionInstruction =
        getExpectedActionInstruction(
            testCase,
            uacEng,
            uacWal,
            qidEng,
            qidWal,
            actionRule,
            actualActionInstruction.getActionRequest().getActionId());
    assertThat(actualActionInstruction)
        .isEqualToComparingFieldByFieldRecursively(expectedActionInstruction);
  }

  @Test(expected = RuntimeException.class)
  public void testWalesQuestionnaireWithTwoQidUacsWrongEnglish() {
    // Given
    EasyRandom easyRandom = new EasyRandom();
    ActionPlan actionPlan = easyRandom.nextObject(ActionPlan.class);
    ActionRule actionRule = new ActionRule();
    actionRule.setActionPlan(actionPlan);
    actionRule.setActionType(ActionType.ICL1E);
    Case testCase = easyRandom.nextObject(Case.class);
    testCase.setTreatmentCode("HH_QF2R1W");
    String uacEng = easyRandom.nextObject(String.class);
    String uacWal = easyRandom.nextObject(String.class);
    String qidEng = "9920000010732199";
    String qidWal = "0320000002861455";

    List<UacQidLink> uacQidLinks = new LinkedList<>();

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setUac(uacEng);
    uacQidLink.setQid(qidEng);
    uacQidLinks.add(uacQidLink);

    uacQidLink = new UacQidLink();
    uacQidLink.setUac(uacWal);
    uacQidLink.setQid(qidWal);
    uacQidLinks.add(uacQidLink);

    when(uacQidLinkRepository.findByCaseId(eq(testCase.getCaseId().toString())))
        .thenReturn(uacQidLinks);

    // When
    ActionInstructionBuilder underTest = new ActionInstructionBuilder(uacQidLinkRepository);
    underTest.buildPrinterActionInstruction(testCase, actionRule);

    // Then
    // Expect exception to be thrown
  }

  @Test(expected = RuntimeException.class)
  public void testWalesQuestionnaireWithTwoQidUacsWrongWelsh() {
    // Given
    EasyRandom easyRandom = new EasyRandom();
    ActionPlan actionPlan = easyRandom.nextObject(ActionPlan.class);
    ActionRule actionRule = new ActionRule();
    actionRule.setActionPlan(actionPlan);
    actionRule.setActionType(ActionType.ICL1E);
    Case testCase = easyRandom.nextObject(Case.class);
    testCase.setTreatmentCode("HH_QF2R1W");
    String uacEng = easyRandom.nextObject(String.class);
    String uacWal = easyRandom.nextObject(String.class);
    String qidEng = "0220000010732199";
    String qidWal = "9920000002861455";

    List<UacQidLink> uacQidLinks = new LinkedList<>();

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setUac(uacEng);
    uacQidLink.setQid(qidEng);
    uacQidLinks.add(uacQidLink);

    uacQidLink = new UacQidLink();
    uacQidLink.setUac(uacWal);
    uacQidLink.setQid(qidWal);
    uacQidLinks.add(uacQidLink);

    when(uacQidLinkRepository.findByCaseId(eq(testCase.getCaseId().toString())))
        .thenReturn(uacQidLinks);

    // When
    ActionInstructionBuilder underTest = new ActionInstructionBuilder(uacQidLinkRepository);
    underTest.buildPrinterActionInstruction(testCase, actionRule);

    // Then
    // Expect exception to be thrown
  }

  @Test(expected = RuntimeException.class)
  public void testWalesQuestionnaireWithTooManyQidUacs() {
    // Given
    EasyRandom easyRandom = new EasyRandom();
    ActionPlan actionPlan = easyRandom.nextObject(ActionPlan.class);
    ActionRule actionRule = new ActionRule();
    actionRule.setActionPlan(actionPlan);
    actionRule.setActionType(ActionType.ICL1E);
    Case testCase = easyRandom.nextObject(Case.class);
    testCase.setTreatmentCode("HH_QF2R1W");
    String uacEng = easyRandom.nextObject(String.class);
    String uacWal = easyRandom.nextObject(String.class);
    String uacRogue = easyRandom.nextObject(String.class);
    String qidEng = "0220000010732199";
    String qidWal = "0320000002861455";
    String qidRogue = "9920000002874536";

    List<UacQidLink> uacQidLinks = new LinkedList<>();

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setUac(uacEng);
    uacQidLink.setQid(qidEng);
    uacQidLinks.add(uacQidLink);

    uacQidLink = new UacQidLink();
    uacQidLink.setUac(uacWal);
    uacQidLink.setQid(qidWal);
    uacQidLinks.add(uacQidLink);

    uacQidLink = new UacQidLink();
    uacQidLink.setUac(uacRogue);
    uacQidLink.setQid(qidRogue);
    uacQidLinks.add(uacQidLink);

    when(uacQidLinkRepository.findByCaseId(eq(testCase.getCaseId().toString())))
        .thenReturn(uacQidLinks);

    // When
    ActionInstructionBuilder underTest = new ActionInstructionBuilder(uacQidLinkRepository);
    underTest.buildPrinterActionInstruction(testCase, actionRule);

    // Then
    // Expect exception to be thrown
  }

  @Test(expected = RuntimeException.class)
  public void testWalesQuestionnaireWithMissingQidUac() {
    // Given
    EasyRandom easyRandom = new EasyRandom();
    ActionPlan actionPlan = easyRandom.nextObject(ActionPlan.class);
    ActionRule actionRule = new ActionRule();
    actionRule.setActionPlan(actionPlan);
    actionRule.setActionType(ActionType.ICL1E);
    Case testCase = easyRandom.nextObject(Case.class);
    testCase.setTreatmentCode("HH_QF2R1W");
    String uacEng = easyRandom.nextObject(String.class);
    String qidEng = "0220000010732199";

    List<UacQidLink> uacQidLinks = new LinkedList<>();

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setUac(uacEng);
    uacQidLink.setQid(qidEng);
    uacQidLinks.add(uacQidLink);

    when(uacQidLinkRepository.findByCaseId(eq(testCase.getCaseId().toString())))
        .thenReturn(uacQidLinks);

    // When
    ActionInstructionBuilder underTest = new ActionInstructionBuilder(uacQidLinkRepository);
    underTest.buildPrinterActionInstruction(testCase, actionRule);

    // Then
    // Expect exception to be thrown
  }

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

    when(uacQidLinkRepository.findByCaseId(eq(testCase.getCaseId().toString()))).thenReturn(null);

    // When
    ActionInstructionBuilder underTest = new ActionInstructionBuilder(uacQidLinkRepository);
    underTest.buildPrinterActionInstruction(testCase, actionRule);

    // Then
    // Expect exception to be thrown
  }

  @Test(expected = RuntimeException.class)
  public void testQidLinksEmpty() {
    // Given
    EasyRandom easyRandom = new EasyRandom();
    ActionPlan actionPlan = easyRandom.nextObject(ActionPlan.class);
    ActionRule actionRule = new ActionRule();
    actionRule.setActionPlan(actionPlan);
    actionRule.setActionType(ActionType.ICL1E);
    Case testCase = easyRandom.nextObject(Case.class);
    testCase.setTreatmentCode("HH_QF2R1W");

    when(uacQidLinkRepository.findByCaseId(eq(testCase.getCaseId().toString())))
        .thenReturn(Collections.EMPTY_LIST);

    // When
    ActionInstructionBuilder underTest = new ActionInstructionBuilder(uacQidLinkRepository);
    underTest.buildPrinterActionInstruction(testCase, actionRule);

    // Then
    // Expect exception to be thrown
  }

  @Test(expected = RuntimeException.class)
  public void testMulitpleQidLinksAmbiguous() {
    // Given
    EasyRandom easyRandom = new EasyRandom();
    ActionPlan actionPlan = easyRandom.nextObject(ActionPlan.class);
    ActionRule actionRule = new ActionRule();
    actionRule.setActionPlan(actionPlan);
    actionRule.setActionType(ActionType.ICL1E);
    Case testCase = easyRandom.nextObject(Case.class);
    testCase.setTreatmentCode("HH_LF2R1E");
    String uacEng = easyRandom.nextObject(String.class);
    String uacWal = easyRandom.nextObject(String.class);
    String qidEng = "0220000010732199";
    String qidWal = "0320000002861455";

    List<UacQidLink> uacQidLinks = new LinkedList<>();

    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setUac(uacEng);
    uacQidLink.setQid(qidEng);
    uacQidLinks.add(uacQidLink);

    uacQidLink = new UacQidLink();
    uacQidLink.setUac(uacWal);
    uacQidLink.setQid(qidWal);
    uacQidLinks.add(uacQidLink);

    when(uacQidLinkRepository.findByCaseId(eq(testCase.getCaseId().toString())))
        .thenReturn(uacQidLinks);

    // When
    ActionInstructionBuilder underTest = new ActionInstructionBuilder(uacQidLinkRepository);
    underTest.buildPrinterActionInstruction(testCase, actionRule);

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
    when(uacQidLinkRepository.findByCaseId(eq(caze.getCaseId().toString())))
        .thenReturn(Collections.singletonList(uacQidLink));

    // When
    ActionInstructionBuilder underTest = new ActionInstructionBuilder(uacQidLinkRepository);
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
    actionRequest.setIacWales(uacWales);
    actionRequest.setQid(qid);
    actionRequest.setQidWales(qidWales);
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
