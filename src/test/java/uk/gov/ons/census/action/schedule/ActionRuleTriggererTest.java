package uk.gov.ons.census.action.schedule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;

public class ActionRuleTriggererTest {
  private final ActionRuleRepository actionRuleRepo = mock(ActionRuleRepository.class);
  private final ActionRuleProcessor actionRuleProcessor = mock(ActionRuleProcessor.class);

  @Test
  public void testTriggerActionRules() {
    // Given
    ActionRule actionRule = new ActionRule();
    when(actionRuleRepo.findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(
            any(OffsetDateTime.class)))
        .thenReturn(Collections.singletonList(actionRule));

    // When
    ActionRuleTriggerer underTest = new ActionRuleTriggerer(actionRuleRepo, actionRuleProcessor);
    underTest.triggerActionRules();

    // Then
    verify(actionRuleProcessor).createScheduledActions(eq(actionRule));
  }

  @Test
  public void testTriggerMultipleActionRules() {
    // Given
    List<ActionRule> actionRules = new ArrayList<>(50);
    for (int i = 0; i < 50; i++) {
      actionRules.add(new ActionRule());
    }

    when(actionRuleRepo.findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(
            any(OffsetDateTime.class)))
        .thenReturn(actionRules);

    // When
    ActionRuleTriggerer underTest = new ActionRuleTriggerer(actionRuleRepo, actionRuleProcessor);
    underTest.triggerActionRules();

    // Then
    verify(actionRuleProcessor, times(50)).createScheduledActions(any(ActionRule.class));
  }
}
