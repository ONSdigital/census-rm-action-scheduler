package uk.gov.ons.census.action.messaging;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.dto.UacQidDTO;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.service.CaseService;

@MessageEndpoint
public class ActionFulfilmentReceiver {
  private static final Logger log = LoggerFactory.getLogger(ActionFulfilmentReceiver.class);
  private final RabbitTemplate rabbitTemplate;
  private final CaseService caseService;
  private final CaseRepository caseRepository;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  @Value("${queueconfig.outbound-printer-routing-key}")
  private String outboundPrinterRoutingKey;

  public ActionFulfilmentReceiver(
      RabbitTemplate rabbitTemplate, CaseService caseService, CaseRepository caseRepository) {
    this.rabbitTemplate = rabbitTemplate;
    this.caseService = caseService;
    this.caseRepository = caseRepository;
  }

  @Transactional
  @ServiceActivator(inputChannel = "actionFulfilmentInputChannel")
  public void receiveEvent(ResponseManagementEvent responseManagementEvent) {
    UUID caseId =
        UUID.fromString(responseManagementEvent.getPayload().getFulfilmentRequest().getCaseId());
    Optional<Case> fulfilmentCase = caseRepository.findByCaseId(caseId);
    log.with("Case ID", caseId).debug("Fulfilment Requested Event");
    if (fulfilmentCase.isEmpty()) {
      log.with("CaseId", caseId).error("Cannot find Case");
      throw new RuntimeException();
    }
    String packCode =
        responseManagementEvent.getPayload().getFulfilmentRequest().getFulfilmentCode();
    Optional<Integer> questionnaireType = determineQuestionnaireType(packCode);
    if (questionnaireType.isEmpty()) {
      log.with("Case Id", caseId).with("Packcode", packCode).error("Unknown packcode");
      throw new RuntimeException();
    }
    UacQidDTO uacQid = caseService.getUacQid("1");
    PrintFileDto printFile =
        populatePrintFileDto(fulfilmentCase.get(), uacQid, responseManagementEvent);
    rabbitTemplate.convertAndSend(outboundExchange, outboundPrinterRoutingKey, printFile);
  }

  private PrintFileDto populatePrintFileDto(
      Case fulfilmentCase, UacQidDTO uacQid, ResponseManagementEvent event) {
    PrintFileDto printFileDto = new PrintFileDto();
    printFileDto.setAddressLine1(fulfilmentCase.getAddressLine1());
    printFileDto.setAddressLine2(fulfilmentCase.getAddressLine2());
    printFileDto.setAddressLine3(fulfilmentCase.getAddressLine3());
    printFileDto.setTownName(fulfilmentCase.getTownName());
    printFileDto.setPostcode(fulfilmentCase.getPostcode());
    printFileDto.setQid(uacQid.getQid());
    printFileDto.setUac(uacQid.getUac());
    printFileDto.setTitle(event.getPayload().getFulfilmentRequest().getContact().getTitle());
    printFileDto.setForename(event.getPayload().getFulfilmentRequest().getContact().getForename());
    printFileDto.setSurname(event.getPayload().getFulfilmentRequest().getContact().getSurname());
    printFileDto.setBatchId(UUID.randomUUID().toString());
    printFileDto.setBatchQuantity(1);
    printFileDto.setPackCode(event.getPayload().getFulfilmentRequest().getFulfilmentCode());
    printFileDto.setActionType(ActionType.P_OR_HX.name());
    return printFileDto;
  }

  private Optional<Integer> determineQuestionnaireType(String packCode) {
    Map<String, Integer> fulfilmentCodeToQuestionnaireType =
        Map.of(
            "P_OR_H1", 1,
            "P_OR_H2", 2,
            "P_OR_H2W", 3,
            "P_OR_H4", 4);
    return Optional.ofNullable(fulfilmentCodeToQuestionnaireType.get(packCode));
  }
}
