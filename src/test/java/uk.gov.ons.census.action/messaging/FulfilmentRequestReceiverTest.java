package uk.gov.ons.census.action.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.ons.census.action.client.CaseClient;
import uk.gov.ons.census.action.model.dto.FulfilmentRequestDTO;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.dto.UacQidDTO;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.repository.CaseRepository;

@RunWith(MockitoJUnitRunner.class)
public class FulfilmentRequestReceiverTest {
  private static final String PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_ENGLAND = "P_OR_I1";
  private static final String PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_WALES_ENGLISH = "P_OR_I2";
  private static final String PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_WALES_WELSH = "P_OR_I2W";
  private static final String PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_NORTHERN_IRELAND = "P_OR_I4";
  private static UUID TEST_INDIVIDUAL_CASE_ID = UUID.randomUUID();

  @Mock private RabbitTemplate rabbitTemplate;
  @Mock private CaseClient caseClient;
  @Mock private CaseRepository caseRepository;

  @InjectMocks FulfilmentRequestReceiver underTest;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  @Value("${queueconfig.outbound-printer-routing-key}")
  private String outboundPrinterRoutingKey;

  private EasyRandom easyRandom = new EasyRandom();

  @Test
  public void testReceiveEventIgnoresUnexpectedFulfilmentCode() {
    // Given
    caseRepositoryReturnsRandomCase();
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);

    // When
    underTest.receiveEvent(event);

