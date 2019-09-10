package uk.gov.ons.census.action.messaging;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
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
  private static final String EVENTS_FULFILMENT_REQUEST_BINDING = "event.fulfilment.request";
  private static final String PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_ENGLAND = "P_OR_I1";

  @Value("${queueconfig.action-fulfilment-inbound-queue}")
  private String actionFulfilmentQueue;

  @Value("${queueconfig.events-exchange}")
  private String eventsExchange;

  @Value("${queueconfig.outbound-printer-queue}")
  private String outboundPrinterQueue;

  @Value("${queueconfig.action-case-queue}")
  private String actionCaseQueue;

  @Rule public WireMockRule mockCaseApi = new WireMockRule(wireMockConfig().port(8089));

  @Autowired private RabbitQueueHelper rabbitQueueHelper;

  @Autowired private CaseRepository caseRepository;

  private EasyRandom easyRandom = new EasyRandom();

  private ObjectMapper objectMapper = new ObjectMapper();

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(actionFulfilmentQueue);
    rabbitQueueHelper.purgeQueue(outboundPrinterQueue);
    rabbitQueueHelper.purgeQueue(actionCaseQueue);
  }

  @Test
  public void testQuestionnaireFulfilment() throws InterruptedException, IOException {

    // Given
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundPrinterQueue);
    BlockingQueue<String> caseSelectedQueue = rabbitQueueHelper.listen(actionCaseQueue);
    Case fulfillmentCase = this.setUpCaseAndSaveInDB();
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
        eventsExchange, EVENTS_FULFILMENT_REQUEST_BINDING, actionFulfilmentEvent);

    // Then
    PrintFileDto actualPrintFileDto =
        rabbitQueueHelper.checkExpectedMessageReceived(outputQueue, PrintFileDto.class);
    checkAddressFieldsMatch(
        fulfillmentCase,
        actionFulfilmentEvent.getPayload().getFulfilmentRequest().getContact(),
        actualPrintFileDto);
    assertThat(actualPrintFileDto).isEqualToComparingOnlyGivenFields(uacQidDto, "uac", "qid");

    ResponseManagementEvent actualRmEvent =
        rabbitQueueHelper.checkExpectedMessageReceived(
            caseSelectedQueue, ResponseManagementEvent.class);
    assertThat(actualRmEvent.getEvent().getType()).isEqualTo(EventType.PRINT_CASE_SELECTED);
    assertThat(actualRmEvent.getPayload().getPrintCaseSelected().getPackCode())
        .isEqualTo("P_OR_H1");
    assertThat(actualRmEvent.getPayload().getPrintCaseSelected().getCaseRef())
        .isEqualTo(fulfillmentCase.getCaseRef());
  }

  @Test
  public void testContinuationQuestionnaireFulfilment() throws InterruptedException, IOException {

    // Given
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundPrinterQueue);
    BlockingQueue<String> caseSelectedQueue = rabbitQueueHelper.listen(actionCaseQueue);
    Case fulfillmentCase = this.setUpCaseAndSaveInDB();
    ResponseManagementEvent actionFulfilmentEvent =
        getResponseManagementEvent(fulfillmentCase.getCaseId(), "P_OR_HC1");
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
        eventsExchange, EVENTS_FULFILMENT_REQUEST_BINDING, actionFulfilmentEvent);

    // Then
    PrintFileDto actualPrintFileDto =
        rabbitQueueHelper.checkExpectedMessageReceived(outputQueue, PrintFileDto.class);
    checkAddressFieldsMatch(
        fulfillmentCase,
        actionFulfilmentEvent.getPayload().getFulfilmentRequest().getContact(),
        actualPrintFileDto);
    assertThat(actualPrintFileDto).isEqualToComparingOnlyGivenFields(uacQidDto, "uac", "qid");

    ResponseManagementEvent actualRmEvent =
        rabbitQueueHelper.checkExpectedMessageReceived(
            caseSelectedQueue, ResponseManagementEvent.class);
    assertThat(actualRmEvent.getEvent().getType()).isEqualTo(EventType.PRINT_CASE_SELECTED);
    assertThat(actualRmEvent.getPayload().getPrintCaseSelected().getPackCode())
        .isEqualTo("P_OR_HC1");
    assertThat(actualRmEvent.getPayload().getPrintCaseSelected().getCaseRef())
        .isEqualTo(fulfillmentCase.getCaseRef());
  }

  @Test
  public void testLargePrintQuestionnaireFulfilment() throws InterruptedException, IOException {

    // Given
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundPrinterQueue);
    BlockingQueue<String> caseSelectedQueue = rabbitQueueHelper.listen(actionCaseQueue);
    Case fulfillmentCase = this.setUpCaseAndSaveInDB();
    ResponseManagementEvent actionFulfilmentEvent =
        getResponseManagementEvent(fulfillmentCase.getCaseId(), "P_LP_HL1");

    // When
    rabbitQueueHelper.sendMessage(
        eventsExchange, EVENTS_FULFILMENT_REQUEST_BINDING, actionFulfilmentEvent);

    // Then
    PrintFileDto actualPrintFileDto =
        rabbitQueueHelper.checkExpectedMessageReceived(outputQueue, PrintFileDto.class);

    checkAddressFieldsMatch(
        fulfillmentCase,
        actionFulfilmentEvent.getPayload().getFulfilmentRequest().getContact(),
        actualPrintFileDto);
    assertThat(actualPrintFileDto.getUac()).isNull();
    assertThat(actualPrintFileDto.getQid()).isNull();

    ResponseManagementEvent actualRmEvent =
        rabbitQueueHelper.checkExpectedMessageReceived(
            caseSelectedQueue, ResponseManagementEvent.class);
    assertThat(actualRmEvent.getEvent().getType()).isEqualTo(EventType.PRINT_CASE_SELECTED);
    assertThat(actualRmEvent.getPayload().getPrintCaseSelected().getPackCode())
        .isEqualTo("P_LP_HL1");
    assertThat(actualRmEvent.getPayload().getPrintCaseSelected().getCaseRef())
        .isEqualTo(fulfillmentCase.getCaseRef());
  }

  @Test
  public void testTranslationBookletFulfilment() throws InterruptedException, IOException {

    // Given
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundPrinterQueue);
    BlockingQueue<String> caseSelectedQueue = rabbitQueueHelper.listen(actionCaseQueue);
    Case fulfillmentCase = this.setUpCaseAndSaveInDB();
    ResponseManagementEvent actionFulfilmentEvent =
        getResponseManagementEvent(fulfillmentCase.getCaseId(), "P_TB_TBARA1");

    // When
    rabbitQueueHelper.sendMessage(
        eventsExchange, EVENTS_FULFILMENT_REQUEST_BINDING, actionFulfilmentEvent);

    PrintFileDto actualPrintFileDto =
        rabbitQueueHelper.checkExpectedMessageReceived(outputQueue, PrintFileDto.class);

    checkAddressFieldsMatch(
        fulfillmentCase,
        actionFulfilmentEvent.getPayload().getFulfilmentRequest().getContact(),
        actualPrintFileDto);
    assertThat(actualPrintFileDto.getUac()).isNull();
    assertThat(actualPrintFileDto.getQid()).isNull();

    ResponseManagementEvent actualRmEvent =
        rabbitQueueHelper.checkExpectedMessageReceived(
            caseSelectedQueue, ResponseManagementEvent.class);
    assertThat(actualRmEvent.getEvent().getType()).isEqualTo(EventType.PRINT_CASE_SELECTED);
    assertThat(actualRmEvent.getPayload().getPrintCaseSelected().getPackCode())
        .isEqualTo("P_TB_TBARA1");
    assertThat(actualRmEvent.getPayload().getPrintCaseSelected().getCaseRef())
        .isEqualTo(fulfillmentCase.getCaseRef());
  }

  @Test
  public void testIndividualResponseFulfilmentRequestIsIgnored()
      throws IOException, InterruptedException {
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundPrinterQueue);
    BlockingQueue<String> caseSelectedQueue = rabbitQueueHelper.listen(actionCaseQueue);
    Case fulfillmentCase = this.setUpCaseAndSaveInDB();
    UUID parentCaseId = UUID.randomUUID();
    UUID childCaseId = fulfillmentCase.getCaseId();
    ResponseManagementEvent actionFulfilmentEvent =
        getResponseManagementEvent(parentCaseId, PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_ENGLAND);
    actionFulfilmentEvent.getPayload().getFulfilmentRequest().setIndividualCaseId(childCaseId);

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

    rabbitQueueHelper.sendMessage(
        eventsExchange, EVENTS_FULFILMENT_REQUEST_BINDING, actionFulfilmentEvent);

    PrintFileDto actualPrintFileDto =
        rabbitQueueHelper.checkExpectedMessageReceived(outputQueue, PrintFileDto.class);

    checkAddressFieldsMatch(
        fulfillmentCase,
        actionFulfilmentEvent.getPayload().getFulfilmentRequest().getContact(),
        actualPrintFileDto);

    ResponseManagementEvent actualRmEvent =
        rabbitQueueHelper.checkExpectedMessageReceived(
            caseSelectedQueue, ResponseManagementEvent.class);
    assertThat(actualRmEvent.getEvent().getType()).isEqualTo(EventType.PRINT_CASE_SELECTED);
    assertThat(actualRmEvent.getPayload().getPrintCaseSelected().getPackCode())
        .isEqualTo(PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_ENGLAND);
    assertThat(actualRmEvent.getPayload().getPrintCaseSelected().getCaseRef())
        .isEqualTo(fulfillmentCase.getCaseRef());
  }

  @Test
  public void testIndividualResponseFulfilmentRequestWithCaseMissingAtFirst()
      throws IOException, InterruptedException {
    caseRepository.deleteAll();
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundPrinterQueue);

    Case fulfillmentCase = easyRandom.nextObject(Case.class);
    caseRepository.saveAndFlush(fulfillmentCase);

    UUID parentCaseId = UUID.randomUUID();
    UUID childCaseId = fulfillmentCase.getCaseId();
    ResponseManagementEvent actionFulfilmentEvent =
        getResponseManagementEvent(parentCaseId, PRINT_INDIVIDUAL_QUESTIONNAIRE_REQUEST_ENGLAND);
    actionFulfilmentEvent.getPayload().getFulfilmentRequest().setIndividualCaseId(childCaseId);

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

    rabbitQueueHelper.sendMessage(
        eventsExchange, EVENTS_FULFILMENT_REQUEST_BINDING, actionFulfilmentEvent);

    rabbitQueueHelper.checkNoMessagesSent(outputQueue);
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

  private ResponseManagementEvent getResponseManagementEvent(UUID caseId, String fulfilmentCode) {
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();

    FulfilmentRequestDTO fulfilmentRequest = easyRandom.nextObject(FulfilmentRequestDTO.class);
    fulfilmentRequest.setFulfilmentCode(fulfilmentCode);
    responseManagementEvent.setPayload(new Payload());
    fulfilmentRequest.setCaseId(caseId);
    responseManagementEvent.getPayload().setFulfilmentRequest(fulfilmentRequest);

    return responseManagementEvent;
  }

  private Case setUpCaseAndSaveInDB() {
    Case fulfilmentCase = easyRandom.nextObject(Case.class);
    caseRepository.saveAndFlush(fulfilmentCase);
    return fulfilmentCase;
  }
}
