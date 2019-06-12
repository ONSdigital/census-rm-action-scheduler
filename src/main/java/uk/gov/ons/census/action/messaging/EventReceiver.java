package uk.gov.ons.census.action.messaging;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.dto.CollectionCase;
import uk.gov.ons.census.action.model.dto.Event;
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
      processCaseUpdatedEvent(responseManagementEvent.getEvent(),
          responseManagementEvent.getPayload().getCollectionCase());
    } else {
      // This code can't be reached because under the class structure the EventType is limited to
      // enums at this point?
      throw new RuntimeException(); // Unexpected event type - maybe throw away?
    }
  }

  private void processCaseCreatedEvent(CollectionCase collectionCase) {
    Case newCase = new Case();
    newCase.setCaseRef(Long.parseLong(collectionCase.getCaseRef()));
    newCase.setCaseId(UUID.fromString(collectionCase.getId()));
    newCase.setState(CaseState.valueOf(collectionCase.getState()));
    newCase.setCollectionExerciseId(collectionCase.getCollectionExerciseId());
    newCase.setAddressLine1(collectionCase.getAddress().getAddressLine1());
    newCase.setAddressLine2(collectionCase.getAddress().getAddressLine2());
    newCase.setAddressLine3(collectionCase.getAddress().getAddressLine3());
    newCase.setTownName(collectionCase.getAddress().getTownName());
    newCase.setPostcode(collectionCase.getAddress().getPostcode());
    newCase.setArid(collectionCase.getAddress().getArid());
    newCase.setLatitude(collectionCase.getAddress().getLatitude());
    newCase.setLongitude(collectionCase.getAddress().getLongitude());
    newCase.setUprn(collectionCase.getAddress().getUprn());
    newCase.setRegion(collectionCase.getAddress().getRegion());

    // Below this line is extra data potentially needed by Action Scheduler - can be ignored by RH
    newCase.setActionPlanId(collectionCase.getActionPlanId()); // This is essential
    newCase.setTreatmentCode(collectionCase.getTreatmentCode()); // This is essential
    newCase.setAddressLevel(collectionCase.getAddress().getAddressLevel());
    newCase.setAbpCode(collectionCase.getAddress().getApbCode());
    newCase.setAddressType(collectionCase.getAddress().getAddressType());
    newCase.setUprn(collectionCase.getAddress().getUprn());
    newCase.setEstabArid(collectionCase.getAddress().getEstabArid());
    newCase.setEstabType(collectionCase.getAddress().getEstabType());
    newCase.setOrganisationName(collectionCase.getAddress().getOrganisationName());
    newCase.setOa(collectionCase.getOa());
    newCase.setLsoa(collectionCase.getLsoa());
    newCase.setMsoa(collectionCase.getMsoa());
    newCase.setLad(collectionCase.getLad());
    newCase.setHtcWillingness(collectionCase.getHtcWillingness());
    newCase.setHtcDigital(collectionCase.getHtcDigital());
    newCase.setFieldCoordinatorId(collectionCase.getFieldCoordinatorId());
    newCase.setFieldOfficerId(collectionCase.getFieldOfficerId());
    newCase.setCeExpectedCapacity(collectionCase.getCeExpectedCapacity());
    newCase.setReceiptReceived(false);

    caseRepository.save(newCase);
  }

  private void processCaseUpdatedEvent(Event event, CollectionCase collectionCase) {
    String caseId = collectionCase.getId();

    Optional<Case> cazeOpt = caseRepository.findByCaseId(UUID.fromString(caseId));

    if (cazeOpt.isEmpty()) {
      log.error(String.format(CASE_NOT_FOUND_ERROR, caseId));
      throw new RuntimeException();
    }

    Case updatedCase = cazeOpt.get();
    updatedCase.setCaseId(UUID.fromString(caseId));
    updatedCase.setState(CaseState.valueOf(collectionCase.getState()));
    updatedCase.setCollectionExerciseId(collectionCase.getCollectionExerciseId());
    updatedCase.setAddressLine1(collectionCase.getAddress().getAddressLine1());
    updatedCase.setAddressLine2(collectionCase.getAddress().getAddressLine2());
    updatedCase.setAddressLine3(collectionCase.getAddress().getAddressLine3());
    updatedCase.setTownName(collectionCase.getAddress().getTownName());
    updatedCase.setPostcode(collectionCase.getAddress().getPostcode());
    updatedCase.setArid(collectionCase.getAddress().getArid());
    updatedCase.setLatitude(collectionCase.getAddress().getLatitude());
    updatedCase.setLongitude(collectionCase.getAddress().getLongitude());
    updatedCase.setUprn(collectionCase.getAddress().getUprn());
    updatedCase.setRegion(collectionCase.getAddress().getRegion());
    updatedCase.setActionPlanId(collectionCase.getActionPlanId()); // This is essential
    updatedCase.setTreatmentCode(collectionCase.getTreatmentCode()); // This is essential
    updatedCase.setAddressLevel(collectionCase.getAddress().getAddressLevel());
    updatedCase.setAbpCode(collectionCase.getAddress().getApbCode());
    updatedCase.setAddressType(collectionCase.getAddress().getAddressType());
    updatedCase.setUprn(collectionCase.getAddress().getUprn());
    updatedCase.setEstabArid(collectionCase.getAddress().getEstabArid());
    updatedCase.setEstabType(collectionCase.getAddress().getEstabType());
    updatedCase.setOrganisationName(collectionCase.getAddress().getOrganisationName());
    updatedCase.setOa(collectionCase.getOa());
    updatedCase.setLsoa(collectionCase.getLsoa());
    updatedCase.setMsoa(collectionCase.getMsoa());
    updatedCase.setLad(collectionCase.getLad());
    updatedCase.setHtcWillingness(collectionCase.getHtcWillingness());
    updatedCase.setHtcDigital(collectionCase.getHtcDigital());
    updatedCase.setFieldCoordinatorId(collectionCase.getFieldCoordinatorId());
    updatedCase.setFieldOfficerId(collectionCase.getFieldOfficerId());
    updatedCase.setCeExpectedCapacity(collectionCase.getCeExpectedCapacity());

    updatedCase.setReceiptReceived(event.isReceiptReceived());

    caseRepository.save(updatedCase);
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
