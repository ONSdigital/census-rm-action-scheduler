package uk.gov.ons.census.action.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.hamcrest.beans.SamePropertyValuesAs;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.ons.census.action.model.dto.CollectionCase;
import uk.gov.ons.census.action.model.dto.EventType;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.dto.Uac;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.CaseState;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

public class EventReceiverTest {
  private CaseRepository caseRepository = mock(CaseRepository.class);
  private UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);

  @Test
  public void testCaseCreated() {
    // given
    EventReceiver eventReceiver = new EventReceiver(caseRepository, uacQidLinkRepository);
    ResponseManagementEvent responseManagementEvent = getResponseManagementEvent();
    responseManagementEvent.getEvent().setType(EventType.CASE_CREATED);

    // when
    eventReceiver.receiveEvent(responseManagementEvent);

    // then
    ArgumentCaptor<Case> eventArgumentCaptor = ArgumentCaptor.forClass(Case.class);
    verify(caseRepository, times(1)).save(eventArgumentCaptor.capture());
    Case actualCase = eventArgumentCaptor.getAllValues().get(0);
    Case expectedCase = getExpectedCase(responseManagementEvent.getPayload().getCollectionCase());

    assertThat(actualCase, SamePropertyValuesAs.samePropertyValuesAs(expectedCase));
  }

  @Test
  public void testCaseUpdate() {
    // given
    EventReceiver eventReceiver = new EventReceiver(caseRepository, uacQidLinkRepository);
    ResponseManagementEvent responseManagementEvent = getResponseManagementEvent();
    responseManagementEvent.getEvent().setType(EventType.UAC_UPDATED);

    // when
    eventReceiver.receiveEvent(responseManagementEvent);

    // then
    ArgumentCaptor<UacQidLink> eventArgumentCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    verify(uacQidLinkRepository, times(1)).save(eventArgumentCaptor.capture());
    UacQidLink actualUacQidLink = eventArgumentCaptor.getAllValues().get(0);

    Uac uac = responseManagementEvent.getPayload().getUac();
    assertEquals(actualUacQidLink.getQid(), uac.getQuestionnaireId());
    assertEquals(actualUacQidLink.getUac(), uac.getUac());
    assertEquals(actualUacQidLink.getCaseId(), uac.getCaseId());
  }

  private ResponseManagementEvent getResponseManagementEvent() {
    EasyRandom easyRandom = new EasyRandom();
    ResponseManagementEvent responseManagementEvent =
        easyRandom.nextObject(ResponseManagementEvent.class);

    responseManagementEvent.getPayload().getCollectionCase().setCaseRef("123");
    responseManagementEvent
        .getPayload()
        .getCollectionCase()
        .setId("d09ac28e-d62f-4cdd-a5f9-e366e05f0fcd");
    responseManagementEvent.getPayload().getCollectionCase().setState("ACTIONABLE");
    return responseManagementEvent;
  }

  private Case getExpectedCase(CollectionCase collectionCase) {
    Case newCase = new Case();
    newCase.setCaseRef(Long.parseLong(collectionCase.getCaseRef()));
    newCase.setCaseId(UUID.fromString(collectionCase.getId()));
    newCase.setActionPlanId(collectionCase.getActionPlanId());
    newCase.setState(CaseState.valueOf(collectionCase.getState()));
    newCase.setTreatmentCode(collectionCase.getTreatmentCode());
    newCase.setAddressLine1(collectionCase.getAddress().getAddressLine1());
    newCase.setAddressLine2(collectionCase.getAddress().getAddressLine2());
    newCase.setAddressLine3(collectionCase.getAddress().getAddressLine3());
    newCase.setTownName(collectionCase.getAddress().getTownName());
    newCase.setPostcode(collectionCase.getAddress().getPostcode());
    newCase.setArid(collectionCase.getAddress().getArid());
    newCase.setLatitude(collectionCase.getAddress().getLatitude());
    newCase.setLongitude(collectionCase.getAddress().getLongitude());
    newCase.setUprn(collectionCase.getAddress().getUprn());
    newCase.setRgn(collectionCase.getAddress().getRegion());
    newCase.setOa(collectionCase.getOa());
    newCase.setLsoa(collectionCase.getLsoa());
    newCase.setMsoa(collectionCase.getMsoa());
    newCase.setLad(collectionCase.getLad());
    newCase.setHtcWillingness(collectionCase.getHtcWillingness());
    newCase.setHtcDigital(collectionCase.getHtcDigital());

    return newCase;
  }
}
