package uk.gov.ons.census.action.schedule;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;

@Component
public class ActionRuleTriggerer {
  private final ActionRuleRepository actionRuleRepo;
  private final ActionRuleProcessor actionRuleProcessor;

  public ActionRuleTriggerer(ActionRuleRepository actionRuleRepo,
      ActionRuleProcessor actionRuleProcessor) {
    this.actionRuleRepo = actionRuleRepo;
    this.actionRuleProcessor = actionRuleProcessor;
  }

  @Transactional
  public void triggerActionRules() {
    List<ActionRule> triggeredActionRules =
        actionRuleRepo.findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(OffsetDateTime.now());

    for (ActionRule triggeredActionRule : triggeredActionRules) {
      actionRuleProcessor.createScheduledActions(triggeredActionRule);
    }
  }

}
