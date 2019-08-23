package uk.gov.ons.census.action.messaging;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.dto.CollectionCase;
import uk.gov.ons.census.action.model.dto.EventType;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.dto.Uac;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.CaseState;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

@MessageEndpoint
public class EventReceiver {
  private static final Logger log = LoggerFactory.getLogger(EventReceiver.class);
  private static final String CASE_NOT_FOUND_ERROR = "Failed to find case by case id '%s'";

  private final CaseRepository caseRepository;
  private final UacQidLinkRepository uacQidLinkRepository;

  public EventReceiver(CaseRepository caseRepository, UacQidLinkRepository uacQidLinkRepository) {
    this.caseRepository = caseRepository;
    this.uacQidLinkRepository = uacQidLinkRepository;
  }

  @Transactional
  @ServiceActivator(inputChannel = "caseCreatedInputChannel")
  public void receiveEvent(ResponseManagementEvent responseManagementEvent) {
    if (responseManagementEvent.getEvent().getType() == EventType.CASE_CREATED) {
      processCaseCreatedEvent(responseManagementEvent.getPayload().getCollectionCase());
    } else if (responseManagementEvent.getEvent().getType() == EventType.UAC_UPDATED) {
      processUacUpdated(responseManagementEvent.getPayload().getUac());
    } else if (responseManagementEvent.getEvent().getType() == EventType.CASE_UPDATED) {
      processCaseUpdatedEvent(responseManagementEvent.getPayload().getCollectionCase());
    } else {
      // This code can't be reached because under the class structure the EventType is limited to
      // enums at this point?
      throw new RuntimeException(); // Unexpected event type - maybe throw away?
    }
  }

  private void processCaseCreatedEvent(CollectionCase collectionCase) {
    Case newCase = new Case();
    setCaseDetails(collectionCase, newCase);
    caseRepository.save(newCase);
  }

  private void processCaseUpdatedEvent(CollectionCase collectionCase) {
    String caseId = collectionCase.getId();

    Optional<Case> cazeOpt = caseRepository.findByCaseId(UUID.fromString(caseId));

    if (cazeOpt.isEmpty()) {
      log.error(String.format(CASE_NOT_FOUND_ERROR, caseId));
      throw new RuntimeException();
    }

    Case updatedCase = cazeOpt.get();
    setCaseDetails(collectionCase, updatedCase);
    caseRepository.save(updatedCase);
  }

  private void setCaseDetails(CollectionCase collectionCase, Case caseDetails) {
    caseDetails.setCaseRef(Integer.parseInt(collectionCase.getCaseRef()));
    caseDetails.setCaseId(UUID.fromString(collectionCase.getId()));
    caseDetails.setCaseRef(Integer.parseInt(collectionCase.getCaseRef()));
    caseDetails.setCaseId(UUID.fromString(collectionCase.getId()));
    caseDetails.setState(CaseState.valueOf(collectionCase.getState()));
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

    // Below this line is extra data potentially needed by Action Scheduler - can be ignored by RH
    caseDetails.setActionPlanId(collectionCase.getActionPlanId()); // This is essential
    caseDetails.setTreatmentCode(collectionCase.getTreatmentCode()); // This is essential
    caseDetails.setAddressLevel(collectionCase.getAddress().getAddressLevel());
    caseDetails.setAbpCode(collectionCase.getAddress().getApbCode());
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
    caseDetails.setReceiptReceived(collectionCase.getReceiptReceived());
    caseDetails.setRefusalReceived(collectionCase.getRefusalReceived());
    caseDetails.setAddressInvalid(collectionCase.getAddressInvalid());
  }

  private void processUacUpdated(Uac uac) {
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setId(UUID.randomUUID());
    uacQidLink.setQid(uac.getQuestionnaireId());
    uacQidLink.setUac(uac.getUac());
    uacQidLink.setCaseId(uac.getCaseId());
    uacQidLink.setActive(uac.isActive());
    uacQidLinkRepository.save(uacQidLink);
  }
}
