package uk.gov.ons.census.action.schedule;

import static org.springframework.data.jpa.domain.Specification.where;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder.In;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.builders.FieldworkFollowupBuilder;
import uk.gov.ons.census.action.builders.PrintCaseSelectedBuilder;
import uk.gov.ons.census.action.builders.PrintFileDtoBuilder;
import uk.gov.ons.census.action.model.dto.FieldworkFollowup;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.entity.ActionHandler;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;
import uk.gov.ons.census.action.model.repository.CustomCaseRepository;

@Component
public class ActionRuleProcessor {
  private static final Logger log = LoggerFactory.getLogger(ActionRuleScheduler.class);
  private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(50);
  private static final String ROUTING_KEY_PREFIX = "Action.";
  private static final String ROUTING_KEY_SUFFIX = ".binding";

  private final ActionRuleRepository actionRuleRepo;
  private final FieldworkFollowupBuilder fieldworkFollowupBuilder;
  private final PrintFileDtoBuilder printFileDtoBuilder;
  private final PrintCaseSelectedBuilder printCaseSelectedBuilder;
  private final RabbitTemplate rabbitTemplate;
  private final CustomCaseRepository customCaseRepository;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  @Value("${queueconfig.action-case-exchange}")
  private String actionCaseExchange;

  public ActionRuleProcessor(
      ActionRuleRepository actionRuleRepo,
      FieldworkFollowupBuilder fieldworkFollowupBuilder,
      PrintFileDtoBuilder printFileDtoBuilder,
      PrintCaseSelectedBuilder printCaseSelectedBuilder,
      RabbitTemplate rabbitTemplate,
      CustomCaseRepository customCaseRepository) {
    this.actionRuleRepo = actionRuleRepo;
    this.fieldworkFollowupBuilder = fieldworkFollowupBuilder;
    this.printFileDtoBuilder = printFileDtoBuilder;
    this.printCaseSelectedBuilder = printCaseSelectedBuilder;
    this.rabbitTemplate = rabbitTemplate;
    this.customCaseRepository = customCaseRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW) // Start a new transaction for every rule
  public void createScheduledActions(ActionRule triggeredActionRule) {
    executeClassifiedCases(triggeredActionRule);
    triggeredActionRule.setHasTriggered(true);
    actionRuleRepo.save(triggeredActionRule);
  }

  private void executeClassifiedCases(ActionRule triggeredActionRule) {
    String actionPlanId = triggeredActionRule.getActionPlan().getId().toString();
    Specification<Case> specification = createSpecificationForActionableCases(actionPlanId);

    for (Map.Entry<String, List<String>> classifier :
        triggeredActionRule.getClassifiers().entrySet()) {
      specification = specification.and(isClassifierIn(classifier.getKey(), classifier.getValue()));
    }

    try (Stream<Case> cases = customCaseRepository.streamAll(specification); ) {
      executeCases(cases, triggeredActionRule);
    }
  }

  private void executeCases(Stream<Case> cases, ActionRule triggeredActionRule) {
    if (triggeredActionRule.getActionType().getHandler() == ActionHandler.PRINTER) {
      executePrinterCases(cases, triggeredActionRule);
    } else if (triggeredActionRule.getActionType().getHandler() == ActionHandler.FIELD) {
      executeFieldCases(cases, triggeredActionRule);
    }
  }

  private String getRoutingKey(ActionRule triggeredActionRule) {
    return String.format(
        "%s%s%s",
        ROUTING_KEY_PREFIX,
        triggeredActionRule.getActionType().getHandler().getRoutingKey(),
        ROUTING_KEY_SUFFIX);
  }

