package uk.gov.ons.census.action.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.action.builders.CaseSelectedBuilder;
import uk.gov.ons.census.action.client.CaseClient;
import uk.gov.ons.census.action.messaging.FulfilmentRequestReceiver;
import uk.gov.ons.census.action.model.dto.FulfilmentRequestDTO;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.dto.UacQidDTO;
import uk.gov.ons.census.action.model.entity.ActionHandler;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;

import static uk.gov.ons.census.action.utility.JsonHelper.convertObjectToJson;

@Service
public class FulfilmentRequestService {
  private static final Logger log = LoggerFactory.getLogger(FulfilmentRequestReceiver.class);
  private final RabbitTemplate rabbitTemplate;
  private final CaseClient caseClient;
  private final CaseSelectedBuilder caseSelectedBuilder;
  private final JdbcTemplate jdbcTemplate;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  @Value("${queueconfig.action-case-exchange}")
  private String actionCaseExchange;

  public FulfilmentRequestService(
          RabbitTemplate rabbitTemplate,
          CaseClient caseClient,
          CaseSelectedBuilder caseSelectedBuilder,
          JdbcTemplate jdbcTemplate) {
    this.rabbitTemplate = rabbitTemplate;
    this.caseClient = caseClient;
    this.caseSelectedBuilder = caseSelectedBuilder;
    this.jdbcTemplate = jdbcTemplate;
  }

  public void processEvent(
          FulfilmentRequestDTO fulfilmentRequest, Case caze, ActionType actionType) {
    String fulfilmentCode = fulfilmentRequest.getFulfilmentCode();

    PrintFileDto printFileDto = createAndPopulatePrintFileDto(caze, actionType, fulfilmentRequest);

    Optional<String> questionnaireType = determineQuestionnaireType(fulfilmentCode);

    if (questionnaireType.isPresent()) {
      UacQidDTO uacQid = caseClient.getUacQid(caze.getCaseId(), questionnaireType.get());
      printFileDto.setQid(uacQid.getQid());
      printFileDto.setUac(uacQid.getUac());
    }

    ResponseManagementEvent printCaseSelected =
            caseSelectedBuilder.buildPrintMessage(printFileDto, null);


    String printFileString = convertObjectToJson(printFileDto);

    sendFulfilmentsToTable(printFileString, fulfilmentRequest);
    rabbitTemplate.convertAndSend(actionCaseExchange, "", printCaseSelected);

    rabbitTemplate.convertAndSend(
        outboundExchange, ActionHandler.PRINTER.getRoutingKey(), printFileDto);
  }


  public void sendFulfilmentsToTable(String printFileDto,  FulfilmentRequestDTO fulfilmentRequestDTO) {
    jdbcTemplate.update(
            "INSERT INTO actionv2.fulfilments_to_send(message_data, fulfilment_code) VALUES(?::json, ?)",
            printFileDto, fulfilmentRequestDTO.getFulfilmentCode());
  }

  public ActionType determineActionType(String fulfilmentCode) {

    // These are currently not added as Enums, as not known.
    switch (fulfilmentCode) {
      case "P_OR_H1":
      case "P_OR_H2":
      case "P_OR_H2W":
      case "P_OR_H4":
      case "P_OR_HC1":
      case "P_OR_HC2":
      case "P_OR_HC2W":
      case "P_OR_HC4":
        return ActionType.P_OR_HX;
      case "P_LP_HL1":
      case "P_LP_HL2":
      case "P_LP_HL2W":
      case "P_LP_HL4":
        return ActionType.P_LP_HLX;
      case "P_TB_TBARA1":
      case "P_TB_TBBEN1":
      case "P_TB_TBCAN1":
      case "P_TB_TBCAN4":
      case "P_TB_TBFRE1":
      case "P_TB_TBGUJ1":
      case "P_TB_TBGUR1":
      case "P_TB_TBIRI4":
      case "P_TB_TBITA1":
      case "P_TB_TBKUR1":
      case "P_TB_TBLIT1":
      case "T_PB_TBLIT4":
      case "P_TB_TBMAN1":
      case "P_TB_TBMAN4":
      case "P_TB_TBPOL1":
      case "P_TB_TBPOL4":
      case "P_TB_TBPOR1":
      case "P_TB_TBRUS1":
      case "P_TB_TBURD1":
      case "P_TB_TBSOM1":
      case "P_TB_TBSPA1":
      case "P_TB_TBTUR1":
      case "P_TB_TBULS4":
      case "P_TB_TBVIE1":
      case "P_TB_TBYSH1":
        return ActionType.P_TB_TBX;
      case "UACHHT1":
      case "UACHHT2":
      case "UACHHT2W":
      case "UACHHT4":
      case "UACIT1":
      case "UACIT2":
      case "UACIT2W":
      case "UACIT4":
        return null; // Ignore SMS fulfilments
      case "P_OR_I1":
      case "P_OR_I2":
      case "P_OR_I2W":
      case "P_OR_I4":
        return ActionType.P_OR_IX;
      default:
        log.with("fulfilmentCode", fulfilmentCode).warn("Unexpected fulfilment code received");
        return null;
    }
  }

  private PrintFileDto createAndPopulatePrintFileDto(
          Case fulfilmentCase, ActionType actionType, FulfilmentRequestDTO fulfilmentRequest) {
    PrintFileDto printFileDto = new PrintFileDto();
    printFileDto.setAddressLine1(fulfilmentCase.getAddressLine1());
    printFileDto.setAddressLine2(fulfilmentCase.getAddressLine2());
    printFileDto.setAddressLine3(fulfilmentCase.getAddressLine3());
    printFileDto.setTownName(fulfilmentCase.getTownName());
    printFileDto.setPostcode(fulfilmentCase.getPostcode());
    printFileDto.setTitle(fulfilmentRequest.getContact().getTitle());
    printFileDto.setForename(fulfilmentRequest.getContact().getForename());
    printFileDto.setSurname(fulfilmentRequest.getContact().getSurname());
    printFileDto.setBatchId(UUID.randomUUID().toString());
    printFileDto.setBatchQuantity(1);
    printFileDto.setPackCode(fulfilmentRequest.getFulfilmentCode());
    printFileDto.setActionType(actionType.name());
    printFileDto.setCaseRef(fulfilmentCase.getCaseRef());
    return printFileDto;
  }

  private static final Map<String, String> fulfilmentCodeToQuestionnaireType =
          Map.ofEntries(
                  Map.entry("P_OR_H1", "1"),
                  Map.entry("P_OR_H2", "2"),
                  Map.entry("P_OR_H2W", "3"),
                  Map.entry("P_OR_H4", "4"),
                  Map.entry("P_OR_HC1", "11"),
                  Map.entry("P_OR_HC2", "12"),
                  Map.entry("P_OR_HC2W", "13"),
                  Map.entry("P_OR_HC4", "14"),
                  Map.entry("P_OR_I1", "21"),
                  Map.entry("P_OR_I2", "22"),
                  Map.entry("P_OR_I2W", "23"),
                  Map.entry("P_OR_I4", "24"));

  private Optional<String> determineQuestionnaireType(String packCode) {
    return Optional.ofNullable(fulfilmentCodeToQuestionnaireType.get(packCode));
  }
}
