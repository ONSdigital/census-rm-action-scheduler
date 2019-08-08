package uk.gov.ons.census.action.messaging;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
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
public class ActionFulfilmentIT {

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
  public void checkReceivedEventsAreEmitted() throws InterruptedException, IOException {
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundPrinterQueue);
    Case fulfillmentCase = this.setUpCase();
    // Given
    ResponseManagementEvent actionFulfilmentEvent =
        getResponseManagementEvent(fulfillmentCase.getCaseId().toString());
    PrintFileDto printFileRequest = setupPrintFile();
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

    // WHEN
    rabbitQueueHelper.sendMessage(
        eventsExchange, eventsFulfilmentRequestBinding, actionFulfilmentEvent);

    PrintFileDto actualPrintFileDto = checkExpectedPrintFileDtoMessageReceived(outputQueue);

    assertThat(actualPrintFileDto.getUac()).isEqualTo(uacQidDto.getUac());
  }

  private PrintFileDto checkExpectedPrintFileDtoMessageReceived(BlockingQueue<String> queue)
      throws InterruptedException, IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    String actualMessage = queue.poll(20, TimeUnit.SECONDS);
    assertNotNull("Did not receive message before timeout", actualMessage);

    return objectMapper.readValue(actualMessage, PrintFileDto.class);
  }

  private ResponseManagementEvent getResponseManagementEvent(String caseId) {
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();

    FulfilmentRequestDTO fulfilmentRequest = easyRandom.nextObject(FulfilmentRequestDTO.class);
    fulfilmentRequest.setFulfilmentCode("P_OR_H1");
    responseManagementEvent.setPayload(new Payload());
    fulfilmentRequest.setCaseId(caseId);
    responseManagementEvent.getPayload().setFulfilmentRequest(fulfilmentRequest);

    return responseManagementEvent;
  }

  private PrintFileDto setupPrintFile() {
    return easyRandom.nextObject(PrintFileDto.class);
  }

  private Case setUpCase() {
    Case fulfilmentCase = easyRandom.nextObject(Case.class);
    caseRepository.saveAndFlush(fulfilmentCase);
    return fulfilmentCase;
  }
}
