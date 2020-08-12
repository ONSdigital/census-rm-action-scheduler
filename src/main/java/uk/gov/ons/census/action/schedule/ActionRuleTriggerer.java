package uk.gov.ons.census.action.schedule;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;

@Component
public class ActionRuleTriggerer {
  private static final Logger log = LoggerFactory.getLogger(ActionRuleTriggerer.class);
  private final ActionRuleRepository actionRuleRepo;
  private final ActionRuleProcessor actionRuleProcessor;

  public ActionRuleTriggerer(
      ActionRuleRepository actionRuleRepo, ActionRuleProcessor actionRuleProcessor) {
    this.actionRuleRepo = actionRuleRepo;
    this.actionRuleProcessor = actionRuleProcessor;
  }

  @Transactional
  public void triggerActionRules() {
    List<ActionRule> triggeredActionRules =
        actionRuleRepo.findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(OffsetDateTime.now());

    for (ActionRule triggeredActionRule : triggeredActionRules) {
      try {
        actionRuleProcessor.createScheduledActions(triggeredActionRule);
      } catch (Exception e) {
        log.with("action_rule_id", triggeredActionRule.getId())
            .error("Stop putting untested SQL into production", e);
      }
    }
  }
}
