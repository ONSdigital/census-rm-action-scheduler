package uk.gov.ons.census.action.messaging;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.hamcrest.beans.SamePropertyValuesAs;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.ons.census.action.model.dto.CollectionCase;
import uk.gov.ons.census.action.model.dto.Event;
import uk.gov.ons.census.action.model.dto.EventType;
import uk.gov.ons.census.action.model.dto.FulfilmentRequestDTO;
import uk.gov.ons.census.action.model.dto.Payload;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.dto.Uac;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;
import uk.gov.ons.census.action.service.FulfilmentRequestService;

public class CaseAndUacReceiverTest {
  private static final String INDIVIDUAL_PRINT_QUESTIONNAIRE_CODE = "P_OR_I1";
  private final CaseRepository caseRepository = mock(CaseRepository.class);
  private final UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
  private final FulfilmentRequestService fulfilmentRequestService =
      mock(FulfilmentRequestService.class);

  private EasyRandom easyRandom = new EasyRandom();

  @Test
  public void testReceiveEventCaseCreatedIgnoresCCSCase() {
    // Given
    CaseAndUacReceiver caseAndUacReceiver =
        new CaseAndUacReceiver(caseRepository, uacQidLinkRepository, fulfilmentRequestService);
    ResponseManagementEvent responseManagementEvent = getResponseManagementEvent();
    responseManagementEvent.getPayload().getCollectionCase().setSurvey("CCS");
    responseManagementEvent.getEvent().setType(EventType.CASE_CREATED);

    // When
    caseAndUacReceiver.receiveEvent(responseManagementEvent);

    // Then
    verifyZeroInteractions(caseRepository);
    verifyZeroInteractions(uacQidLinkRepository);
    verifyZeroInteractions(fulfilmentRequestService);
  }

  @Test
  public void testReceiveEventCaseUpdatedIgnoresCCSCase() {
    // Given
    CaseAndUacReceiver caseAndUacReceiver =
        new CaseAndUacReceiver(caseRepository, uacQidLinkRepository, fulfilmentRequestService);
    ResponseManagementEvent responseManagementEvent = getResponseManagementEvent();
    responseManagementEvent.getPayload().getCollectionCase().setSurvey("CCS");
    responseManagementEvent.getEvent().setType(EventType.CASE_UPDATED);

    // When
    caseAndUacReceiver.receiveEvent(responseManagementEvent);

    // Then
    verifyZeroInteractions(caseRepository);
    verifyZeroInteractions(uacQidLinkRepository);
    verifyZeroInteractions(fulfilmentRequestService);
  }

  @Test
  public void testReceiveEventUACUpdatedIgnoresCCSQID() {
    // Given
    CaseAndUacReceiver caseAndUacReceiver =
        new CaseAndUacReceiver(caseRepository, uacQidLinkRepository, fulfilmentRequestService);
    ResponseManagementEvent responseManagementEvent = getResponseManagementEvent();
    responseManagementEvent.getPayload().getUac().setQuestionnaireId("51");
    responseManagementEvent.getEvent().setType(EventType.UAC_UPDATED);

    // When
    caseAndUacReceiver.receiveEvent(responseManagementEvent);

    // Then
    verifyZeroInteractions(caseRepository);
    verifyZeroInteractions(uacQidLinkRepository);
    verifyZeroInteractions(fulfilmentRequestService);
  }

  @Test
  public void testCaseCreated() {
    // given
    CaseAndUacReceiver caseAndUacReceiver =
        new CaseAndUacReceiver(caseRepository, uacQidLinkRepository, fulfilmentRequestService);
    ResponseManagementEvent responseManagementEvent = getResponseManagementEvent();
    responseManagementEvent.getEvent().setType(EventType.CASE_CREATED);

    // when
    caseAndUacReceiver.receiveEvent(responseManagementEvent);

    // then
    ArgumentCaptor<Case> eventArgumentCaptor = ArgumentCaptor.forClass(Case.class);
    verify(caseRepository, times(1)).save(eventArgumentCaptor.capture());
    Case actualCase = eventArgumentCaptor.getAllValues().get(0);
    Case expectedCase = getExpectedCase(responseManagementEvent.getPayload().getCollectionCase());

    assertThat(actualCase, SamePropertyValuesAs.samePropertyValuesAs(expectedCase));
  }

