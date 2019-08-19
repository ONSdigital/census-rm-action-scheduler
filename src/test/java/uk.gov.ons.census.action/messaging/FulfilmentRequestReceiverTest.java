package uk.gov.ons.census.action.messaging;

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
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.dto.UacQidDTO;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.repository.CaseRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FulfilmentRequestReceiverTest {

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

    // Then no exception is thrown
  }

  @Test
  public void testReceiveFulfilmentRequestP_OR_H1() {
    // Given
    testOnRequestQuestionnaireFulfilment("P_OR_H1", ActionType.P_OR_HX, "1");
  }

  @Test
  public void testReceiveFulfilmentRequestP_OR_H2() {
    // Given
    testOnRequestQuestionnaireFulfilment("P_OR_H2", ActionType.P_OR_HX, "2");
  }

  @Test
  public void testReceiveFulfilmentRequestP_OR_H2W() {
    // Given
    testOnRequestQuestionnaireFulfilment("P_OR_H2W", ActionType.P_OR_HX, "3");
  }

  @Test
  public void testReceiveFulfilmentRequestP_OR_H4() {
    // Given
    testOnRequestQuestionnaireFulfilment("P_OR_H4", ActionType.P_OR_HX, "4");
  }

  private void testOnRequestQuestionnaireFulfilment(
      String fulfilmentCode, ActionType actionType, String questionnaireType) {
    Case fulfilmentCase = caseRepositoryReturnsRandomCase();
    ResponseManagementEvent event = easyRandom.nextObject(ResponseManagementEvent.class);
    event.getPayload().getFulfilmentRequest().setFulfilmentCode(fulfilmentCode);
    UacQidDTO uacQidDTO = easyRandom.nextObject(UacQidDTO.class);
    when(caseClient.getUacQid(eq(fulfilmentCase.getCaseId()), eq(questionnaireType)))
        .thenReturn(uacQidDTO);

    // When
    underTest.receiveEvent(event);

    // Then
  checkCorrectPrintFileDTOIsSent(event, fulfilmentCase, actionType);
  }

  private void checkCorrectPrintFileDTOIsSent(
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
  }

  private Case caseRepositoryReturnsRandomCase() {
    Case caze = easyRandom.nextObject(Case.class);
    when(caseRepository.findByCaseId(any(UUID.class))).thenReturn(Optional.of(caze));
    return caze;
  }
}
