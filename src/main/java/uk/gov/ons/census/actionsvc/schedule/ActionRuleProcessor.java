package uk.gov.ons.census.actionsvc.schedule;

import static org.springframework.data.jpa.domain.Specification.where;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder.In;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.actionsvc.model.dto.instruction.ActionAddress;
import uk.gov.ons.census.actionsvc.model.dto.instruction.ActionEvent;
import uk.gov.ons.census.actionsvc.model.dto.instruction.ActionInstruction;
import uk.gov.ons.census.actionsvc.model.dto.instruction.ActionRequest;
import uk.gov.ons.census.actionsvc.model.dto.instruction.Priority;
import uk.gov.ons.census.actionsvc.model.entity.ActionRule;
import uk.gov.ons.census.actionsvc.model.entity.Case;
import uk.gov.ons.census.actionsvc.model.repository.ActionRuleRepository;
import uk.gov.ons.census.actionsvc.model.repository.CaseRepository;

@Component
public class ActionRuleProcessor {
  private final ActionRuleRepository actionRuleRepo;
  private final CaseRepository caseRepository;
  private final RabbitTemplate rabbitTemplate;

  @Value("${queueconfig.outbound-queue}")
  private String outboundQueue;

  public ActionRuleProcessor(
      ActionRuleRepository actionRuleRepo,
      CaseRepository caseRepository,
      @Qualifier("actionInstructionRabbitTemplate") RabbitTemplate rabbitTemplate) {
    this.actionRuleRepo = actionRuleRepo;
    this.caseRepository = caseRepository;
    this.rabbitTemplate = rabbitTemplate;
  }

  @Transactional
  public void processActionRules() {
    List<ActionRule> triggeredActionRules =
        actionRuleRepo.findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(OffsetDateTime.now());
    for (ActionRule triggeredActionRule : triggeredActionRules) {
      createScheduledActions(triggeredActionRule);
      triggeredActionRule.setHasTriggered(true);
      actionRuleRepo.saveAndFlush(triggeredActionRule);
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
      cases.forEach(caze -> createAndSendActionRequest(caze));
    }
  }

  private void executeClassifiedCases(ActionRule triggeredActionRule) {
    String actionPlanId = triggeredActionRule.getActionPlan().getId().toString();

    Specification<Case> specification = where(isActionPlanIdEqualTo(actionPlanId));

    for (Map.Entry<String, List<String>> classifier :
        triggeredActionRule.getClassifiers().entrySet()) {
      specification = specification.and(isClassifierIn(classifier.getKey(), classifier.getValue()));
    }

    List<Case> caseList = caseRepository.findAll(specification);
    caseList.forEach(caze -> createAndSendActionRequest(caze));
  }

  private void createAndSendActionRequest(Case caze) {
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
    actionRequest.setActionPlan("77056b5a-c329-4b98-b85e-c981e03f98e8");
    actionRequest.setActionType("ICL1E");
    actionRequest.setAddress(actionAddress);
    actionRequest.setLegalBasis("Statistics of Trade Act 1947");
    actionRequest.setCaseGroupStatus("NOTSTARTED");
    actionRequest.setCaseId(caze.getCaseId().toString());
    actionRequest.setPriority(Priority.MEDIUM);
    actionRequest.setCaseRef(Long.toString(caze.getCaseRef()));
    actionRequest.setIac(caze.getUac());
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
