package uk.gov.ons.census.action.schedule;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;
import uk.gov.ons.census.action.model.repository.CaseClassifier;

@Component
public class ActionRuleProcessor {
  private final CaseClassifier caseClassifier;
  private final ActionRuleRepository actionRuleRepo;

  public ActionRuleProcessor(CaseClassifier caseClassifier, ActionRuleRepository actionRuleRepo) {
    this.caseClassifier = caseClassifier;
    this.actionRuleRepo = actionRuleRepo;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW) // Start a new transaction for every rule
  public void createScheduledActions(ActionRule triggeredActionRule) {
    caseClassifier.enqueueCasesForActionRule(triggeredActionRule);
    triggeredActionRule.setHasTriggered(true);
    actionRuleRepo.save(triggeredActionRule);
  }
}
