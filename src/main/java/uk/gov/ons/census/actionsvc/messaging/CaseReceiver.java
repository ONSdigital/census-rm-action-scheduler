package uk.gov.ons.census.actionsvc.messaging;

import java.util.UUID;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.actionsvc.model.dto.CaseCreatedEvent;
import uk.gov.ons.census.actionsvc.model.entity.Case;
import uk.gov.ons.census.actionsvc.model.repository.CaseRepository;

@MessageEndpoint
public class CaseReceiver {
  private CaseRepository caseRepository;

  public CaseReceiver(CaseRepository caseRepository) {
    this.caseRepository = caseRepository;
  }

  @Transactional
  @ServiceActivator(inputChannel = "caseCreatedInputChannel")
  public void receiveCaseCreatedEvent(CaseCreatedEvent caseCreatedEvent) {
    Case newCase = new Case();
    newCase.setCaseRef(
        Long.parseLong(caseCreatedEvent.getPayload().getCollectionCase().getCaseRef()));
    newCase.setCaseId(UUID.fromString(caseCreatedEvent.getPayload().getCollectionCase().getId()));
    newCase.setActionPlanId(caseCreatedEvent.getPayload().getCollectionCase().getActionPlanId());
    newCase.setTreatmentCode(caseCreatedEvent.getPayload().getCollectionCase().getTreatmentCode());
    newCase.setUac(caseCreatedEvent.getPayload().getCollectionCase().getUac());
    newCase.setAddressLine1(
        caseCreatedEvent.getPayload().getCollectionCase().getAddress().getAddressLine1());
    newCase.setAddressLine2(
        caseCreatedEvent.getPayload().getCollectionCase().getAddress().getAddressLine2());
    newCase.setAddressLine3(
        caseCreatedEvent.getPayload().getCollectionCase().getAddress().getAddressLine3());
    newCase.setTownName(
        caseCreatedEvent.getPayload().getCollectionCase().getAddress().getTownName());
    newCase.setPostcode(
        caseCreatedEvent.getPayload().getCollectionCase().getAddress().getPostcode());
    caseRepository.save(newCase);
  }
}
