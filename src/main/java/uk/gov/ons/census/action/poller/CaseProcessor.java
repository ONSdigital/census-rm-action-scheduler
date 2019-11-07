package uk.gov.ons.census.action.poller;

import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.builders.CaseSelectedBuilder;
import uk.gov.ons.census.action.builders.FieldworkFollowupBuilder;
import uk.gov.ons.census.action.builders.PrintFileDtoBuilder;
import uk.gov.ons.census.action.model.dto.FieldworkFollowup;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.entity.ActionHandler;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.CaseToProcess;

@Component
public class CaseProcessor {
  private final FieldworkFollowupBuilder fieldworkFollowupBuilder;
  private final PrintFileDtoBuilder printFileDtoBuilder;
  private final CaseSelectedBuilder caseSelectedBuilder;
  private final RabbitTemplate rabbitTemplate;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  @Value("${queueconfig.action-case-exchange}")
  private String actionCaseExchange;

  public CaseProcessor(
      FieldworkFollowupBuilder fieldworkFollowupBuilder,
      PrintFileDtoBuilder printFileDtoBuilder,
      CaseSelectedBuilder caseSelectedBuilder,
      RabbitTemplate rabbitTemplate) {
    this.fieldworkFollowupBuilder = fieldworkFollowupBuilder;
    this.printFileDtoBuilder = printFileDtoBuilder;
    this.caseSelectedBuilder = caseSelectedBuilder;
    this.rabbitTemplate = rabbitTemplate;
  }

  public void process(CaseToProcess caseToProcess) {
    ActionRule triggeredActionRule = caseToProcess.getActionRule();

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

    PrintFileDto printFileDto =
        printFileDtoBuilder.buildPrintFileDto(
            caseToProcess.getCaze(),
            triggeredActionRule.getActionType().getPackCode(),
            batchId,
            triggeredActionRule.getActionType());
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
}
