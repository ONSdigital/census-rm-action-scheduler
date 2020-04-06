package uk.gov.ons.census.action.service;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.action.builders.CaseSelectedBuilder;
import uk.gov.ons.census.action.client.CaseClient;
import uk.gov.ons.census.action.messaging.FulfilmentRequestReceiver;
import uk.gov.ons.census.action.model.dto.FulfilmentRequestDTO;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.dto.UacQidDTO;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.FulfilmentToSend;
import uk.gov.ons.census.action.model.repository.FulfilmentToSendRepository;

@Service
public class FulfilmentRequestService {
  private static final Logger log = LoggerFactory.getLogger(FulfilmentRequestReceiver.class);
  private final RabbitTemplate rabbitTemplate;
  private final CaseClient caseClient;
  private final CaseSelectedBuilder caseSelectedBuilder;
  private final FulfilmentToSendRepository fulfilmentToSendRepository;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  @Value("${queueconfig.action-case-exchange}")
  private String actionCaseExchange;

  public FulfilmentRequestService(
      RabbitTemplate rabbitTemplate,
      CaseClient caseClient,
      CaseSelectedBuilder caseSelectedBuilder,
      FulfilmentToSendRepository fulfilmentToSendRepository) {
    this.rabbitTemplate = rabbitTemplate;
    this.caseClient = caseClient;
    this.caseSelectedBuilder = caseSelectedBuilder;
    this.fulfilmentToSendRepository = fulfilmentToSendRepository;
  }

  public void processEvent(
      FulfilmentRequestDTO fulfilmentRequest, Case caze, ActionType actionType) {

    checkMandatoryFields(fulfilmentRequest, caze);

    PrintFileDto printFileDto = createAndPopulatePrintFileDto(caze, actionType, fulfilmentRequest);

    ResponseManagementEvent printCaseSelected =
        caseSelectedBuilder.buildPrintMessage(printFileDto, null);

    rabbitTemplate.convertAndSend(actionCaseExchange, "", printCaseSelected);

    sendFulfilmentToTable(printFileDto, fulfilmentRequest);
  }

  // TODO check list of codes
  private static final Set<String> paperQuestionnaireFulfilmentCodes =
      Set.of(
          "P_OR_H1",
          "P_OR_H2",
          "P_OR_H2W",
          "P_OR_H4",
          "P_OR_HC1",
          "P_OR_HC2",
          "P_OR_HC2W",
          "P_OR_HC4");

  private void checkMandatoryFields(FulfilmentRequestDTO fulfilmentRequest, Case caze) {
    Map<String, Object> mandatoryValues = new HashMap<>();
    mandatoryValues.put("addressLine1", caze.getAddressLine1());
    mandatoryValues.put("postcode", caze.getPostcode());
    mandatoryValues.put("townName", caze.getTownName());

    if (isPaperQuestionnaireFulfilment(fulfilmentRequest) && caze.isHandDelivery()) {
      // Non PQ fulfilments which are hand delivered need a field officer and coordinator
      mandatoryValues.put("fieldCoordinatorId", caze.getFieldCoordinatorId());
      mandatoryValues.put("fieldOfficerId", caze.getFieldOfficerId());
    }

    List<String> missingFields = new ArrayList<>();
    for (Map.Entry<String, Object> entry : mandatoryValues.entrySet()) {
      if (entry.getValue() == null) {
        missingFields.add(entry.getKey());
      }
    }
    if (!missingFields.isEmpty()) {
      throw new RuntimeException(
          String.format(
              "Received fulfilment request for case which is missing mandatory values: %s, fulfilmentCode: %s, caseId: %s",
              missingFields.toString(), fulfilmentRequest.getFulfilmentCode(), caze.getCaseId()));
    }
  }

  private boolean isPaperQuestionnaireFulfilment(FulfilmentRequestDTO fulfilmentRequest) {
    return paperQuestionnaireFulfilmentCodes.contains(fulfilmentRequest.getFulfilmentCode());
  }

  public void sendFulfilmentToTable(
      PrintFileDto printFileDto, FulfilmentRequestDTO fulfilmentRequestDTO) {
    FulfilmentToSend fulfilmentToSend = new FulfilmentToSend();
    fulfilmentToSend.setFulfilmentCode(fulfilmentRequestDTO.getFulfilmentCode());
    fulfilmentToSend.setMessageData(printFileDto);

    fulfilmentToSendRepository.saveAndFlush(fulfilmentToSend);
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
      case "P_TB_TBLIT4":
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
      case "RM_TC_HI":
      case "RM_TC":
        return null; // Ignore SMS and RM internal fulfilments
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
    printFileDto.setPackCode(fulfilmentRequest.getFulfilmentCode());
    printFileDto.setActionType(actionType.name());
    printFileDto.setCaseRef(fulfilmentCase.getCaseRef());

    Optional<String> questionnaireType =
        determineQuestionnaireType(fulfilmentRequest.getFulfilmentCode());

    if (questionnaireType.isPresent()) {
      UacQidDTO uacQid = caseClient.getUacQid(fulfilmentCase.getCaseId(), questionnaireType.get());
      printFileDto.setQid(uacQid.getQid());
      printFileDto.setUac(uacQid.getUac());
    }
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
