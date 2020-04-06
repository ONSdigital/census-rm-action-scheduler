package uk.gov.ons.census.action.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.ons.census.action.builders.CaseSelectedBuilder;
import uk.gov.ons.census.action.client.CaseClient;
import uk.gov.ons.census.action.model.dto.FulfilmentRequestDTO;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.dto.UacQidDTO;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.FulfilmentToSend;
import uk.gov.ons.census.action.model.repository.FulfilmentToSendRepository;

@RunWith(MockitoJUnitRunner.class)
public class FulfillmentRequestServiceTest {
  @Mock CaseClient caseClient;
  @Mock RabbitTemplate rabbitTemplate;
  @Mock CaseSelectedBuilder caseSelectedBuilder;
  @Mock FulfilmentToSendRepository fulfilmentToSendRepository;
  @Mock JdbcTemplate jdbcTemplate;

  @InjectMocks FulfilmentRequestService underTest;

  @Value("${queueconfig.outbound-exchange}")
  private String outboundExchange;

  @Value("${queueconfig.action-case-exchange}")
  private String actionCaseExchange;

  private final EasyRandom easyRandom = new EasyRandom();

  @Test
  public void testLargePrintQuestionnaireFulfilmentMappings() {
    assertThat(underTest.determineActionType("P_LP_HL1")).isEqualTo(ActionType.P_LP_HLX);
  }

  @Test
  public void testTranslationBookletFulfilmentMappings() {
    assertThat(underTest.determineActionType("P_TB_TBARA1")).isEqualTo(ActionType.P_TB_TBX);
  }

  @Test
  public void testIndividualPrintFulfilment() {
    FulfilmentRequestDTO fulfilmentRequestDTO = easyRandom.nextObject(FulfilmentRequestDTO.class);
    fulfilmentRequestDTO.setFulfilmentCode("P_OR_I1");
    Case caze = easyRandom.nextObject(Case.class);

    UacQidDTO uacQidDTO = easyRandom.nextObject(UacQidDTO.class);
    when(caseClient.getUacQid(caze.getCaseId(), "21")).thenReturn(uacQidDTO);
    when(caseSelectedBuilder.buildPrintMessage(any(), any()))
        .thenReturn(new ResponseManagementEvent());

    underTest.processEvent(fulfilmentRequestDTO, caze, ActionType.P_OR_IX);

    checkCorrectPackCodeAndAddressAreSent(fulfilmentRequestDTO, caze, ActionType.P_OR_IX);
  }

  @Test
  public void testHouseholdBookletlPrintFulfilment() {
    FulfilmentRequestDTO fulfilmentRequestDTO = easyRandom.nextObject(FulfilmentRequestDTO.class);
    fulfilmentRequestDTO.setFulfilmentCode("P_TB_TBARA1");
    Case caze = easyRandom.nextObject(Case.class);

    when(caseSelectedBuilder.buildPrintMessage(any(), any()))
        .thenReturn(new ResponseManagementEvent());

    underTest.processEvent(fulfilmentRequestDTO, caze, ActionType.P_OR_IX);

    checkCorrectPackCodeAndAddressAreSent(fulfilmentRequestDTO, caze, ActionType.P_OR_IX);

    verifyZeroInteractions(caseClient);
  }

  @Test(expected = RuntimeException.class)
  public void testQmFulfilmentForHandDeliverCaseMissingFieldIdsIsRejected() {
    FulfilmentRequestDTO fulfilmentRequestDTO = easyRandom.nextObject(FulfilmentRequestDTO.class);
    fulfilmentRequestDTO.setFulfilmentCode("P_OR_H1");
    Case caze = easyRandom.nextObject(Case.class);
    caze.setHandDelivery(true);
    caze.setFieldCoordinatorId(null);
    caze.setFieldOfficerId(null);

    try {
      underTest.processEvent(fulfilmentRequestDTO, caze, ActionType.P_OR_HX);
    } catch (RuntimeException runtimeException) {
      assertThat(runtimeException.getMessage()).contains("fieldOfficerId");
      assertThat(runtimeException.getMessage()).contains("fieldCoordinatorId");
      assertThat(runtimeException.getMessage()).contains(caze.getCaseId().toString());
      assertThat(runtimeException.getMessage()).contains(fulfilmentRequestDTO.getFulfilmentCode());
      throw runtimeException;
    }
  }

  @Test(expected = RuntimeException.class)
  public void testCaseMissingMandatoryAddressFieldsIsRejected() {
    FulfilmentRequestDTO fulfilmentRequestDTO = easyRandom.nextObject(FulfilmentRequestDTO.class);
    fulfilmentRequestDTO.setFulfilmentCode("P_OR_H1");
    Case caze = easyRandom.nextObject(Case.class);
    caze.setAddressLine1(null);
    caze.setPostcode(null);

    try {
      underTest.processEvent(fulfilmentRequestDTO, caze, ActionType.P_OR_HX);
    } catch (RuntimeException runtimeException) {
      assertThat(runtimeException.getMessage()).contains("addressLine1");
      assertThat(runtimeException.getMessage()).contains("postcode");
      assertThat(runtimeException.getMessage()).contains(caze.getCaseId().toString());
      assertThat(runtimeException.getMessage()).contains(fulfilmentRequestDTO.getFulfilmentCode());
      throw runtimeException;
    }
  }

  private void checkCorrectPackCodeAndAddressAreSent(
      FulfilmentRequestDTO fulfilmentRequestDTO,
      Case fulfilmentCase,
      ActionType expectedActionType) {

    ArgumentCaptor<ResponseManagementEvent> responseManagementEventArgumentCaptor =
        ArgumentCaptor.forClass(ResponseManagementEvent.class);

    verify(rabbitTemplate)
        .convertAndSend(
            eq(actionCaseExchange), eq(""), responseManagementEventArgumentCaptor.capture());

    ArgumentCaptor<String> printFileDtoArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<FulfilmentToSend> fulfilmentsToSendArgumentCaptor =
        ArgumentCaptor.forClass(FulfilmentToSend.class);

    verify(fulfilmentToSendRepository).saveAndFlush(fulfilmentsToSendArgumentCaptor.capture());

    PrintFileDto printFile = fulfilmentsToSendArgumentCaptor.getValue().getMessageData();
    assertEquals(fulfilmentRequestDTO.getFulfilmentCode(), printFile.getPackCode());
    assertEquals(expectedActionType.name(), printFile.getActionType());
    assertThat(printFile)
        .isEqualToComparingOnlyGivenFields(
            fulfilmentCase, "addressLine1", "addressLine2", "addressLine3", "postcode", "townName");
    assertThat(printFile)
        .isEqualToComparingOnlyGivenFields(
            fulfilmentRequestDTO.getContact(), "title", "forename", "surname");
  }
}
