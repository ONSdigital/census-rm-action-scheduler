package uk.gov.ons.census.action.messaging;

import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.service.FulfilmentRequestService;

@RunWith(MockitoJUnitRunner.class)
public class FulfilmentRequestReceiverTest {
  private static final String PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_ENGLAND = "P_OR_I1";
  private static final String PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_WALES_ENGLISH = "P_OR_I2";
  private static final String PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_WALES_WELSH = "P_OR_I2W";
  private static final String PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_NORTHERN_IRELAND = "P_OR_I4";
  private static UUID TEST_INDIVIDUAL_CASE_ID = UUID.randomUUID();

  @Mock private CaseRepository caseRepository;
  @Mock private FulfilmentRequestService fulfilmentRequestService;

  @InjectMocks FulfilmentRequestReceiver underTest;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  private EasyRandom easyRandom = new EasyRandom();

  @Test
  public void testReceiveEventIgnoresUnexpectedFulfilmentCode() {
    // Given
    when(fulfilmentRequestService.determineActionType(anyString())).thenReturn(null);
    caseRepositoryReturnsRandomCase();
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);

    // When
    underTest.receiveEvent(event);

    verifyZeroInteractions(caseRepository);
    // Then no exception is thrown
  }

  @Test
  public void testReceiveEventIgnoresUnwantedFulfilmentCodes() {
    // Given
    caseRepositoryReturnsRandomCase();
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getPayload().getFulfilmentRequest().setFulfilmentCode("UACHHT1");

    // When
    underTest.receiveEvent(event);

    verifyZeroInteractions(caseRepository);
  }

  @Test
  public void testOnRequestQuestionnaireFulfilment() {
    // Given
    Case fulfilmentCase = caseRepositoryReturnsRandomCase();
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getPayload().getFulfilmentRequest().setFulfilmentCode("P_OR_H1");
    event.getPayload().getFulfilmentRequest().setCaseId(fulfilmentCase.getCaseId());

    when(fulfilmentRequestService.determineActionType("P_OR_H1")).thenReturn(ActionType.P_OR_HX);

    // When
    underTest.receiveEvent(event);

    // Then
    verify(fulfilmentRequestService, times(1))
        .processEvent(
            event.getPayload().getFulfilmentRequest(), fulfilmentCase, ActionType.P_OR_HX);
  }

  @Test
  public void testOnRequestContinuationQuestionnaireFulfilment() {
    // Given
    Case fulfilmentCase = caseRepositoryReturnsRandomCase();
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getPayload().getFulfilmentRequest().setFulfilmentCode("P_OR_HC1");
    event.getPayload().getFulfilmentRequest().setCaseId(fulfilmentCase.getCaseId());

    when(fulfilmentRequestService.determineActionType("P_OR_HC1")).thenReturn(ActionType.P_OR_HX);

    // When
    underTest.receiveEvent(event);

    // Then
    verify(fulfilmentRequestService, times(1))
        .processEvent(
            event.getPayload().getFulfilmentRequest(), fulfilmentCase, ActionType.P_OR_HX);
  }

  @Test
  public void testOnRequestIndividualQuestionnaireFulfilmentEngland() {
    testIndividualResponseRequestIsIgnored(
        PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_ENGLAND, "21", ActionType.P_OR_IX);
  }

  @Test
  public void testOnRequestIndividualQuestionnaireFulfilmentWalesEnglish() {
    testIndividualResponseRequestIsIgnored(
        PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_WALES_ENGLISH, "22", ActionType.P_OR_IX);
  }

  @Test
  public void testOnRequestIndividualQuestionnaireFulfilmentWalesWelsh() {
    testIndividualResponseRequestIsIgnored(
        PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_WALES_WELSH, "23", ActionType.P_OR_IX);
  }

  @Test
  public void testOnRequestIndividualQuestionnaireFulfilmentNorthernIreland() {
    testIndividualResponseRequestIsIgnored(
        PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_NORTHERN_IRELAND, "24", ActionType.P_OR_IX);
  }

  private void testIndividualResponseRequestIsIgnored(
      String fulfilmentCode, String expectedQuestionaireType, ActionType expectedActionType) {

    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getPayload().getFulfilmentRequest().setFulfilmentCode(fulfilmentCode);

    underTest.receiveEvent(event);

    verifyZeroInteractions(fulfilmentRequestService);
    verifyZeroInteractions(caseRepository);
  }

  private Case caseRepositoryReturnsRandomCase() {
    Case caze = easyRandom.nextObject(Case.class);
    when(caseRepository.findByCaseId(caze.getCaseId())).thenReturn(Optional.of(caze));
    return caze;
  }
}
