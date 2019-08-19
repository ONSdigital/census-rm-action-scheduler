package uk.gov.ons.census.action.messaging;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.dto.*;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.repository.CaseRepository;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class FulfilmentRequestReceiverIT {

  @Value("${queueconfig.action-fulfilment-inbound-queue}")
  private String actionFulfilmentQueue;

  @Value("${queueconfig.events-exchange}")
  private String eventsExchange;

  @Value("${queueconfig.events-fulfilment-request-binding}")
  private String eventsFulfilmentRequestBinding;

  @Value("${queueconfig.outbound-printer-queue}")
  private String outboundPrinterQueue;

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(8089).httpsPort(8443));

  @Autowired private RabbitQueueHelper rabbitQueueHelper;

  @Autowired private CaseRepository caseRepository;

  private EasyRandom easyRandom = new EasyRandom();

  private ObjectMapper objectMapper = new ObjectMapper();

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(actionFulfilmentQueue);
    rabbitQueueHelper.purgeQueue(outboundPrinterQueue);
  }

  @Test
  public void testQuestionnaireFulfilment() throws InterruptedException, IOException {

    // Given
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundPrinterQueue);
    Case fulfillmentCase = this.setUpCase();
    ResponseManagementEvent actionFulfilmentEvent =
        getResponseManagementEvent(fulfillmentCase.getCaseId(), "P_OR_H1");
    String url = "/uacqid/create/";
    UacQidDTO uacQidDto = easyRandom.nextObject(UacQidDTO.class);
    String returnJson = objectMapper.writeValueAsString(uacQidDto);
    givenThat(
        post(urlEqualTo(url))
            .willReturn(
                aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBody(returnJson)));

    // When
    rabbitQueueHelper.sendMessage(
        eventsExchange, eventsFulfilmentRequestBinding, actionFulfilmentEvent);

    // Then
    PrintFileDto actualPrintFileDto = checkExpectedPrintFileDtoMessageReceived(outputQueue);
    checkAddressFieldsMatch(
        fulfillmentCase,
        actionFulfilmentEvent.getPayload().getFulfilmentRequest().getContact(),
        actualPrintFileDto);
    assertThat(actualPrintFileDto).isEqualToComparingOnlyGivenFields(uacQidDto, "uac", "qid");
  }

  @Test
  public void testLargePrintQuestionnaireFulfilment() throws InterruptedException, IOException {

    // Given
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundPrinterQueue);
    Case fulfillmentCase = this.setUpCase();
    ResponseManagementEvent actionFulfilmentEvent =
        getResponseManagementEvent(fulfillmentCase.getCaseId(), "P_LP_HL1");

    // When
    rabbitQueueHelper.sendMessage(
        eventsExchange, eventsFulfilmentRequestBinding, actionFulfilmentEvent);

    // Then
    PrintFileDto actualPrintFileDto = checkExpectedPrintFileDtoMessageReceived(outputQueue);

    checkAddressFieldsMatch(
        fulfillmentCase,
        actionFulfilmentEvent.getPayload().getFulfilmentRequest().getContact(),
        actualPrintFileDto);
    assertThat(actualPrintFileDto.getUac()).isNull();
    assertThat(actualPrintFileDto.getQid()).isNull();
  }

  @Test
  public void testTranslationBookletFulfilment() throws InterruptedException, IOException {

    // Given
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundPrinterQueue);
    Case fulfillmentCase = this.setUpCase();
    ResponseManagementEvent actionFulfilmentEvent =
        getResponseManagementEvent(fulfillmentCase.getCaseId(), "P_TB_TBARA1");

    // When
    rabbitQueueHelper.sendMessage(
        eventsExchange, eventsFulfilmentRequestBinding, actionFulfilmentEvent);

    PrintFileDto actualPrintFileDto = checkExpectedPrintFileDtoMessageReceived(outputQueue);

    checkAddressFieldsMatch(
        fulfillmentCase,
        actionFulfilmentEvent.getPayload().getFulfilmentRequest().getContact(),
        actualPrintFileDto);
    assertThat(actualPrintFileDto.getUac()).isNull();
    assertThat(actualPrintFileDto.getQid()).isNull();
  }

  private void checkAddressFieldsMatch(
      Case expectedCase, Contact expectedContact, PrintFileDto actualPrintFileDto) {
    assertThat(actualPrintFileDto)
        .isEqualToComparingOnlyGivenFields(
            expectedCase,
            "addressLine1",
            "addressLine2",
            "addressLine3",
            "postcode",
            "townName",
            "caseRef");
    assertThat(actualPrintFileDto)
        .isEqualToComparingOnlyGivenFields(expectedContact, "title", "forename", "surname");
  }

  private PrintFileDto checkExpectedPrintFileDtoMessageReceived(BlockingQueue<String> queue)
      throws InterruptedException, IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    String actualMessage = queue.poll(20, TimeUnit.SECONDS);
    assertNotNull("Did not receive message before timeout", actualMessage);

    return objectMapper.readValue(actualMessage, PrintFileDto.class);
  }

  private ResponseManagementEvent getResponseManagementEvent(UUID caseId, String fulfilmentCode) {
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();

    FulfilmentRequestDTO fulfilmentRequest = easyRandom.nextObject(FulfilmentRequestDTO.class);
    fulfilmentRequest.setFulfilmentCode(fulfilmentCode);
    responseManagementEvent.setPayload(new Payload());
    fulfilmentRequest.setCaseId(caseId);
    responseManagementEvent.getPayload().setFulfilmentRequest(fulfilmentRequest);

    return responseManagementEvent;
  }

  private Case setUpCase() {
    Case fulfilmentCase = easyRandom.nextObject(Case.class);
    caseRepository.saveAndFlush(fulfilmentCase);
    return fulfilmentCase;
  }
}
