package uk.gov.ons.census.action.messaging;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.service.FulfilmentRequestService;

@MessageEndpoint
public class FulfilmentRequestReceiver {
  private static final Logger log = LoggerFactory.getLogger(FulfilmentRequestReceiver.class);
  private static final Set<String> individualResponseRequestCodes =
      new HashSet<>(Arrays.asList("P_OR_I1", "P_OR_I2", "P_OR_I2W", "P_OR_I4"));
  private final CaseRepository caseRepository;
  private final FulfilmentRequestService fulfilmentRequestService;

  public FulfilmentRequestReceiver(
      CaseRepository caseRepository, FulfilmentRequestService fulfilmentRequestService) {
    this.caseRepository = caseRepository;
    this.fulfilmentRequestService = fulfilmentRequestService;
  }

  @Transactional
  @ServiceActivator(inputChannel = "actionFulfilmentInputChannel")
  public void receiveEvent(ResponseManagementEvent event) {
    String fulfilmentCode = event.getPayload().getFulfilmentRequest().getFulfilmentCode();

    if (individualResponseRequestCodes.contains(fulfilmentCode)) {
      // We can't process this message until the case has been cloned from its parent case.
      // We will receive an 'enriched' case creation message including the fulfilment details
      // from Case Processor.
      return;
    }

    ActionType actionType = fulfilmentRequestService.determineActionType(fulfilmentCode);
    if (actionType == null) {
      return; // This is not a fulfilment that we need to process
    }

    Case fulfilmentCase =
        fetchFulfilmentCase(event.getPayload().getFulfilmentRequest().getCaseId());

    fulfilmentRequestService.processEvent(
        event.getPayload().getFulfilmentRequest(), fulfilmentCase, actionType);
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
}
