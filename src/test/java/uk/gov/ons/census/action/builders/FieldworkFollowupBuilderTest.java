package uk.gov.ons.census.action.builders;

import static org.assertj.core.api.Assertions.assertThat;

import org.jeasy.random.EasyRandom;
import org.junit.Test;
import uk.gov.ons.census.action.model.dto.FieldworkFollowup;
import uk.gov.ons.census.action.model.entity.ActionPlan;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;

public class FieldworkFollowupBuilderTest {

  @Test
  public void testLatAndLongCopiedToAddress() {
    // Given
    EasyRandom easyRandom = new EasyRandom();
    Case caze = easyRandom.nextObject(Case.class);
    caze.setLatitude("1.123456789999");
    caze.setLongitude("-9.987654321111");

    ActionRule actionRule = generateRandomActionRule(easyRandom);

    // When
    FieldworkFollowupBuilder underTest = new FieldworkFollowupBuilder();
    FieldworkFollowup actualResult =
        underTest.buildFieldworkFollowup(
            caze, actionRule.getActionPlan().getId().toString(), actionRule.getActionType().name());

    // Then
    assertThat(caze.getLatitude()).isEqualTo(actualResult.getLatitude());
    assertThat(caze.getLongitude()).isEqualTo(actualResult.getLongitude());
  }

  @Test
  public void testFWMTRequiredFields() {
    // Given
    EasyRandom easyRandom = new EasyRandom();
    Case caze = easyRandom.nextObject(Case.class);

    ActionRule actionRule = generateRandomActionRule(easyRandom);

    // When
    FieldworkFollowupBuilder underTest = new FieldworkFollowupBuilder();
    FieldworkFollowup actualResult =
        underTest.buildFieldworkFollowup(
            caze, actionRule.getActionPlan().getId().toString(), actionRule.getActionType().name());

    // Then
    assertThat(actualResult.getSurveyName()).isEqualTo("CENSUS");
    assertThat(actualResult.getUndeliveredAsAddress()).isEqualTo(caze.isUndeliveredAsAddressed());
    assertThat(actualResult.getBlankQreReturned()).isFalse();
    assertThat(actualResult.getCaseId()).isEqualTo(caze.getCaseId().toString());
    assertThat(actualResult.getCaseRef()).isEqualTo(Integer.toString(caze.getCaseRef()));
    assertThat(actualResult)
        .isEqualToIgnoringGivenFields(
            caze,
            "actionPlan",
            "actionType",
            "surveyName",
            "undeliveredAsAddress",
            "blankQreReturned",
            "caseId",
            "caseRef");
  }

  private ActionRule generateRandomActionRule(EasyRandom easyRandom) {
    ActionPlan actionPlan = easyRandom.nextObject(ActionPlan.class);
    ActionRule actionRule = new ActionRule();
    actionRule.setActionPlan(actionPlan);
    actionRule.setActionType(ActionType.FIELD);
    return actionRule;
  }
}
