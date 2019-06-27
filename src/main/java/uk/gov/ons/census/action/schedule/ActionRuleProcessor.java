package uk.gov.ons.census.action.schedule;

import static org.springframework.data.jpa.domain.Specification.where;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.time.OffsetDateTime;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.instruction.printer.ActionInstruction;
import uk.gov.ons.census.action.model.entity.ActionHandler;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;
import uk.gov.ons.census.action.model.repository.CaseRepository;

@Component
public class ActionRuleProcessor {
  private static final Logger log = LoggerFactory.getLogger(ActionRuleScheduler.class);
  private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(50);
  private static final String ROUTING_KEY_PREFIX = "Action.";
  private static final String ROUTING_KEY_SUFFIX = ".binding";

  private final ActionRuleRepository actionRuleRepo;
  private final CaseRepository caseRepository;
  private final ActionInstructionBuilder actionInstructionBuilder;
  private final PrintFileDtoBuilder printFileDtoBuilder;
  private final RabbitTemplate rabbitTemplate;
  private final RabbitTemplate rabbitFieldTemplate;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  public ActionRuleProcessor(
      ActionRuleRepository actionRuleRepo,
      CaseRepository caseRepository,
      ActionInstructionBuilder actionInstructionBuilder,
      PrintFileDtoBuilder printFileDtoBuilder,
      RabbitTemplate rabbitTemplate,
      @Qualifier("actionInstructionFieldRabbitTemplate") RabbitTemplate rabbitFieldTemplate) {
    this.actionRuleRepo = actionRuleRepo;
    this.caseRepository = caseRepository;
    this.actionInstructionBuilder = actionInstructionBuilder;
    this.printFileDtoBuilder = printFileDtoBuilder;
    this.rabbitTemplate = rabbitTemplate;
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
    if (triggeredActionRule.getClassifiers() == null
        || triggeredActionRule.getClassifiers().isEmpty()) {
      executeAllCases(triggeredActionRule);
    } else {
      executeClassifiedCases(triggeredActionRule);
    }
  }

  private void executeAllCases(ActionRule triggeredActionRule) {
    String actionPlanId = triggeredActionRule.getActionPlan().getId().toString();

    try (Stream<Case> stream =
        caseRepository.findByActionPlanIdAndReceiptReceivedIsFalse(actionPlanId)) {
      executeCases(stream, triggeredActionRule);
    }
  }

  private void executeClassifiedCases(ActionRule triggeredActionRule) {
    String actionPlanId = triggeredActionRule.getActionPlan().getId().toString();

    Specification<Case> specification = createSpecificationForUnreceiptedCases(actionPlanId);

    for (Map.Entry<String, List<String>> classifier :
        triggeredActionRule.getClassifiers().entrySet()) {
      specification = specification.and(isClassifierIn(classifier.getKey(), classifier.getValue()));
    }

    try (Stream<Case> cases = caseRepository.findAll(specification).stream()) {
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

  private void executePrinterCases(Stream<Case> cases, ActionRule triggeredActionRule) {
    UUID batchId = UUID.randomUUID();

//  TODO: Count terminates the Stream...  how to do this nicely?
    //    long batchQty = cases.count();.  you could copy the Steam, or query the database for a count
    long batchQty = 10L;

    List<Callable<PrintFileDto>> callables = new LinkedList<>();
    cases.forEach(
        caze -> {
          callables.add(
              () ->
                  printFileDtoBuilder.buildPrintFileDto(caze, triggeredActionRule, batchQty, batchId)
          );

//
//                  actionInstructionBuilder.buildPrinterActionInstruction(
//                      caze, triggeredActionRule));
        });

    try {
      final String routingKey =
          String.format(
              "%s%s%s",
              ROUTING_KEY_PREFIX,
              triggeredActionRule.getActionType().getHandler().getRoutingKey(),
              ROUTING_KEY_SUFFIX);

      List<Future<PrintFileDto>> results = EXECUTOR_SERVICE.invokeAll(callables);

      log.info("About to send {} ActionInstruction messages", results.size());
      int messagesSent = 0;
      for (Future<PrintFileDto> result : results) {
        if (messagesSent++ % 1000 == 0) {
          log.info("Sent {} ActionInstruction messages", messagesSent - 1);
        }

        rabbitTemplate.convertAndSend(outboundExchange, routingKey, result.get());
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(); // Roll the whole transaction back
    }
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
      final String routingKey =
          String.format(
              "%s%s%s",
              ROUTING_KEY_PREFIX,
              triggeredActionRule.getActionType().getHandler().getRoutingKey(),
              ROUTING_KEY_SUFFIX);

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
}
