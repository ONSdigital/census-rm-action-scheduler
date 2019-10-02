package uk.gov.ons.census.action.schedule;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.ons.census.action.model.entity.ActionPlan;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;
import uk.gov.ons.census.action.model.repository.CaseClassifier;

public class ActionRuleProcessorTest {
  private final ActionRuleRepository actionRuleRepo = mock(ActionRuleRepository.class);
  private final CaseClassifier caseClassifier = mock(CaseClassifier.class);

  @Test
  public void testExecuteClassifiers() {
    // Given
    ActionRule actionRule = setUpActionRule(ActionType.ICL1E);
    Map<String, List<String>> classifiers = new HashMap<>();
    List<String> columnValues = Arrays.asList("a", "b", "c");
    classifiers.put("A_Column", columnValues);
    actionRule.setClassifiers(classifiers);

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

    Map<String, List<String>> classifiers = new HashMap<>();
    classifiers.put("A Key", new ArrayList<>());

    actionRule.setClassifiers(classifiers);
    actionRule.setActionType(actionType);

    ActionPlan actionPlan = new ActionPlan();
    actionPlan.setId(UUID.randomUUID());

    actionRule.setActionPlan(actionPlan);

    return actionRule;
  }
}