  @Test
  public void testCECaseCreated() {
    // given
    CaseAndUacReceiver caseAndUacReceiver =
        new CaseAndUacReceiver(caseRepository, uacQidLinkRepository, fulfilmentRequestService);
    ResponseManagementEvent responseManagementEvent = getCEResponseManagementEvent();
    responseManagementEvent.getEvent().setType(EventType.CASE_CREATED);

    // when
    caseAndUacReceiver.receiveEvent(responseManagementEvent);

    // then
    ArgumentCaptor<Case> eventArgumentCaptor = ArgumentCaptor.forClass(Case.class);
    verify(caseRepository, times(1)).save(eventArgumentCaptor.capture());
    Case actualCase = eventArgumentCaptor.getAllValues().get(0);
    Case expectedCase = getExpectedCase(responseManagementEvent.getPayload().getCollectionCase());

    assertThat(actualCase, SamePropertyValuesAs.samePropertyValuesAs(expectedCase));
  }

  @Test
  public void testCaseUpdated() {
    // given
    CaseAndUacReceiver caseAndUacReceiver =
        new CaseAndUacReceiver(caseRepository, uacQidLinkRepository, fulfilmentRequestService);
    ResponseManagementEvent responseManagementEvent = getResponseManagementEvent();
    responseManagementEvent.getEvent().setType(EventType.CASE_UPDATED);

    Case expectedCase = easyRandom.nextObject(Case.class);
    when(caseRepository.findByCaseId(any())).thenReturn(Optional.of(expectedCase));

    // when
    caseAndUacReceiver.receiveEvent(responseManagementEvent);

    // then
    ArgumentCaptor<Case> eventArgumentCaptor = ArgumentCaptor.forClass(Case.class);
    verify(caseRepository, times(1)).save(eventArgumentCaptor.capture());
    Case actualCase = eventArgumentCaptor.getAllValues().get(0);

    assertThat(actualCase, SamePropertyValuesAs.samePropertyValuesAs(expectedCase));
  }

  @Test
  public void testCECaseUpdated() {
    // given
    CaseAndUacReceiver caseAndUacReceiver =
        new CaseAndUacReceiver(caseRepository, uacQidLinkRepository, fulfilmentRequestService);
    ResponseManagementEvent responseManagementEvent = getCEResponseManagementEvent();
    responseManagementEvent.getEvent().setType(EventType.CASE_UPDATED);

    Case expectedCase = easyRandom.nextObject(Case.class);
    expectedCase.setMetadata(null);

    when(caseRepository.findByCaseId(any())).thenReturn(Optional.of(expectedCase));

    // when
    caseAndUacReceiver.receiveEvent(responseManagementEvent);

    // then
    ArgumentCaptor<Case> eventArgumentCaptor = ArgumentCaptor.forClass(Case.class);
    verify(caseRepository, times(1)).save(eventArgumentCaptor.capture());
    Case actualCase = eventArgumentCaptor.getAllValues().get(0);

    // This test is pretty useless because it's comparing the sane object with itself
    assertThat(actualCase, SamePropertyValuesAs.samePropertyValuesAs(expectedCase));

    assertNotNull(actualCase.getMetadata());
    assertEquals("TRUE", actualCase.getMetadata().get("secureEstablishment"));
  }

  @Test
  public void testCaseCreatedWithFulfilmentAttached() {
    // given
    CaseAndUacReceiver caseAndUacReceiver =
        new CaseAndUacReceiver(caseRepository, uacQidLinkRepository, fulfilmentRequestService);
    ResponseManagementEvent responseManagementEvent = getResponseManagementEvent();
    responseManagementEvent.getEvent().setType(EventType.CASE_CREATED);
    FulfilmentRequestDTO fulfilmentRequestDTO = easyRandom.nextObject(FulfilmentRequestDTO.class);
    fulfilmentRequestDTO.setFulfilmentCode(INDIVIDUAL_PRINT_QUESTIONNAIRE_CODE);
    responseManagementEvent.getPayload().setFulfilmentRequest(fulfilmentRequestDTO);
    when(fulfilmentRequestService.determineActionType(INDIVIDUAL_PRINT_QUESTIONNAIRE_CODE))
        .thenReturn(ActionType.P_OR_IX);

    // when
    caseAndUacReceiver.receiveEvent(responseManagementEvent);

    // then
    ArgumentCaptor<Case> caseArgumentCaptor = ArgumentCaptor.forClass(Case.class);
    verify(caseRepository, times(1)).save(caseArgumentCaptor.capture());
    Case actualCase = caseArgumentCaptor.getAllValues().get(0);
    Case expectedCase = getExpectedCase(responseManagementEvent.getPayload().getCollectionCase());

    assertThat(actualCase, SamePropertyValuesAs.samePropertyValuesAs(expectedCase));

    verify(fulfilmentRequestService, times(1))
        .processEvent(
            eq(fulfilmentRequestDTO), caseArgumentCaptor.capture(), eq(ActionType.P_OR_IX));

    assertThat(
        caseArgumentCaptor.getAllValues().get(0),
        SamePropertyValuesAs.samePropertyValuesAs(expectedCase));
  }

