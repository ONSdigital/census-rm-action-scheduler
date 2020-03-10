package uk.gov.ons.census.action.messaging;

import java.util.Optional;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uk.gov.ons.census.action.builders.FieldworkFollowupBuilder;
import uk.gov.ons.census.action.model.dto.FieldworkFollowup;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.entity.ActionHandler;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

@MessageEndpoint
public class UndeliveredMailReceiver {
  private final RabbitTemplate rabbitTemplate;
  private final CaseRepository caseRepository;
  private final UacQidLinkRepository uacQidLinkRepository;
  private final FieldworkFollowupBuilder fieldworkFollowupBuilder;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  public UndeliveredMailReceiver(
      RabbitTemplate rabbitTemplate,
      CaseRepository caseRepository,
      UacQidLinkRepository uacQidLinkRepository,
      FieldworkFollowupBuilder fieldworkFollowupBuilder) {
    this.rabbitTemplate = rabbitTemplate;
    this.caseRepository = caseRepository;
    this.uacQidLinkRepository = uacQidLinkRepository;
    this.fieldworkFollowupBuilder = fieldworkFollowupBuilder;
  }

  @Transactional
  @ServiceActivator(inputChannel = "undeliveredMailInputChannel")
  public void receiveMessage(ResponseManagementEvent event) {
    String questionnaireId = event.getPayload().getFulfilmentInformation().getQuestionnaireId();
    Optional<Case> caseOpt;

    if (!StringUtils.isEmpty(questionnaireId)) {
      Optional<UacQidLink> uacQidLinkOpt = uacQidLinkRepository.findByQid(questionnaireId);
      if (uacQidLinkOpt.isEmpty()) {
        throw new RuntimeException(String.format("Can't find by QID %s", questionnaireId));
      }

      caseOpt = caseRepository.findByCaseId(UUID.fromString(uacQidLinkOpt.get().getCaseId()));
    } else {
      long caseRef = Long.parseLong(event.getPayload().getFulfilmentInformation().getCaseRef());
      caseOpt = caseRepository.findById(caseRef);
    }

    if (caseOpt.isEmpty()) {
      throw new RuntimeException(
          String.format(
              "Can't find by Case Ref %s",
              event.getPayload().getFulfilmentInformation().getCaseRef()));
    }

    Case caze = caseOpt.get();

    if (caze.isAddressInvalid()
        || caze.isReceiptReceived()
        || caze.isRefusalReceived()
        || StringUtils.isEmpty(caze.getFieldCoordinatorId())
        || "HI".equals(caze.getCaseType())) {
      return; // We want to ignore this case - don't send to Field
    }

    // FWMT do not need an action plan or action type, and they make no sense in this context
    FieldworkFollowup fieldworkFollowup =
        fieldworkFollowupBuilder.buildFieldworkFollowup(caze, "dummy", "dummy");

    // We need to force override of this, because Action Scheduler's copy of the case probably
    // hasn't been updated yet - Case Processor is handling the message at exactly the same time.
    fieldworkFollowup.setUndeliveredAsAddress(true);

    rabbitTemplate.convertAndSend(
        outboundExchange, ActionHandler.FIELD.getRoutingKey(), fieldworkFollowup);
  }
}
