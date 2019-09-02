package uk.gov.ons.census.action.messaging;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.client.CaseClient;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.dto.UacQidDTO;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.repository.CaseRepository;

@MessageEndpoint
public class FulfilmentRequestReceiver {
  private static final Set<String> individualResponseRequestCodes =
      new HashSet<>(Arrays.asList("P_OR_I1", "P_OR_I2", "P_OR_I2W", "P_OR_I4"));
  private static final Logger log = LoggerFactory.getLogger(FulfilmentRequestReceiver.class);
  private final RabbitTemplate rabbitTemplate;
  private final CaseClient caseClient;
  private final CaseRepository caseRepository;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  @Value("${queueconfig.outbound-printer-routing-key}")
  private String outboundPrinterRoutingKey;

  public FulfilmentRequestReceiver(
      RabbitTemplate rabbitTemplate, CaseClient caseClient, CaseRepository caseRepository) {
    this.rabbitTemplate = rabbitTemplate;
    this.caseClient = caseClient;
    this.caseRepository = caseRepository;
  }

  @Transactional
  @ServiceActivator(inputChannel = "actionFulfilmentInputChannel")
  public void receiveEvent(ResponseManagementEvent event) {
    String fulfilmentCode = event.getPayload().getFulfilmentRequest().getFulfilmentCode();

    Optional<ActionType> actionType = determineActionType(fulfilmentCode);
    if (actionType.isEmpty()) {
      return;
    }

    UUID caseId = event.getPayload().getFulfilmentRequest().getCaseId();

    if (individualResponseRequestCodes.contains(fulfilmentCode)) {
      caseId = UUID.fromString(event.getPayload().getFulfilmentRequest().getIndividualCaseId());

      // Here be dragons

      try {
        /*
        Case processor and action scheduler ingest this message at the same time.
        While case processor is creating the case we expect this to throw an exception
        as the case will probably not exist the first time the case is looked for.
        The sleep will reduce excessive logging.
         */
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new RuntimeException("sleep Exception, really");
      }
    }

    Case fulfilmentCase = fetchFulfilmentCase(caseId);

    PrintFileDto printFileDto =
        createAndPopulatePrintFileDto(fulfilmentCase, actionType.get(), event);

    Optional<String> questionnaireType = determineQuestionnaireType(fulfilmentCode);

    if (questionnaireType.isPresent()) {
      UacQidDTO uacQid = caseClient.getUacQid(caseId, questionnaireType.get());
      printFileDto.setQid(uacQid.getQid());
      printFileDto.setUac(uacQid.getUac());
    }

    rabbitTemplate.convertAndSend(outboundExchange, outboundPrinterRoutingKey, printFileDto);
  }

  private Case fetchFulfilmentCase(UUID caseId) {
    Optional<Case> fulfilmentCase = caseRepository.findByCaseId(caseId);
    if (fulfilmentCase.isEmpty()) {
      log.with("caseId", caseId).error("Cannot find Case for fulfilment request.");
      throw new RuntimeException(
          String.format("Cannot find case %s for fulfilment request.", caseId));
    }
    return fulfilmentCase.get();
  }

  private PrintFileDto createAndPopulatePrintFileDto(
      Case fulfilmentCase, ActionType actionType, ResponseManagementEvent event) {
    PrintFileDto printFileDto = new PrintFileDto();
    printFileDto.setAddressLine1(fulfilmentCase.getAddressLine1());
    printFileDto.setAddressLine2(fulfilmentCase.getAddressLine2());
    printFileDto.setAddressLine3(fulfilmentCase.getAddressLine3());
    printFileDto.setTownName(fulfilmentCase.getTownName());
    printFileDto.setPostcode(fulfilmentCase.getPostcode());
    printFileDto.setTitle(event.getPayload().getFulfilmentRequest().getContact().getTitle());
    printFileDto.setForename(event.getPayload().getFulfilmentRequest().getContact().getForename());
    printFileDto.setSurname(event.getPayload().getFulfilmentRequest().getContact().getSurname());
    printFileDto.setBatchId(UUID.randomUUID().toString());
    printFileDto.setBatchQuantity(1);
    printFileDto.setPackCode(event.getPayload().getFulfilmentRequest().getFulfilmentCode());
    printFileDto.setActionType(actionType.name());
    printFileDto.setCaseRef(fulfilmentCase.getCaseRef());
    return printFileDto;
  }

  private Optional<ActionType> determineActionType(String fulfilmentCode) {

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
        return Optional.of(ActionType.P_OR_HX);
      case "P_LP_HL1":
      case "P_LP_HL2":
      case "P_LP_HL2W":
      case "P_LP_HL4":
        return Optional.of(ActionType.P_LP_HLX);
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
        return Optional.of(ActionType.P_TB_TBX);
      case "UACHHT1":
      case "UACHHT2":
      case "UACHHT2W":
      case "UACHHT4":
      case "UACIT1":
      case "UACIT2":
      case "UACIT2W":
      case "UACIT4":
        return Optional.empty(); // Ignore SMS fulfilments
      case "P_OR_I1":
        return Optional.of(ActionType.P_OR_I1);
      case "P_OR_I2":
      case "P_OR_I2W":
        return Optional.of(ActionType.P_OR_I2);
      case "P_OR_I4":
        return Optional.of(ActionType.P_OR_I4);
      default:
        log.with("fulfilmentCode", fulfilmentCode).warn("Unexpected fulfilment code received");
        return Optional.empty();
    }
  }

  static final Map<String, String> fulfilmentCodeToQuestionnaireType =
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