  @Test
  public void testCaseUACUpdate() {
    // given
    CaseAndUacReceiver caseAndUacReceiver =
        new CaseAndUacReceiver(caseRepository, uacQidLinkRepository, fulfilmentRequestService);
    ResponseManagementEvent responseManagementEvent = getResponseManagementEvent();
    responseManagementEvent.getEvent().setType(EventType.UAC_UPDATED);
    responseManagementEvent.getPayload().getUac().setQuestionnaireId("01");

    // when
    caseAndUacReceiver.receiveEvent(responseManagementEvent);

    // then
    ArgumentCaptor<UacQidLink> eventArgumentCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    verify(uacQidLinkRepository, times(1)).save(eventArgumentCaptor.capture());
    UacQidLink actualUacQidLink = eventArgumentCaptor.getAllValues().get(0);

    Uac uac = responseManagementEvent.getPayload().getUac();
    assertEquals(actualUacQidLink.getQid(), uac.getQuestionnaireId());
    assertEquals(actualUacQidLink.getUac(), uac.getUac());
    assertEquals(actualUacQidLink.getCaseId(), uac.getCaseId());
  }

  @Test
  public void testUacUpdate() {
    // Given
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    Event event = new Event();
    event.setType(EventType.UAC_UPDATED);
    Uac uac = new Uac();
    uac.setQuestionnaireId("Test QID");
    uac.setUac("Test UAC");
    uac.setCaseId("Test Case Id");
    uac.setActive(true);
    Payload payload = new Payload();
    payload.setUac(uac);
    responseManagementEvent.setEvent(event);
    responseManagementEvent.setPayload(payload);
    CaseAndUacReceiver underTest =
        new CaseAndUacReceiver(caseRepository, uacQidLinkRepository, fulfilmentRequestService);

    // When
    underTest.receiveEvent(responseManagementEvent);

    // Then
    verify(uacQidLinkRepository).findByQid(eq("Test QID"));
    ArgumentCaptor<UacQidLink> uacQidLinkArgumentCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    verify(uacQidLinkRepository).save(uacQidLinkArgumentCaptor.capture());
    UacQidLink actualUacQidLink = uacQidLinkArgumentCaptor.getValue();
    assertEquals("Test Case Id", actualUacQidLink.getCaseId());
    assertEquals("Test QID", actualUacQidLink.getQid());
    assertEquals("Test UAC", actualUacQidLink.getUac());
    assertEquals(true, actualUacQidLink.isActive());
  }

  @Test
  public void testUacUpdateExisting() {
    // Given
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    Event event = new Event();
    event.setType(EventType.UAC_UPDATED);
    Uac uac = new Uac();
    uac.setQuestionnaireId("Test QID");
    uac.setUac("Test UAC");
    uac.setCaseId("Updated Test Case ID");
    uac.setActive(false);
    Payload payload = new Payload();
    payload.setUac(uac);
    responseManagementEvent.setEvent(event);
    responseManagementEvent.setPayload(payload);
    CaseAndUacReceiver underTest =
        new CaseAndUacReceiver(caseRepository, uacQidLinkRepository, fulfilmentRequestService);
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setActive(true);
    uacQidLink.setCaseId("Change me");
    when(uacQidLinkRepository.findByQid(anyString())).thenReturn(Optional.of(uacQidLink));

    // When
    underTest.receiveEvent(responseManagementEvent);

    // Then
    verify(uacQidLinkRepository).findByQid(eq("Test QID"));
    ArgumentCaptor<UacQidLink> uacQidLinkArgumentCaptor = ArgumentCaptor.forClass(UacQidLink.class);
    verify(uacQidLinkRepository).save(uacQidLinkArgumentCaptor.capture());
    UacQidLink actualUacQidLink = uacQidLinkArgumentCaptor.getValue();
    assertEquals("Updated Test Case ID", actualUacQidLink.getCaseId());
    assertEquals(false, actualUacQidLink.isActive());
  }

