package uk.gov.ons.census.action.poller;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.builders.CaseSelectedBuilder;
import uk.gov.ons.census.action.builders.FieldworkFollowupBuilder;
import uk.gov.ons.census.action.builders.PrintFileDtoBuilder;
import uk.gov.ons.census.action.model.dto.FieldworkFollowup;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.entity.ActionHandler;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.CaseToProcess;
import uk.gov.ons.census.action.model.repository.CaseToProcessRepository;

@Component
public class CaseToProcessPoller {
  private static final Logger log = LoggerFactory.getLogger(CaseToProcessPoller.class);

  private final CaseToProcessRepository caseToProcessRepository;
  private final FieldworkFollowupBuilder fieldworkFollowupBuilder;
  private final PrintFileDtoBuilder printFileDtoBuilder;
  private final CaseSelectedBuilder caseSelectedBuilder;
  private final RabbitTemplate rabbitTemplate;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  @Value("${queueconfig.action-case-exchange}")
  private String actionCaseExchange;

  public CaseToProcessPoller(
      CaseToProcessRepository caseToProcessRepository,
      FieldworkFollowupBuilder fieldworkFollowupBuilder,
      PrintFileDtoBuilder printFileDtoBuilder,
      CaseSelectedBuilder caseSelectedBuilder,
      RabbitTemplate rabbitTemplate) {
    this.caseToProcessRepository = caseToProcessRepository;
    this.fieldworkFollowupBuilder = fieldworkFollowupBuilder;
    this.printFileDtoBuilder = printFileDtoBuilder;
    this.caseSelectedBuilder = caseSelectedBuilder;
    this.rabbitTemplate = rabbitTemplate;
  }

  @Scheduled(fixedDelayString = "60000")
  @Transactional
  public void processQueuedCases() {
    do {
      try (Stream<CaseToProcess> cases = caseToProcessRepository.findChunkToProcess(1000)) {
        cases.forEach(
            caseToProcess -> {
              process(caseToProcess);
              caseToProcessRepository.delete(caseToProcess);
            });
      }
    } while (caseToProcessRepository.count() > 0); // Don't go to sleep while there's work to do
  }

  private void process(CaseToProcess caseToProcess) {
    ActionRule triggeredActionRule = caseToProcess.getActionRule();
    Case caze = caseToProcess.getCaze();

    if (triggeredActionRule.getActionType().getHandler() == ActionHandler.PRINTER) {
      executePrinterCase(caseToProcess);
    } else if (triggeredActionRule.getActionType().getHandler() == ActionHandler.FIELD) {
      executeFieldCase(caseToProcess);
    }
  }

  private void executePrinterCase(CaseToProcess caseToProcess) {
    ActionRule triggeredActionRule = caseToProcess.getActionRule();
    UUID batchId = caseToProcess.getBatchId();
    int batchQty = caseToProcess.getBatchQuantity();

    String routingKey = triggeredActionRule.getActionType().getHandler().getRoutingKey();

    final String packCode =
        actionTypeToPackCodeMap.get(triggeredActionRule.getActionType().toString());

    PrintFileDto printFileDto =
        printFileDtoBuilder.buildPrintFileDto(
            caseToProcess.getCaze(), packCode, batchId, triggeredActionRule.getActionType());
    printFileDto.setBatchQuantity(batchQty);

    rabbitTemplate.convertAndSend(outboundExchange, routingKey, printFileDto);

    ResponseManagementEvent printCaseSelected =
        caseSelectedBuilder.buildPrintMessage(printFileDto, triggeredActionRule.getId().toString());

    rabbitTemplate.convertAndSend(actionCaseExchange, "", printCaseSelected);
  }

  private void executeFieldCase(CaseToProcess caseToProcess) {
    ActionRule triggeredActionRule = caseToProcess.getActionRule();

    String routingKey = triggeredActionRule.getActionType().getHandler().getRoutingKey();

    FieldworkFollowup fieldworkFollowup =
        fieldworkFollowupBuilder.buildFieldworkFollowup(
            caseToProcess.getCaze(),
            triggeredActionRule.getActionPlan().getId().toString(),
            triggeredActionRule.getActionType().name());

    rabbitTemplate.convertAndSend(outboundExchange, routingKey, fieldworkFollowup);

    ResponseManagementEvent fieldCaseSelected =
        caseSelectedBuilder.buildFieldMessage(
            fieldworkFollowup.getCaseRef(), triggeredActionRule.getId());

    rabbitTemplate.convertAndSend(actionCaseExchange, "", fieldCaseSelected);
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
          put(ActionType.P_RD_2RL1_1.name(), ActionType.P_RD_2RL1_1.name());
          put(ActionType.P_RD_2RL2B_1.name(), ActionType.P_RD_2RL2B_1.name());
          put(ActionType.P_RD_2RL1_2.name(), ActionType.P_RD_2RL1_2.name());
          put(ActionType.P_RD_2RL2B_2.name(), ActionType.P_RD_2RL2B_2.name());
          put(ActionType.P_RD_2RL1_3.name(), ActionType.P_RD_2RL1_3.name());
          put(ActionType.P_RD_2RL2B_3.name(), ActionType.P_RD_2RL2B_3.name());
        }
      };
}
