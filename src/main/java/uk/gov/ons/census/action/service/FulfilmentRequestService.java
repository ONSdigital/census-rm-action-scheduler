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
  private static final Set<String> paperQuestionnaireFulfilmentCodes =
      Set.of(
          "P_OR_H1",
          "P_OR_H2",
          "P_OR_H2W",
          "P_OR_H4",
          "P_OR_HC1",
          "P_OR_HC2",
          "P_OR_HC2W",
          "P_OR_HC4",
          "P_OR_I1",
          "P_OR_I2",
          "P_OR_I2W",
          "P_OR_I4");

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

  private static void checkMandatoryFields(FulfilmentRequestDTO fulfilmentRequest, Case caze) {
    /*
    Throws a RuntimeException if the case does not have the minimum data according to the mandatory fields listed here
    https://collaborate2.ons.gov.uk/confluence/display/SDC/Handle+New+Address+Reported+Events
     */
    Map<String, Object> mandatoryValues = new HashMap<>();
    mandatoryValues.put("addressLine1", caze.getAddressLine1());
    mandatoryValues.put("postcode", caze.getPostcode());
    mandatoryValues.put("townName", caze.getTownName());

    if (!isPaperQuestionnaireFulfilment(fulfilmentRequest) && caze.isHandDelivery()) {
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

  private static boolean isPaperQuestionnaireFulfilment(FulfilmentRequestDTO fulfilmentRequest) {
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
      case "P_UAC_UACHHP1":
      case "P_UAC_UACHHP2B":
      case "P_UAC_UACHHP4":
        return ActionType.P_UAC_HX;
      case "P_LP_HL1":
      case "P_LP_HL2":
      case "P_LP_HL2W":
      case "P_LP_HL4":
        return ActionType.P_LP_HLX;
      case "P_ER_ILER1":
      case "P_ER_ILER2B":
        return ActionType.P_ER_IL;
      case "P_TB_TBALB1":
      case "P_TB_TBAMH1":
      case "P_TB_TBARA1":
      case "P_TB_TBARA2":
      case "P_TB_TBARA4":
      case "P_TB_TBARM1":
      case "P_TB_TBBEN1":
      case "P_TB_TBBEN2":
      case "P_TB_TBBOS1":
      case "P_TB_TBBUL1":
      case "P_TB_TBBUL2":
      case "P_TB_TBBUL4":
      case "P_TB_TBBUR1":
      case "P_TB_TBCAN1":
      case "P_TB_TBCAN2":
      case "P_TB_TBCAN4":
      case "P_TB_TBCZE1":
      case "P_TB_TBCZE4":
      case "P_TB_TBFAR1":
      case "P_TB_TBFAR2":
      case "P_TB_TBFRE1":
      case "P_TB_TBGER1":
      case "P_TB_TBGRE1":
      case "P_TB_TBGRE2":
      case "P_TB_TBGUJ1":
      case "P_TB_TBPAN1":
      case "P_TB_TBPAN2":
      case "P_TB_TBHEB1":
      case "P_TB_TBHIN1":
      case "P_TB_TBHUN1":
      case "P_TB_TBHUN4":
      case "P_TB_TBIRI4":
      case "P_TB_TBITA1":
      case "P_TB_TBITA2":
      case "P_TB_TBJAP1":
      case "P_TB_TBKOR1":
      case "P_TB_TBKUR1":
      case "P_TB_TBKUR2":
      case "P_TB_TBLAT1":
      case "P_TB_TBLAT2":
      case "P_TB_TBLAT4":
      case "P_TB_TBLIN1":
      case "P_TB_TBLIT1":
      case "P_TB_TBLIT4":
      case "P_TB_TBMAL1":
      case "P_TB_TBMAL2":
      case "P_TB_TBMAN1":
      case "P_TB_TBMAN2":
      case "P_TB_TBMAN4":
      case "P_TB_TBNEP1":
      case "P_TB_TBPAS1":
      case "P_TB_TBPAS2":
      case "P_TB_TBPOL1":
      case "P_TB_TBPOL2":
      case "P_TB_TBPOL4":
      case "P_TB_TBPOR1":
      case "P_TB_TBPOR2":
      case "P_TB_TBPOR4":
      case "P_TB_TBPOT1":
      case "P_TB_TBROM1":
      case "P_TB_TBROM4":
      case "P_TB_TBRUS1":
      case "P_TB_TBRUS2":
      case "P_TB_TBRUS4":
      case "P_TB_TBSLE1":
      case "P_TB_TBSLO1":
      case "P_TB_TBSLO4":
      case "P_TB_TBSOM1":
      case "P_TB_TBSOM4":
      case "P_TB_TBSPA1":
      case "P_TB_TBSPA2":
      case "P_TB_TBSWA1":
      case "P_TB_TBSWA2":
      case "P_TB_TBTAG1":
      case "P_TB_TBTAM1":
      case "P_TB_TBTHA1":
      case "P_TB_TBTHA2":
      case "P_TB_TBTET4":
      case "P_TB_TBTIG1":
      case "P_TB_TBTUR1":
      case "P_TB_TBUKR1":
      case "P_TB_TBULS4":
      case "P_TB_TBURD1":
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
      case "P_UAC_UACIP1":
      case "P_UAC_UACIP2B":
      case "P_UAC_UACIP4":
        return ActionType.P_UAC_IX;
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
          Map.entry("P_UAC_UACHHP1", "1"),
          Map.entry("P_OR_H2", "2"),
          Map.entry("P_UAC_UACHHP2B", "2"),
          Map.entry("P_OR_H2W", "3"),
          Map.entry("P_UAC_UACHHP4", "4"),
          Map.entry("P_OR_H4", "4"),
          Map.entry("P_OR_HC1", "11"),
          Map.entry("P_OR_HC2", "12"),
          Map.entry("P_OR_HC2W", "13"),
          Map.entry("P_OR_HC4", "14"),
          Map.entry("P_OR_I1", "21"),
          Map.entry("P_OR_I2", "22"),
          Map.entry("P_OR_I2W", "23"),
          Map.entry("P_OR_I4", "24"),
          Map.entry("P_UAC_UACIP1", "21"),
          Map.entry("P_UAC_UACIP2B", "22"),
          Map.entry("P_UAC_UACIP4", "24"));

  private Optional<String> determineQuestionnaireType(String packCode) {
    return Optional.ofNullable(fulfilmentCodeToQuestionnaireType.get(packCode));
  }
}