  @Test(expected = RuntimeException.class)
  public void testUnknownEventType() {
    // Given
    CaseAndUacReceiver caseAndUacReceiver =
        new CaseAndUacReceiver(caseRepository, uacQidLinkRepository, fulfilmentRequestService);
    ResponseManagementEvent responseManagementEvent = getResponseManagementEvent();
    responseManagementEvent.getEvent().setType(EventType.PRINT_CASE_SELECTED);

    String expectedErrorMessage =
        String.format("Unexpected event type '%s'", EventType.PRINT_CASE_SELECTED);

    try {
      // When
      caseAndUacReceiver.receiveEvent(responseManagementEvent);
    } catch (RuntimeException re) {
      // THEN
      Assertions.assertThat(re.getMessage()).isEqualTo(expectedErrorMessage);
      throw re;
    }
  }

  private ResponseManagementEvent getResponseManagementEvent() {
    ResponseManagementEvent responseManagementEvent =
        easyRandom.nextObject(ResponseManagementEvent.class);

    responseManagementEvent.getPayload().getCollectionCase().setCaseRef("123");
    responseManagementEvent
        .getPayload()
        .getCollectionCase()
        .setId("d09ac28e-d62f-4cdd-a5f9-e366e05f0fcd");
    responseManagementEvent.getPayload().getUac().setQuestionnaireId("123");
    responseManagementEvent.getPayload().getCollectionCase().setReceiptReceived(false);
    responseManagementEvent.getPayload().getCollectionCase().setRefusalReceived(false);
    return responseManagementEvent;
  }

  private ResponseManagementEvent getCEResponseManagementEvent() {
    ResponseManagementEvent responseManagementEvent =
        easyRandom.nextObject(ResponseManagementEvent.class);

    responseManagementEvent.getPayload().getCollectionCase().setCaseRef("123");
    responseManagementEvent
        .getPayload()
        .getCollectionCase()
        .setId("d09ac28e-d62f-4cdd-a5f9-e366e05f0fcd");
    responseManagementEvent.getPayload().getUac().setQuestionnaireId("123");
    responseManagementEvent.getPayload().getCollectionCase().setReceiptReceived(false);
    responseManagementEvent.getPayload().getCollectionCase().setRefusalReceived(false);
    Map<String, String> metadata = new HashMap<>();
    metadata.put("secureEstablishment", "TRUE");
    responseManagementEvent.getPayload().getCollectionCase().setMetadata(metadata);
    return responseManagementEvent;
  }

  private Case getExpectedCase(CollectionCase collectionCase) {
    Case newCase = new Case();
    newCase.setCaseRef(Integer.parseInt(collectionCase.getCaseRef()));
    newCase.setCaseId(UUID.fromString(collectionCase.getId()));
    newCase.setCaseType(collectionCase.getCaseType());
    newCase.setActionPlanId(collectionCase.getActionPlanId());
    newCase.setCollectionExerciseId(collectionCase.getCollectionExerciseId());
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
    newCase.setRegion(collectionCase.getAddress().getRegion());
    newCase.setOa(collectionCase.getOa());
    newCase.setLsoa(collectionCase.getLsoa());
    newCase.setMsoa(collectionCase.getMsoa());
    newCase.setLad(collectionCase.getLad());
    newCase.setHtcWillingness(collectionCase.getHtcWillingness());
    newCase.setHtcDigital(collectionCase.getHtcDigital());
    newCase.setAddressLevel(collectionCase.getAddress().getAddressLevel());
    newCase.setAbpCode(collectionCase.getAddress().getApbCode());
    newCase.setAddressType(collectionCase.getAddress().getAddressType());
    newCase.setUprn(collectionCase.getAddress().getUprn());
    newCase.setEstabArid(collectionCase.getAddress().getEstabArid());
    newCase.setEstabType(collectionCase.getAddress().getEstabType());
    newCase.setOrganisationName(collectionCase.getAddress().getOrganisationName());
    newCase.setFieldCoordinatorId(collectionCase.getFieldCoordinatorId());
    newCase.setFieldOfficerId(collectionCase.getFieldOfficerId());
    newCase.setCeExpectedCapacity(collectionCase.getCeExpectedCapacity());
    newCase.setCeActualResponses(collectionCase.getCeActualResponses());
    newCase.setAddressInvalid(collectionCase.getAddressInvalid());
    newCase.setHandDelivery(collectionCase.isHandDelivery());
    newCase.setMetadata(collectionCase.getMetadata());
    return newCase;
  }
}