    verifyZeroInteractions(caseRepository);
    verifyZeroInteractions(rabbitTemplate);
    verifyZeroInteractions(caseClient);
    // Then no exception is thrown
  }

  @Test
  public void testReceiveEventIgnoresUACFulfilmentCode() {
    // Given
    caseRepositoryReturnsRandomCase();
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getPayload().getFulfilmentRequest().setFulfilmentCode("UACHHT1");

    // When
    underTest.receiveEvent(event);

    verifyZeroInteractions(caseRepository);
    verifyZeroInteractions(rabbitTemplate);
    verifyZeroInteractions(caseClient);
    // Then no exception is thrown
  }

  @Test
  public void testOnRequestQuestionnaireFulfilment() {
    // Given
    Case fulfilmentCase = caseRepositoryReturnsRandomCase();
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getPayload().getFulfilmentRequest().setFulfilmentCode("P_OR_H1");
    event.getPayload().getFulfilmentRequest().setCaseId(fulfilmentCase.getCaseId());
    UacQidDTO uacQidDTO = easyRandom.nextObject(UacQidDTO.class);
    when(caseClient.getUacQid(eq(fulfilmentCase.getCaseId()), eq("1"))).thenReturn(uacQidDTO);

    // When
    underTest.receiveEvent(event);

    // Then
    PrintFileDto actualPrintFileDTO =
        checkCorrectPackCodeAndAddressAreSent(event, fulfilmentCase, ActionType.P_OR_HX);
    assertThat(actualPrintFileDTO).isEqualToComparingOnlyGivenFields(uacQidDTO, "uac", "qid");
  }

  @Test
  public void testOnRequestContinuationQuestionnaireFulfilment() {
    // Given
    Case fulfilmentCase = caseRepositoryReturnsRandomCase();
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getPayload().getFulfilmentRequest().setFulfilmentCode("P_OR_HC1");
    event.getPayload().getFulfilmentRequest().setCaseId(fulfilmentCase.getCaseId());
    UacQidDTO uacQidDTO = easyRandom.nextObject(UacQidDTO.class);
    when(caseClient.getUacQid(fulfilmentCase.getCaseId(), "11")).thenReturn(uacQidDTO);

    // When
    underTest.receiveEvent(event);

    // Then
    PrintFileDto actualPrintFileDTO =
        checkCorrectPackCodeAndAddressAreSent(event, fulfilmentCase, ActionType.P_OR_HX);
    assertThat(actualPrintFileDTO).isEqualToComparingOnlyGivenFields(uacQidDTO, "uac", "qid");
  }

  @Test
  public void testOnRequestIndividualQuestionnaireFulfilmentEngland() {
    testIndividualResponseRequest(
        PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_ENGLAND, "21", ActionType.P_OR_I1);
  }

  @Test(expected = RuntimeException.class)
  public void testRequestIndividualQuestionaireWithMissingCaseThrowsExceptions() {
    Case individualFulfilmentCase = easyRandom.nextObject(Case.class);
    individualFulfilmentCase.setCaseId(TEST_INDIVIDUAL_CASE_ID);

    individualFulfilmentCase.setCaseId(TEST_INDIVIDUAL_CASE_ID);
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    FulfilmentRequestDTO fulfilmentRequest = event.getPayload().getFulfilmentRequest();
    fulfilmentRequest.setFulfilmentCode(PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_ENGLAND);
    fulfilmentRequest.setIndividualCaseId(TEST_INDIVIDUAL_CASE_ID.toString());

    // When
    underTest.receiveEvent(event);

    verifyZeroInteractions(caseClient);
    verifyZeroInteractions(rabbitTemplate);
  }

  @Test
  public void testOnRequestIndividualQuestionnaireFulfilmentWalesEnglish() {
    testIndividualResponseRequest(
        PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_WALES_ENGLISH, "22", ActionType.P_OR_I2);
  }

  @Test
  public void testOnRequestIndividualQuestionnaireFulfilmentWalesWelsh() {
    testIndividualResponseRequest(
        PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_WALES_WELSH, "23", ActionType.P_OR_I2);
  }

  @Test
  public void testOnRequestIndividualQuestionnaireFulfilmentNorthernIreland() {
    testIndividualResponseRequest(
        PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_NORTHERN_IRELAND, "24", ActionType.P_OR_I4);
  }

  private void testIndividualResponseRequest(
      String fulfilmentCode, String expectedQuestionaireType, ActionType expectedActionType) {

    // Given
    Case individualFulfilmentCase = easyRandom.nextObject(Case.class);
    individualFulfilmentCase.setCaseId(TEST_INDIVIDUAL_CASE_ID);

    when(caseRepository.findByCaseId(TEST_INDIVIDUAL_CASE_ID))
        .thenReturn(Optional.of(individualFulfilmentCase));

    individualFulfilmentCase.setCaseId(TEST_INDIVIDUAL_CASE_ID);
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    FulfilmentRequestDTO fulfilmentRequest = event.getPayload().getFulfilmentRequest();
    fulfilmentRequest.setFulfilmentCode(fulfilmentCode);
    fulfilmentRequest.setIndividualCaseId(TEST_INDIVIDUAL_CASE_ID.toString());

    UacQidDTO uacQidDTO = easyRandom.nextObject(UacQidDTO.class);

    when(caseClient.getUacQid(TEST_INDIVIDUAL_CASE_ID, expectedQuestionaireType))
        .thenReturn(uacQidDTO);

    // When
    underTest.receiveEvent(event);

    // Then
    PrintFileDto actualPrintFileDTO =
        checkCorrectPackCodeAndAddressAreSent(event, individualFulfilmentCase, expectedActionType);

    assertThat(actualPrintFileDTO).isEqualToComparingOnlyGivenFields(uacQidDTO, "uac", "qid");
  }

  @Test
  public void testLargePrintQuestionnaireFulfilment() {
    // Given
    Case fulfilmentCase = caseRepositoryReturnsRandomCase();
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getPayload().getFulfilmentRequest().setFulfilmentCode("P_LP_HL1");
    event.getPayload().getFulfilmentRequest().setCaseId(fulfilmentCase.getCaseId());

    // When
    underTest.receiveEvent(event);

    // Then
    checkCorrectPackCodeAndAddressAreSent(event, fulfilmentCase, ActionType.P_LP_HLX);
    verify(caseClient, never()).getUacQid(any(UUID.class), any(String.class));
  }

  @Test
  public void testTranslationBookletFulfilment() {
    // Given
    Case fulfilmentCase = caseRepositoryReturnsRandomCase();
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getPayload().getFulfilmentRequest().setFulfilmentCode("P_TB_TBARA1");
    event.getPayload().getFulfilmentRequest().setCaseId(fulfilmentCase.getCaseId());

    // When
    underTest.receiveEvent(event);

    // Then
    checkCorrectPackCodeAndAddressAreSent(event, fulfilmentCase, ActionType.P_TB_TBX);
    verify(caseClient, never()).getUacQid(any(UUID.class), any(String.class));
  }

  private PrintFileDto checkCorrectPackCodeAndAddressAreSent(
      ResponseManagementEvent event, Case fulfilmentCase, ActionType expectedActionType) {
    ArgumentCaptor<PrintFileDto> printFileDtoArgumentCaptor =
        ArgumentCaptor.forClass(PrintFileDto.class);
    verify(rabbitTemplate)
        .convertAndSend(
            eq(outboundExchange),
            eq(outboundPrinterRoutingKey),
            printFileDtoArgumentCaptor.capture());
    PrintFileDto actualPrintFileDTO = printFileDtoArgumentCaptor.getValue();
    assertEquals(
        event.getPayload().getFulfilmentRequest().getFulfilmentCode(),
        printFileDtoArgumentCaptor.getValue().getPackCode());
    assertEquals(expectedActionType.name(), actualPrintFileDTO.getActionType());
    assertThat(actualPrintFileDTO)
        .isEqualToComparingOnlyGivenFields(
            fulfilmentCase, "addressLine1", "addressLine2", "addressLine3", "postcode", "townName");
    assertThat(actualPrintFileDTO)
        .isEqualToComparingOnlyGivenFields(
            event.getPayload().getFulfilmentRequest().getContact(), "title", "forename", "surname");
    return actualPrintFileDTO;
  }

  private Case caseRepositoryReturnsRandomCase() {
    Case caze = easyRandom.nextObject(Case.class);
    when(caseRepository.findByCaseId(caze.getCaseId())).thenReturn(Optional.of(caze));
    return caze;
  }
}
