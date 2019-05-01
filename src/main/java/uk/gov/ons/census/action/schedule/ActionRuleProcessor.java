package uk.gov.ons.census.action.schedule;

import static org.springframework.data.jpa.domain.Specification.where;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder.In;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.dto.instruction.ActionAddress;
import uk.gov.ons.census.action.model.dto.instruction.ActionEvent;
import uk.gov.ons.census.action.model.dto.instruction.ActionInstruction;
import uk.gov.ons.census.action.model.dto.instruction.ActionRequest;
import uk.gov.ons.census.action.model.dto.instruction.Priority;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

@Component
public class ActionRuleProcessor {
  private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(50);

  private final ActionRuleRepository actionRuleRepo;
  private final CaseRepository caseRepository;
  private final UacQidLinkRepository uacQidLinkRepository;
  private final RabbitTemplate rabbitTemplate;

  @Value("${queueconfig.outbound-queue}")
  private String outboundQueue;

  public ActionRuleProcessor(
      ActionRuleRepository actionRuleRepo,
      CaseRepository caseRepository,
      UacQidLinkRepository uacQidLinkRepository,
      @Qualifier("actionInstructionRabbitTemplate") RabbitTemplate rabbitTemplate) {
    this.actionRuleRepo = actionRuleRepo;
    this.caseRepository = caseRepository;
    this.uacQidLinkRepository = uacQidLinkRepository;
    this.rabbitTemplate = rabbitTemplate;
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
      cases.forEach(caze -> createAndSendActionRequest(caze, triggeredActionRule));
    }
  }

  private void executeClassifiedCases(ActionRule triggeredActionRule) {
    String actionPlanId = triggeredActionRule.getActionPlan().getId().toString();

    Specification<Case> specification = where(isActionPlanIdEqualTo(actionPlanId));

    for (Map.Entry<String, List<String>> classifier : triggeredActionRule.getClassifiers().entrySet()) {
      specification = specification.and(isClassifierIn(classifier.getKey(), classifier.getValue()));
    }

    List<Case> caseList = caseRepository.findAll(specification);
    List<Callable<Boolean>> callables = new ArrayList<>(caseList.size());
    caseList.forEach(
        caze -> {
          callables.add(
              () -> {
                createAndSendActionRequest(caze, triggeredActionRule);
                return Boolean.TRUE;
              });
        });

    try {
      EXECUTOR_SERVICE.invokeAll(callables);
    } catch (InterruptedException e) {
      throw new RuntimeException(); // KABOOM - WHOLE THING ROLLS BACK
    }
  }

  private void createAndSendActionRequest(Case caze, ActionRule actionRule) {

    List<UacQidLink> uacQidLinks = uacQidLinkRepository.findByCaseId(caze.getCaseId().toString());

    if (uacQidLinks == null || uacQidLinks.isEmpty()) {
      throw new RuntimeException(); // TODO: How can we process this case without a UAC?
    } else if (uacQidLinks.size() > 1) {
      throw new RuntimeException(); // TODO: How do we know which one to use?
    }

    String uac = uacQidLinks.get(0).getUac();

    ActionEvent actionEvent = new ActionEvent();
    actionEvent
        .getEvents()
        .add("CASE_CREATED : null : SYSTEM : Case created when Initial creation of case");
    ActionAddress actionAddress = new ActionAddress();
    actionAddress.setLine1(caze.getAddressLine1());
    actionAddress.setLine2(caze.getAddressLine2());
    actionAddress.setLine3(caze.getAddressLine3());
    actionAddress.setTownName(caze.getTownName());
    actionAddress.setPostcode(caze.getPostcode());
    actionAddress.setOrganisationName(caze.getOrganisationName());
    ActionRequest actionRequest = new ActionRequest();
    actionRequest.setActionId(UUID.randomUUID().toString());
    actionRequest.setResponseRequired(false);
    actionRequest.setActionPlan(actionRule.getActionPlan().getId().toString());
    actionRequest.setActionType(actionRule.getActionType().toString());
    actionRequest.setAddress(actionAddress);
    actionRequest.setLegalBasis("Statistics of Trade Act 1947");
    actionRequest.setCaseGroupStatus("NOTSTARTED");
    actionRequest.setCaseId(caze.getCaseId().toString());
    actionRequest.setPriority(Priority.MEDIUM);
    actionRequest.setCaseRef(Long.toString(caze.getCaseRef()));
    actionRequest.setIac(uac);
    actionRequest.setEvents(actionEvent);
    actionRequest.setExerciseRef("201904");
    actionRequest.setUserDescription("Census-FNSM580JQE3M4");
    actionRequest.setSurveyName("Census-FNSM580JQE3M4");
    actionRequest.setSurveyRef("Census-FNSM580JQE3M4");
    actionRequest.setReturnByDate("27/04");
    actionRequest.setSampleUnitRef("DDR190314000000516472");
    ActionInstruction actionInstruction = new ActionInstruction();
    actionInstruction.setActionRequest(actionRequest);

    rabbitTemplate.convertAndSend(outboundQueue, "", actionInstruction);
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
