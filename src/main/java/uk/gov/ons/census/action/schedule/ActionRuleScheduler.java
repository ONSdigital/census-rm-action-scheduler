package uk.gov.ons.census.action.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ActionRuleScheduler {
  private final ActionRuleProcessor actionRuleProcessor;

  public ActionRuleScheduler(ActionRuleProcessor actionRuleProcessor) {
    this.actionRuleProcessor = actionRuleProcessor;
  }

  @Scheduled(fixedDelayString = "${scheduler.frequency}")
  public void processActionRules() {
    try {
      actionRuleProcessor.processActionRules();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
