package uk.gov.ons.census.actionsvc.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ActionRuleScheduler {
  private final ActionRuleProcessor actionRuleProcessor;

  public ActionRuleScheduler(ActionRuleProcessor actionRuleProcessor) {
    this.actionRuleProcessor = actionRuleProcessor;
  }

  @Scheduled(fixedDelay = 60000)
  public void processActionRules() {
    actionRuleProcessor.processActionRules();
  }
}
