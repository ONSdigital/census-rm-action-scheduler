package uk.gov.ons.census.action.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static uk.gov.ons.census.action.model.dto.EventType.UNDELIVERED_MAIL_REPORTED;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.dto.Event;
import uk.gov.ons.census.action.model.dto.FieldworkFollowup;
import uk.gov.ons.census.action.model.dto.FulfilmentInformation;
import uk.gov.ons.census.action.model.dto.Payload;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class UndeliveredMailReceiverIT {
  private static final String OUTBOUND_FIELD_QUEUE = "Action.Field";

  @Value("${queueconfig.undelivered-mail-queue}")
  private String undeliveredMailQueue;

  @Value("${queueconfig.events-exchange}")
  private String eventsExchange;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;

  @Autowired private CaseRepository caseRepository;

  @Autowired private UacQidLinkRepository uacQidLinkRepository;

  private EasyRandom easyRandom = new EasyRandom();

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(undeliveredMailQueue);
    rabbitQueueHelper.purgeQueue(OUTBOUND_FIELD_QUEUE);
  }

  @Test
  public void testWithQid() throws InterruptedException, IOException {

    // Given
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(OUTBOUND_FIELD_QUEUE);
    Case caze = setUpCase();
    UacQidLink uacQidLink = setUpUacQidLink(caze);
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setEvent(new Event());
    responseManagementEvent.getEvent().setType(UNDELIVERED_MAIL_REPORTED);
    responseManagementEvent.setPayload(new Payload());
    responseManagementEvent.getPayload().setFulfilmentInformation(new FulfilmentInformation());
    responseManagementEvent
        .getPayload()
        .getFulfilmentInformation()
        .setQuestionnaireId(uacQidLink.getQid());

    // When
    rabbitQueueHelper.sendMessage(
        eventsExchange, "event.fulfilment.undelivered", responseManagementEvent);

    // Then
    FieldworkFollowup actualFieldworkFollowup =
        checkExpectedFieldworkFollowupMessageReceived(outputQueue);
    checkTheThings(actualFieldworkFollowup, caze);
  }

  @Test
  public void testWithCaseRef() throws InterruptedException, IOException {

    // Given
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(OUTBOUND_FIELD_QUEUE);
    Case caze = setUpCase();
    ResponseManagementEvent responseManagementEvent = new ResponseManagementEvent();
    responseManagementEvent.setEvent(new Event());
    responseManagementEvent.getEvent().setType(UNDELIVERED_MAIL_REPORTED);
    responseManagementEvent.setPayload(new Payload());
    responseManagementEvent.getPayload().setFulfilmentInformation(new FulfilmentInformation());
    responseManagementEvent
        .getPayload()
        .getFulfilmentInformation()
        .setCaseRef(Integer.toString(caze.getCaseRef()));

    // When
    rabbitQueueHelper.sendMessage(
        eventsExchange, "event.fulfilment.undelivered", responseManagementEvent);

    // Then
    FieldworkFollowup actualFieldworkFollowup =
        checkExpectedFieldworkFollowupMessageReceived(outputQueue);
    checkTheThings(actualFieldworkFollowup, caze);
  }

  private FieldworkFollowup checkExpectedFieldworkFollowupMessageReceived(
      BlockingQueue<String> queue) throws InterruptedException, IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    String actualMessage = queue.poll(20, TimeUnit.SECONDS);
    assertNotNull("Did not receive message before timeout", actualMessage);

    return objectMapper.readValue(actualMessage, FieldworkFollowup.class);
  }

  private void checkTheThings(FieldworkFollowup actual, Case expected) {
    assertThat(actual.getCaseId()).isEqualTo(expected.getCaseId().toString());
    assertThat(actual.getCaseRef()).isEqualTo(Integer.toString(expected.getCaseRef()));
    assertThat(actual.getActionPlan()).isEqualTo("dummy");
    assertThat(actual.getActionType()).isEqualTo("dummy");
    assertThat(actual.getAddressLine1()).isEqualTo(expected.getAddressLine1());
    assertThat(actual.getUndeliveredAsAddress()).isTrue();
  }

  private Case setUpCase() {
    Case caze = easyRandom.nextObject(Case.class);
    caze.setAddressInvalid(false);
    caze.setRefusalReceived(false);
    caze.setReceiptReceived(false);
    return caseRepository.saveAndFlush(caze);
  }

  private UacQidLink setUpUacQidLink(Case caze) {
    UacQidLink uacQidLink = easyRandom.nextObject(UacQidLink.class);
    uacQidLink.setCaseId(caze.getCaseId().toString());
    return uacQidLinkRepository.saveAndFlush(uacQidLink);
  }
}
