package uk.gov.ons.census.action.messaging;

import java.util.Optional;
import java.util.UUID;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.dto.CollectionCase;
import uk.gov.ons.census.action.model.dto.EventType;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.dto.Uac;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;
import uk.gov.ons.census.action.service.FulfilmentRequestService;
import uk.gov.ons.census.action.utility.QuestionnaireTypeHelper;

@MessageEndpoint
public class CaseAndUacReceiver {
  private static final String CASE_NOT_FOUND_ERROR = "Failed to find case by case id '%s'";

  private final CaseRepository caseRepository;
  private final UacQidLinkRepository uacQidLinkRepository;
  private final FulfilmentRequestService fulfilmentRequestService;

  public CaseAndUacReceiver(
      CaseRepository caseRepository,
      UacQidLinkRepository uacQidLinkRepository,
      FulfilmentRequestService fulfilmentRequestService) {
    this.caseRepository = caseRepository;
    this.uacQidLinkRepository = uacQidLinkRepository;
    this.fulfilmentRequestService = fulfilmentRequestService;
  }

  @Transactional
  @ServiceActivator(inputChannel = "caseCreatedInputChannel")
  public void receiveEvent(ResponseManagementEvent responseManagementEvent) {

    EventType eventType = responseManagementEvent.getEvent().getType();

    // Action scheduler can ignore CCS cases and just acknowledge the message
    if (isCCSCase(responseManagementEvent, eventType)) return;

    if (eventType == EventType.CASE_CREATED) {

      Case caze = processCaseCreatedEvent(responseManagementEvent.getPayload().getCollectionCase());

      // We can get sent a fulfilment request along with the case, which we need to process
      if (responseManagementEvent.getPayload().getFulfilmentRequest() != null) {
        ActionType actionType =
            fulfilmentRequestService.determineActionType(
                responseManagementEvent.getPayload().getFulfilmentRequest().getFulfilmentCode());

        fulfilmentRequestService.processEvent(
            responseManagementEvent.getPayload().getFulfilmentRequest(), caze, actionType);
      }
      return;
    }

    if (eventType == EventType.UAC_UPDATED) {
      processUacUpdated(responseManagementEvent.getPayload().getUac());
      return;
    }

    if (eventType == EventType.CASE_UPDATED) {
      processCaseUpdatedEvent(responseManagementEvent.getPayload().getCollectionCase());
      return;
    }

    throw new RuntimeException(String.format("Unexpected event type '%s'", eventType));
  }

  private boolean isCCSCase(ResponseManagementEvent responseManagementEvent, EventType eventType) {

    if (eventType == EventType.CASE_CREATED || eventType == EventType.CASE_UPDATED) {
      return responseManagementEvent.getPayload().getCollectionCase().getSurvey().equals("CCS");
    } else if (eventType == EventType.UAC_UPDATED) {
      return QuestionnaireTypeHelper.isCCSQuestionnaireType(
          responseManagementEvent.getPayload().getUac().getQuestionnaireId());
    }

    return false;
  }

  private Case processCaseCreatedEvent(CollectionCase collectionCase) {
    Case newCase = new Case();
    setCaseDetails(collectionCase, newCase);
    return caseRepository.save(newCase);
  }

  private void processCaseUpdatedEvent(CollectionCase collectionCase) {
    String caseId = collectionCase.getId();

    Optional<Case> cazeOpt = caseRepository.findByCaseId(UUID.fromString(caseId));

    if (cazeOpt.isEmpty()) {
      throw new RuntimeException(String.format(CASE_NOT_FOUND_ERROR, caseId));
    }

    Case updatedCase = cazeOpt.get();
    setCaseDetails(collectionCase, updatedCase);
    caseRepository.save(updatedCase);
  }

