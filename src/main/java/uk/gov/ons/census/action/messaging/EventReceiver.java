package uk.gov.ons.census.action.messaging;

import java.util.List;
import java.util.UUID;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.dto.CollectionCase;
import uk.gov.ons.census.action.model.dto.EventType;
import uk.gov.ons.census.action.model.dto.FanoutEvent;
import uk.gov.ons.census.action.model.dto.Uac;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

@MessageEndpoint
public class EventReceiver {
  private CaseRepository caseRepository;
  private UacQidLinkRepository uacQidLinkRepository;

  public EventReceiver(CaseRepository caseRepository, UacQidLinkRepository uacQidLinkRepository) {
    this.caseRepository = caseRepository;
    this.uacQidLinkRepository = uacQidLinkRepository;
  }

  @Transactional
  @ServiceActivator(inputChannel = "caseCreatedInputChannel")
  public void receiveEvent(FanoutEvent fanoutEvent) {
    if (fanoutEvent.getEvent().getType() == EventType.CASE_CREATED) {
      processCaseCreatedEvent(fanoutEvent.getPayload().getCollectionCase());
    } else if (fanoutEvent.getEvent().getType() == EventType.UAC_UPDATED) {
      processUacUpdated(fanoutEvent.getPayload().getUac());
    } else {
      throw new RuntimeException(); // Unexpected event type - maybe throw away?
    }
  }

  private void processCaseCreatedEvent(CollectionCase collectionCase) {
    Case newCase = new Case();
    newCase.setCaseRef(Long.parseLong(collectionCase.getCaseRef()));
    newCase.setCaseId(UUID.fromString(collectionCase.getId()));
    newCase.setActionPlanId(collectionCase.getActionPlanId());
    newCase.setTreatmentCode(collectionCase.getTreatmentCode());
    //    newCase.setUac(collectionCase.getUac());
    newCase.setAddressLine1(collectionCase.getAddress().getAddressLine1());
    newCase.setAddressLine2(collectionCase.getAddress().getAddressLine2());
    newCase.setAddressLine3(collectionCase.getAddress().getAddressLine3());
    newCase.setTownName(collectionCase.getAddress().getTownName());
    newCase.setPostcode(collectionCase.getAddress().getPostcode());
    newCase = caseRepository.save(newCase);

    // Link any 'dangling' UAC/QID pairs which were waiting to be linked to their parent case
    List<UacQidLink> uacQidLinks = uacQidLinkRepository.findByCaseId(collectionCase.getId());
    for (UacQidLink uacQidLink : uacQidLinks) {
      uacQidLink.setCaze(newCase);
      uacQidLinkRepository.save(uacQidLink);
    }
  }

  private void processUacUpdated(Uac uac) {
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setId(UUID.randomUUID());
    uacQidLink.setQid(uac.getQuestionnaireId());
    uacQidLink.setUac(uac.getUac());
    uacQidLink.setCaseId(uac.getCaseId());

    Case caze = caseRepository.findByCaseId(UUID.fromString(uac.getCaseId()));

    // The case might not have been received/processed yet - messages can be out of sequence.
    // In the event that this entity is not linked to its parent case, that will be handled
    // when the case arrives.
    if (caze != null) {
      uacQidLink.setCaze(caze);
    }

    uacQidLinkRepository.save(uacQidLink);
  }
}
