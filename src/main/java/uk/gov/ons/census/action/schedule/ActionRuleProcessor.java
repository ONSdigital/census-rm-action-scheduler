package uk.gov.ons.census.action.schedule;

import static org.springframework.data.jpa.domain.Specification.where;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder.In;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;
import uk.gov.ons.census.action.model.repository.CaseRepository;

@Component
public class ActionRuleProcessor {

  private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(50);

  private final ActionRuleRepository actionRuleRepo;
  private final CaseRepository caseRepository;
  private final ActionRequestSender actionRequestSender;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  public ActionRuleProcessor(
      ActionRuleRepository actionRuleRepo,
      CaseRepository caseRepository,
      ActionRequestSender actionRequestSender) {
    this.actionRuleRepo = actionRuleRepo;
    this.caseRepository = caseRepository;
    this.actionRequestSender = actionRequestSender;
  }

  @Transactional
  public void processActionRules() {
    List<ActionRule> triggeredActionRules =
        actionRuleRepo.findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(OffsetDateTime.now());

    for (ActionRule triggeredActionRule : triggeredActionRules) {
      createScheduledActions(triggeredActionRule);
      triggeredActionRule.setHasTriggered(true);
      actionRuleRepo.save(triggeredActionRule);
    }
  }

  private void createScheduledActions(ActionRule triggeredActionRule) {
    if (triggeredActionRule.getClassifiers() == null
        || triggeredActionRule.getClassifiers().isEmpty()) {
      executeAllCases(triggeredActionRule);
    } else {
      executeClassifiedCases(triggeredActionRule);
    }
  }

  private void executeAllCases(ActionRule triggeredActionRule) {
    String actionPlanId = triggeredActionRule.getActionPlan().getId().toString();

    try (Stream<Case> cases = caseRepository.findByActionPlanId(actionPlanId)) {
      executeCases(cases, triggeredActionRule);
    }
  }

  private void executeClassifiedCases(ActionRule triggeredActionRule) {
    String actionPlanId = triggeredActionRule.getActionPlan().getId().toString();

    Specification<Case> specification = where(isActionPlanIdEqualTo(actionPlanId));

    for (Map.Entry<String, List<String>> classifier :
        triggeredActionRule.getClassifiers().entrySet()) {
      specification = specification.and(isClassifierIn(classifier.getKey(), classifier.getValue()));
    }

    try (Stream<Case> cases = caseRepository.findAll(specification).stream()) {
      executeCases(cases, triggeredActionRule);
    }
  }

  private void executeCases(Stream<Case> cases, ActionRule triggeredActionRule) {
    List<Callable<Boolean>> callables = new LinkedList<>();
    cases.forEach(
        caze -> {
          callables.add(
              () -> {
                actionRequestSender.createAndSendActionRequest(caze, triggeredActionRule);
                return Boolean.TRUE;
              });
        });

    try {
      List<Future<Boolean>> results = EXECUTOR_SERVICE.invokeAll(callables);

      for (Future<Boolean> result : results) {
        if (result.get() != Boolean.TRUE) {
          throw new RuntimeException(); // One of the threads had a problem
        }
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(); // Roll the whole transaction back
    } catch (ExecutionException e) {
      throw new RuntimeException(); // Roll the whole transaction back
    }
  }

  private Specification<Case> isActionPlanIdEqualTo(String actionPlanId) {
    return (Specification<Case>)
        (root, query, builder) -> builder.equal(root.get("actionPlanId"), actionPlanId);
  }

  private Specification<Case> isClassifierIn(
      final String fieldName, final List<String> inClauseValues) {
    return (Specification<Case>)
        (root, query, builder) -> {
          In<String> inClause = builder.in(root.get(fieldName));
          for (String inClauseValue : inClauseValues) {
            inClause.value(inClauseValue);
          }
          return inClause;
        };
  }
}
