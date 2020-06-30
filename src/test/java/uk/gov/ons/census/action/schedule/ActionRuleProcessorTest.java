package uk.gov.ons.census.action.schedule;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.ons.census.action.model.entity.ActionPlan;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;

public class ActionRuleProcessorTest {
  private final ActionRuleRepository actionRuleRepo = mock(ActionRuleRepository.class);
  private final CaseClassifier caseClassifier = mock(CaseClassifier.class);

  @Test
  public void testExecuteClassifiers() {
    // Given
    ActionRule actionRule = setUpActionRule(ActionType.ICL1E);
    String userDefinedWhereClause = " AND treatment_code IN ('abc', 'xyz')";
    actionRule.setUserDefinedWhereClause(userDefinedWhereClause);

    // when
    ActionRuleProcessor actionRuleProcessor =
        new ActionRuleProcessor(caseClassifier, actionRuleRepo);
    actionRuleProcessor.createScheduledActions(actionRule);

    // then
    ArgumentCaptor<ActionRule> actionRuleCaptor = ArgumentCaptor.forClass(ActionRule.class);
    verify(actionRuleRepo, times(1)).save(actionRuleCaptor.capture());
    ActionRule actualActionRule = actionRuleCaptor.getAllValues().get(0);
    actionRule.setHasTriggered(true);
    Assertions.assertThat(actualActionRule).isEqualTo(actionRule);

    verify(caseClassifier).enqueueCasesForActionRule(eq(actionRule));
  }

  private ActionRule setUpActionRule(ActionType actionType) {
    ActionRule actionRule = new ActionRule();
    UUID actionRuleId = UUID.randomUUID();
    actionRule.setId(actionRuleId);
    actionRule.setTriggerDateTime(OffsetDateTime.now());
    actionRule.setHasTriggered(false);

    String userDefinedWhereClause = " AND treatment_code IN ('abc', 'xyz')";
    actionRule.setUserDefinedWhereClause(userDefinedWhereClause);

    actionRule.setActionType(actionType);

    ActionPlan actionPlan = new ActionPlan();
    actionPlan.setId(UUID.randomUUID());

    actionRule.setActionPlan(actionPlan);

    return actionRule;
  }
}