  private void executePrinterCases(Stream<Case> cases, ActionRule triggeredActionRule) {
    UUID batchId = UUID.randomUUID();
    String routingKey = getRoutingKey(triggeredActionRule);

    final String packCode =
        actionTypeToPackCodeMap.get(triggeredActionRule.getActionType().toString());

    List<Callable<PrintFileDto>> printFileDtoBuilders = new LinkedList<>();

    cases.forEach(
        caze ->
            printFileDtoBuilders.add(
                () ->
                    printFileDtoBuilder.buildPrintFileDto(
                        caze, packCode, batchId, triggeredActionRule.getActionType())));

    try {
      final int batchQty = printFileDtoBuilders.size();
      List<Future<PrintFileDto>> results = EXECUTOR_SERVICE.invokeAll(printFileDtoBuilders);

      Logger batchLog = log.with("batchId", batchId).with("packCode", packCode);
      batchLog.info("About to send {} printer action messages", results.size());

      int messagesSent = 0;

      for (Future<PrintFileDto> result : results) {
        PrintFileDto printFileDto = result.get();
        printFileDto.setBatchQuantity(batchQty);
        rabbitTemplate.convertAndSend(outboundExchange, routingKey, printFileDto);

        ResponseManagementEvent printCaseSelected =
            printCaseSelectedBuilder.buildMessage(printFileDto, triggeredActionRule.getId());

        rabbitTemplate.convertAndSend(actionCaseExchange, "", printCaseSelected);

        if (messagesSent++ % 1000 == 0) {
          batchLog.info(
              "Sending in progress, sent {} printer action messages so far", messagesSent - 1);
        }
      }
      batchLog.info("Finished sending, sent {} printer action messages", messagesSent);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e); // Roll the whole transaction back
    }
  }

  private void executeFieldCases(Stream<Case> cases, ActionRule triggeredActionRule) {
    List<Callable<FieldworkFollowup>> fieldworkFollowupBuilders = new LinkedList<>();

    cases.forEach(
        caze ->
            fieldworkFollowupBuilders.add(
                () -> fieldworkFollowupBuilder.buildFieldworkFollowup(caze, triggeredActionRule)));

    log.with("Triggered action rule", triggeredActionRule);

    try {
      final String routingKey = getRoutingKey(triggeredActionRule);

      List<Future<FieldworkFollowup>> results =
          EXECUTOR_SERVICE.invokeAll(fieldworkFollowupBuilders);

      log.info("About to send {} field action messages", results.size());
      int messagesSent = 0;
      for (Future<FieldworkFollowup> result : results) {

        rabbitTemplate.convertAndSend(outboundExchange, routingKey, result.get());
        if (messagesSent++ % 1000 == 0) {
          log.info("Finished sending, sent {} field action messages", messagesSent - 1);
        }
      }
      log.info("Finished sending, sent {} field action messages", messagesSent);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e); // Roll the whole transaction back
    }
  }

  private Specification<Case> createSpecificationForActionableCases(String actionPlanId) {
    return where(isActionPlanIdEqualTo(actionPlanId))
        .and(excludeReceiptedCases().and(excludeRefusedCases()).and(excludeAddressInvalidCases()));
  }

  private Specification<Case> isActionPlanIdEqualTo(String actionPlanId) {
    return (Specification<Case>)
        (root, query, builder) -> builder.equal(root.get("actionPlanId"), actionPlanId);
  }

  private Specification<Case> excludeReceiptedCases() {
    return (Specification<Case>)
        (root, query, builder) -> builder.equal(root.get("receiptReceived"), false);
  }

  private Specification<Case> excludeRefusedCases() {
    return (Specification<Case>)
        (root, query, builder) -> builder.equal(root.get("refusalReceived"), false);
  }

  private Specification<Case> excludeAddressInvalidCases() {
    return (Specification<Case>)
        (root, query, builder) -> builder.equal(root.get("addressInvalid"), false);
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

  private final HashMap<String, String> actionTypeToPackCodeMap =
      new HashMap<>() {
        {
          put(ActionType.ICHHQE.name(), "P_IC_H1");
          put(ActionType.ICHHQW.name(), "P_IC_H2");
          put(ActionType.ICHHQN.name(), "P_IC_H4");
          put(ActionType.ICL1E.name(), "P_IC_ICL1");
          put(ActionType.ICL2W.name(), "P_IC_ICL2B");
          put(ActionType.ICL4N.name(), "P_IC_ICL4");
          put(ActionType.P_RL_1RL1_1.name(), ActionType.P_RL_1RL1_1.name());
          put(ActionType.P_RL_1RL2B_1.name(), ActionType.P_RL_1RL2B_1.name());
          put(ActionType.P_RL_1RL4.name(), ActionType.P_RL_1RL4.name());
          put(ActionType.P_RL_1RL1_2.name(), ActionType.P_RL_1RL1_2.name());
          put(ActionType.P_RL_1RL2B_2.name(), ActionType.P_RL_1RL2B_2.name());
          put(ActionType.P_RL_2RL1_3a.name(), ActionType.P_RL_2RL1_3a.name());
          put(ActionType.P_RL_2RL2B_3a.name(), ActionType.P_RL_2RL2B_3a.name());
          put(ActionType.P_QU_H1.name(), ActionType.P_QU_H1.name());
          put(ActionType.P_QU_H2.name(), ActionType.P_QU_H2.name());
          put(ActionType.P_QU_H4.name(), ActionType.P_QU_H4.name());
        }
      };
}
