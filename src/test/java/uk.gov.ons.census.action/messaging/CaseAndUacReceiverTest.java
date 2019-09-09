package uk.gov.ons.census.action.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.util.UUID;
import org.hamcrest.beans.SamePropertyValuesAs;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.ons.census.action.model.dto.CollectionCase;
import uk.gov.ons.census.action.model.dto.EventType;
import uk.gov.ons.census.action.model.dto.FulfilmentRequestDTO;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.dto.Uac;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.CaseState;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;
import uk.gov.ons.census.action.service.FulfilmentRequestService;

public class CaseAndUacReceiverTest {
  public static final String INDIVIDUAL_PRINT_QUESTIONNAIRE_CODE = "P_OR_I1";
  private final CaseRepository caseRepository = mock(CaseRepository.class);
  private final UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
  private final FulfilmentRequestService fulfilmentRequestService =
      mock(FulfilmentRequestService.class);

  EasyRandom easyRandom = new EasyRandom();

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

    ArgumentCaptor<FulfilmentRequestDTO> fulfilmentRequestDTOArgumentCaptor =
        ArgumentCaptor.forClass(FulfilmentRequestDTO.class);
    ArgumentCaptor<ActionType> actionTypeArgumentCaptor = ArgumentCaptor.forClass(ActionType.class);
    verify(fulfilmentRequestService, times(1))
        .processEvent(
            eq(fulfilmentRequestDTO), caseArgumentCaptor.capture(), eq(ActionType.P_OR_IX));

    assertThat(
        caseArgumentCaptor.getAllValues().get(0),
        SamePropertyValuesAs.samePropertyValuesAs(expectedCase));
  }

  @Test
  public void testCaseUpdate() {
    // given
    CaseAndUacReceiver caseAndUacReceiver =
        new CaseAndUacReceiver(caseRepository, uacQidLinkRepository, fulfilmentRequestService);
    ResponseManagementEvent responseManagementEvent = getResponseManagementEvent();
    responseManagementEvent.getEvent().setType(EventType.UAC_UPDATED);

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

  private ResponseManagementEvent getResponseManagementEvent() {
    ResponseManagementEvent responseManagementEvent =
        easyRandom.nextObject(ResponseManagementEvent.class);

    responseManagementEvent.getPayload().getCollectionCase().setCaseRef("123");
    responseManagementEvent
        .getPayload()
        .getCollectionCase()
        .setId("d09ac28e-d62f-4cdd-a5f9-e366e05f0fcd");
    responseManagementEvent.getPayload().getCollectionCase().setState("ACTIONABLE");
    responseManagementEvent.getPayload().getCollectionCase().setReceiptReceived(false);
    responseManagementEvent.getPayload().getCollectionCase().setRefusalReceived(false);
    return responseManagementEvent;
  }

  private Case getExpectedCase(CollectionCase collectionCase) {
    Case newCase = new Case();
    newCase.setCaseRef(Integer.parseInt(collectionCase.getCaseRef()));
    newCase.setCaseId(UUID.fromString(collectionCase.getId()));
    newCase.setCaseType(collectionCase.getCaseType());
    newCase.setActionPlanId(collectionCase.getActionPlanId());
    newCase.setCollectionExerciseId(collectionCase.getCollectionExerciseId());
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
    newCase.setAddressInvalid(collectionCase.getAddressInvalid());
    return newCase;
  }
}
