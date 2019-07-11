package uk.gov.ons.census.action.schedule;

import static org.springframework.data.jpa.domain.Specification.where;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.time.OffsetDateTime;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder.In;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.builders.ActionInstructionBuilder;
import uk.gov.ons.census.action.builders.PrintCaseSelectedBuilder;
import uk.gov.ons.census.action.builders.PrintFileDtoBuilder;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.entity.ActionHandler;
import uk.gov.ons.census.action.model.entity.ActionRule;
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
  private final ActionInstructionBuilder actionInstructionBuilder;
  private final PrintFileDtoBuilder printFileDtoBuilder;
  private final PrintCaseSelectedBuilder printCaseSelectedBuilder;
  private final RabbitTemplate rabbitTemplate;
  private final CustomCaseRepository customCaseRepository;
  private final RabbitTemplate rabbitFieldTemplate;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  @Value("${queueconfig.action-case-exchange}")
  private String actionCaseExchange;

  public ActionRuleProcessor(
      ActionRuleRepository actionRuleRepo,
      ActionInstructionBuilder actionInstructionBuilder,
      PrintFileDtoBuilder printFileDtoBuilder,
      PrintCaseSelectedBuilder printCaseSelectedBuilder,
      RabbitTemplate rabbitTemplate,
      CustomCaseRepository customCaseRepository,
      @Qualifier("actionInstructionFieldRabbitTemplate") RabbitTemplate rabbitFieldTemplate) {
    this.actionRuleRepo = actionRuleRepo;
    this.actionInstructionBuilder = actionInstructionBuilder;
    this.printFileDtoBuilder = printFileDtoBuilder;
    this.printCaseSelectedBuilder = printCaseSelectedBuilder;
    this.rabbitTemplate = rabbitTemplate;
    this.customCaseRepository = customCaseRepository;
    this.rabbitFieldTemplate = rabbitFieldTemplate;
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
    executeClassifiedCases(triggeredActionRule);
  }

  private void executeClassifiedCases(ActionRule triggeredActionRule) {
    String actionPlanId = triggeredActionRule.getActionPlan().getId().toString();
    Specification<Case> specification = createSpecificationForUnreceiptedCases(actionPlanId);

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

    // Run Mapping in Parallel then collect to list and send them sequentially to Rabbit as it
    // doesn't play nicely with Spring's @Transaction :(
    List<PrintFileDto> caseList =
        cases
            .parallel()
            .map(
                caze ->
                    printFileDtoBuilder.buildPrintFileDto(
                        caze, packCode, batchId, triggeredActionRule.getActionType().toString()))
            .collect(Collectors.toList());

    final int batchQty = caseList.size();

    log.info("About to send {} PrintFileDto messages", caseList.size());

    caseList.forEach(
        printFileDto -> {
          printFileDto.setBatchQty(batchQty);
          rabbitTemplate.convertAndSend(outboundExchange, routingKey, printFileDto);

          ResponseManagementEvent printCaseSelected =
              printCaseSelectedBuilder.buildMessage(printFileDto, triggeredActionRule.getId());

          rabbitTemplate.convertAndSend(actionCaseExchange, "", printCaseSelected);
        });
  }

  private void executeFieldCases(Stream<Case> cases, ActionRule triggeredActionRule) {
    List<Callable<uk.gov.ons.census.action.model.dto.instruction.field.ActionInstruction>>
        callables = new LinkedList<>();
    cases.forEach(
        caze -> {
          callables.add(
              () ->
                  actionInstructionBuilder.buildFieldActionInstruction(caze, triggeredActionRule));
        });

    try {
      final String routingKey = getRoutingKey(triggeredActionRule);

      List<Future<uk.gov.ons.census.action.model.dto.instruction.field.ActionInstruction>> results =
          EXECUTOR_SERVICE.invokeAll(callables);

      log.info("About to send {} ActionInstruction messages", results.size());
      int messagesSent = 0;
      for (Future<uk.gov.ons.census.action.model.dto.instruction.field.ActionInstruction> result :
          results) {
        if (messagesSent++ % 1000 == 0) {
          log.info("Sent {} ActionInstruction messages", messagesSent - 1);
        }

        rabbitFieldTemplate.convertAndSend(outboundExchange, routingKey, result.get());
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e); // Roll the whole transaction back
    }
  }

  private Specification<Case> createSpecificationForUnreceiptedCases(String actionPlanId) {
    return where(isActionPlanIdEqualTo(actionPlanId)).and(excludeReceiptedCases());
  }

  private Specification<Case> isActionPlanIdEqualTo(String actionPlanId) {
    return (Specification<Case>)
        (root, query, builder) -> builder.equal(root.get("actionPlanId"), actionPlanId);
  }

  private Specification<Case> excludeReceiptedCases() {
    return (Specification<Case>)
        (root, query, builder) -> builder.equal(root.get("receiptReceived"), false);
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
          put("ICHHQE", "P_IC_H1");
          put("ICHHQW", "P_IC_H2");
          put("ICHHQN", "P_IC_H4");
          put("ICL1E", "P_IC_ICL1");
          put("ICL2W", "P_IC_ICL2");
          put("ICL4N", "P_IC_ICL4");
        }
      };
}
