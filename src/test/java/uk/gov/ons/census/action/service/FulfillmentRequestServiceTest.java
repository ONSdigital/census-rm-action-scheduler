package uk.gov.ons.census.action.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ons.census.action.model.entity.ActionType;

@RunWith(MockitoJUnitRunner.class)
public class FulfillmentRequestServiceTest {

  @InjectMocks FulfilmentRequestService underTest;

  @Test
  public void testLargePrintQuestionnaireFulfilmentMappings() {
    assertThat(underTest.determineActionType("P_LP_HL1")).isEqualTo(ActionType.P_LP_HLX);
  }

  @Test
  public void testTranslationBookletFulfilmentMappings() {
    assertThat(underTest.determineActionType("P_TB_TBARA1")).isEqualTo(ActionType.P_TB_TBX);
  }

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
//
//  @Test
//  public void testIndividualFulfillmentRequest() {
//
//      underTest.processEvent();
//  }

  // Test actual processing

  //
  //
  //    private PrintFileDto checkCorrectPackCodeAndAddressAreSent(
  //        ResponseManagementEvent event, Case fulfilmentCase, ActionType expectedActionType) {
  //      ArgumentCaptor<PrintFileDto> printFileDtoArgumentCaptor =
  //          ArgumentCaptor.forClass(PrintFileDto.class);
  //      verify(rabbitTemplate)
  //          .convertAndSend(
  //              eq(outboundExchange),
  //              eq("Action.Printer.binding"),
  //              printFileDtoArgumentCaptor.capture());
  //      PrintFileDto actualPrintFileDTO = printFileDtoArgumentCaptor.getValue();
  //      assertEquals(
  //          event.getPayload().getFulfilmentRequest().getFulfilmentCode(),
  //          printFileDtoArgumentCaptor.getValue().getPackCode());
  //      assertEquals(expectedActionType.name(), actualPrintFileDTO.getActionType());
  //      assertThat(actualPrintFileDTO)
  //          .isEqualToComparingOnlyGivenFields(
  //              fulfilmentCase, "addressLine1", "addressLine2", "addressLine3", "postcode",
  //   "townName");
  //      assertThat(actualPrintFileDTO)
  //          .isEqualToComparingOnlyGivenFields(
  //              event.getPayload().getFulfilmentRequest().getContact(), "title", "forename",
  //   "surname");
  //      return actualPrintFileDTO;
  //    }
  //

}
