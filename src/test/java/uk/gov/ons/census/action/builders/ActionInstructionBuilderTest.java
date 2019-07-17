package uk.gov.ons.census.action.builders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jeasy.random.EasyRandom;
import org.junit.Test;
import uk.gov.ons.census.action.model.UacQidTuple;
import uk.gov.ons.census.action.model.dto.FieldworkFollowup;
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
    underTest.buildFieldworkFollowup(testCase, actionRule);

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
    FieldworkFollowup actualResult = underTest.buildFieldworkFollowup(caze, actionRule);

    // Then
    assertThat(caze.getLatitude()).isEqualTo(actualResult.getLatitude().toString());
    assertThat(caze.getLongitude()).isEqualTo(actualResult.getLongitude().toString());
  }
}