  private void setCaseDetails(CollectionCase collectionCase, Case caseDetails) {
    caseDetails.setCaseRef(Long.parseLong(collectionCase.getCaseRef()));
    caseDetails.setCaseId(UUID.fromString(collectionCase.getId()));
    caseDetails.setCollectionExerciseId(collectionCase.getCollectionExerciseId());
    caseDetails.setAddressLine1(collectionCase.getAddress().getAddressLine1());
    caseDetails.setAddressLine2(collectionCase.getAddress().getAddressLine2());
    caseDetails.setAddressLine3(collectionCase.getAddress().getAddressLine3());
    caseDetails.setTownName(collectionCase.getAddress().getTownName());
    caseDetails.setPostcode(collectionCase.getAddress().getPostcode());
    caseDetails.setArid(collectionCase.getAddress().getArid());
    caseDetails.setLatitude(collectionCase.getAddress().getLatitude());
    caseDetails.setLongitude(collectionCase.getAddress().getLongitude());
    caseDetails.setUprn(collectionCase.getAddress().getUprn());
    caseDetails.setRegion(collectionCase.getAddress().getRegion());
    // Nope don't add new stuff here... look at the comment below...

    // Below this line is extra data potentially needed by Action Scheduler - can be ignored by RH
    caseDetails.setActionPlanId(collectionCase.getActionPlanId()); // This is essential
    caseDetails.setTreatmentCode(collectionCase.getTreatmentCode()); // This is essential
    caseDetails.setAddressLevel(collectionCase.getAddress().getAddressLevel());
    caseDetails.setAbpCode(collectionCase.getAddress().getApbCode());
    caseDetails.setCaseType(collectionCase.getCaseType());
    caseDetails.setAddressType(collectionCase.getAddress().getAddressType());
    caseDetails.setUprn(collectionCase.getAddress().getUprn());
    caseDetails.setEstabArid(collectionCase.getAddress().getEstabArid());
    caseDetails.setEstabType(collectionCase.getAddress().getEstabType());
    caseDetails.setOrganisationName(collectionCase.getAddress().getOrganisationName());
    caseDetails.setOa(collectionCase.getOa());
    caseDetails.setLsoa(collectionCase.getLsoa());
    caseDetails.setMsoa(collectionCase.getMsoa());
    caseDetails.setLad(collectionCase.getLad());
    caseDetails.setHtcWillingness(collectionCase.getHtcWillingness());
    caseDetails.setHtcDigital(collectionCase.getHtcDigital());
    caseDetails.setFieldCoordinatorId(collectionCase.getFieldCoordinatorId());
    caseDetails.setFieldOfficerId(collectionCase.getFieldOfficerId());
    caseDetails.setCeExpectedCapacity(collectionCase.getCeExpectedCapacity());
    caseDetails.setCeActualResponses(collectionCase.getCeActualResponses());
    caseDetails.setReceiptReceived(collectionCase.getReceiptReceived());
    caseDetails.setRefusalReceived(collectionCase.getRefusalReceived());
    caseDetails.setAddressInvalid(collectionCase.getAddressInvalid());
    caseDetails.setUndeliveredAsAddressed(collectionCase.getUndeliveredAsAddressed());
    caseDetails.setHandDelivery(collectionCase.isHandDelivery());
    caseDetails.setMetadata(collectionCase.getMetadata());
    caseDetails.setSkeleton(collectionCase.isSkeleton());
    // Yep. Here is a good place to add new stuff.
  }

  private void processUacUpdated(Uac uac) {
    Optional<UacQidLink> uacQidLinkOpt = uacQidLinkRepository.findByQid(uac.getQuestionnaireId());

    UacQidLink uacQidLink;
    if (uacQidLinkOpt.isEmpty()) {
      uacQidLink = new UacQidLink();
      uacQidLink.setId(UUID.randomUUID());
      uacQidLink.setQid(uac.getQuestionnaireId());
      uacQidLink.setUac(uac.getUac());
    } else {
      uacQidLink = uacQidLinkOpt.get();
    }

    uacQidLink.setCaseId(uac.getCaseId());
    uacQidLink.setActive(uac.isActive());
    uacQidLinkRepository.save(uacQidLink);
  }
}
