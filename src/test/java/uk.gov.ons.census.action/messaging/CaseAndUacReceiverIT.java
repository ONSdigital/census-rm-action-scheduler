package uk.gov.ons.census.action.messaging;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
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
import uk.gov.ons.census.action.model.dto.Contact;
import uk.gov.ons.census.action.model.dto.EventType;
import uk.gov.ons.census.action.model.dto.FieldworkFollowup;
import uk.gov.ons.census.action.model.dto.FulfilmentRequestDTO;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.dto.Uac;
import uk.gov.ons.census.action.model.dto.UacQidDTO;
import uk.gov.ons.census.action.model.entity.ActionPlan;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.repository.ActionPlanRepository;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class CaseAndUacReceiverIT {
  private static final int DELAY_ACTION_BY_SECONDS = 5;

  @Value("${queueconfig.inbound-queue}")
  private String inboundQueue;

  @Value("${queueconfig.outbound-printer-queue}")
  private String outboundPrinterQueue;

  @Value("${queueconfig.outbound-field-queue}")
  private String outboundFieldQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;
  @Autowired private CaseRepository caseRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;
  @Autowired private ActionRuleRepository actionRuleRepository;
  @Autowired private ActionPlanRepository actionPlanRepository;
  private EasyRandom easyRandom = new EasyRandom();

  private final HashMap<String, String> actionTypeToPackCodeMap =
      new HashMap<>() {
        {
          put("ICHHQE", "P_IC_H1");
          put("ICHHQW", "P_IC_H2");
          put("ICHHQN", "P_IC_H4");
          put("ICL1E", "P_IC_ICL1");
          put("ICL2W", "P_IC_ICL2B");
          put("ICL4N", "P_IC_ICL4");
        }
      };

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(inboundQueue);
    rabbitQueueHelper.purgeQueue(outboundPrinterQueue);
    uacQidLinkRepository.deleteAllInBatch();
    caseRepository.deleteAllInBatch();
    actionRuleRepository.deleteAllInBatch();
    actionPlanRepository.deleteAll();
    actionRuleRepository.deleteAll();
    actionPlanRepository.deleteAllInBatch();
  }

  @Test
  public void checkReceivedEventsAreEmitted() throws InterruptedException, IOException {
    // Given
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundPrinterQueue);

    ActionPlan actionPlan = setUpActionPlan("happy", "path");
    actionPlanRepository.saveAndFlush(actionPlan);
    ActionRule actionRule = setUpActionRule(actionPlan);
    actionRuleRepository.saveAndFlush(actionRule);

    ResponseManagementEvent caseCreatedEvent =
        getResponseManagementEvent(actionPlan.getId().toString());
    caseCreatedEvent.getEvent().setType(EventType.CASE_CREATED);

    Uac uac = getUac(caseCreatedEvent);
    ResponseManagementEvent uacUpdatedEvent =
        getResponseManagementEvent(actionPlan.getId().toString());
    uacUpdatedEvent.getEvent().setType(EventType.UAC_UPDATED);
    uacUpdatedEvent.getPayload().setUac(uac);

    // WHEN
    rabbitQueueHelper.sendMessage(inboundQueue, caseCreatedEvent);
    rabbitQueueHelper.sendMessage(inboundQueue, uacUpdatedEvent);

    // THEN
    PrintFileDto printFileDto =
        rabbitQueueHelper.checkExpectedMessageReceived(outputQueue, PrintFileDto.class);

    assertThat(printFileDto.getAddressLine1())
        .isEqualTo(
            caseCreatedEvent.getPayload().getCollectionCase().getAddress().getAddressLine1());
    assertThat(printFileDto.getPackCode())
        .isEqualTo(actionTypeToPackCodeMap.get(actionRule.getActionType().toString()));
  }

  @Test
  public void checkReceivedFulfilmentCreatedEventProcessed() throws InterruptedException, IOException {
    // Given
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundPrinterQueue);

    ActionPlan actionPlan = setUpActionPlan("happy", "path");

    ResponseManagementEvent caseCreatedEvent =
            getResponseManagementEvent(actionPlan.getId().toString());
    caseCreatedEvent.getEvent().setType(EventType.CASE_CREATED);

    FulfilmentRequestDTO fulfilmentRequestDTO = easyRandom.nextObject(FulfilmentRequestDTO.class);
    fulfilmentRequestDTO.setFulfilmentCode("P_OR_I1");
    fulfilmentRequestDTO.setCaseId(UUID.fromString(caseCreatedEvent.getPayload().getCollectionCase().getId()));
    caseCreatedEvent.getPayload().setFulfilmentRequest(fulfilmentRequestDTO);

    // WHEN
    rabbitQueueHelper.sendMessage(inboundQueue, caseCreatedEvent);

    // THEN
    PrintFileDto printFileDto =
            rabbitQueueHelper.checkExpectedMessageReceived(outputQueue, PrintFileDto.class);

    assertThat(printFileDto.getAddressLine1())
            .isEqualTo(
                    caseCreatedEvent.getPayload().getCollectionCase().getAddress().getAddressLine1());
  }

  @Test
  public void checkFieldEventsAreEmitted() throws InterruptedException, IOException {
    // Given
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundFieldQueue);

    ActionPlan actionPlan = setUpActionPlan("happy", "path");
    actionPlanRepository.saveAndFlush(actionPlan);
    ActionRule actionRule = setUpFieldActionRule(actionPlan);
    actionRuleRepository.saveAndFlush(actionRule);

    ResponseManagementEvent caseCreatedEvent =
        getResponseManagementEvent(actionPlan.getId().toString());
    caseCreatedEvent.getEvent().setType(EventType.CASE_CREATED);
    caseCreatedEvent.getPayload().getCollectionCase().setCeExpectedCapacity("666");

    Uac uac = getUac(caseCreatedEvent);
    ResponseManagementEvent uacUpdatedEvent =
        getResponseManagementEvent(actionPlan.getId().toString());
    uacUpdatedEvent.getEvent().setType(EventType.UAC_UPDATED);
    uacUpdatedEvent.getPayload().setUac(uac);

    // WHEN
    rabbitQueueHelper.sendMessage(inboundQueue, caseCreatedEvent);
    rabbitQueueHelper.sendMessage(inboundQueue, uacUpdatedEvent);

    // THEN
    FieldworkFollowup followup =
        rabbitQueueHelper.checkExpectedMessageReceived(outputQueue, FieldworkFollowup.class);

    assertThat(followup.getActionPlan()).isEqualTo(actionPlan.getId().toString());
    assertThat(followup.getCaseId())
        .isEqualTo(caseCreatedEvent.getPayload().getCollectionCase().getId());
    assertThat(followup.getCaseRef())
        .isEqualTo(caseCreatedEvent.getPayload().getCollectionCase().getCaseRef());
  }

  @Test
  public void checkFieldEventsAreNotEmittedForAddressInvalid() throws InterruptedException {
    // Given
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundFieldQueue);

    ActionPlan actionPlan = setUpActionPlan("happy", "path");
    actionPlanRepository.saveAndFlush(actionPlan);
    ActionRule actionRule = setUpFieldActionRule(actionPlan);
    actionRuleRepository.saveAndFlush(actionRule);

    ResponseManagementEvent caseCreatedEvent =
        getResponseManagementEvent(actionPlan.getId().toString());
    caseCreatedEvent.getEvent().setType(EventType.CASE_CREATED);
    caseCreatedEvent.getPayload().getCollectionCase().setCeExpectedCapacity("666");
    caseCreatedEvent.getPayload().getCollectionCase().setAddressInvalid(true);

    Uac uac = getUac(caseCreatedEvent);
    ResponseManagementEvent uacUpdatedEvent =
        getResponseManagementEvent(actionPlan.getId().toString());
    uacUpdatedEvent.getEvent().setType(EventType.UAC_UPDATED);
    uacUpdatedEvent.getPayload().setUac(uac);

    // WHEN
    rabbitQueueHelper.sendMessage(inboundQueue, caseCreatedEvent);
    rabbitQueueHelper.sendMessage(inboundQueue, uacUpdatedEvent);

    // THEN
    rabbitQueueHelper.checkNoMessagesSent(outputQueue);
  }

  @Test
  public void checkCaseWithNoLinkedUACQuidDoesntSendThenWorksWhenUACAdded()
      throws InterruptedException, IOException {
    // Given
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundPrinterQueue);

    ActionPlan actionPlan = setUpActionPlan("no Quid", "or links");
    actionPlanRepository.saveAndFlush(actionPlan);
    ActionRule actionRule = setUpActionRule(actionPlan);
    actionRuleRepository.saveAndFlush(actionRule);

    ResponseManagementEvent caseCreatedEvent =
        getResponseManagementEvent(actionPlan.getId().toString());
    caseCreatedEvent.getEvent().setType(EventType.CASE_CREATED);

    // WHEN
    rabbitQueueHelper.sendMessage(inboundQueue, caseCreatedEvent);

    // THEN
    rabbitQueueHelper.checkNoMessagesSent(outputQueue);

    actionRule = actionRuleRepository.findById(actionRule.getId()).get();
    assertThat(actionRule.getHasTriggered()).isEqualTo(false);

    // Now add the UAC and check that it then runs successfully
    Uac uac = getUac(caseCreatedEvent);
    ResponseManagementEvent uacUpdatedEvent =
        getResponseManagementEvent(actionPlan.getId().toString());
    uacUpdatedEvent.getEvent().setType(EventType.UAC_UPDATED);
    uacUpdatedEvent.getPayload().setUac(uac);
    rabbitQueueHelper.sendMessage(inboundQueue, uacUpdatedEvent);

    // THEN
    PrintFileDto printFileDto =
        rabbitQueueHelper.checkExpectedMessageReceived(outputQueue, PrintFileDto.class);

    actionRule = actionRuleRepository.findById(actionRule.getId()).get();

    assertThat(printFileDto.getAddressLine1())
        .isEqualTo(
            caseCreatedEvent.getPayload().getCollectionCase().getAddress().getAddressLine1());
    assertThat(printFileDto.getPackCode())
        .isEqualTo(actionTypeToPackCodeMap.get(actionRule.getActionType().toString()));

    assertThat(actionRule.getHasTriggered()).isEqualTo(true);
  }

  @Test
  public void checkNoMessagesSentWhenTransactionRollback() throws InterruptedException {
    // Given
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundPrinterQueue);

    ActionPlan actionPlan = setUpActionPlan("no Quid", "or links");
    actionPlanRepository.saveAndFlush(actionPlan);
    ActionRule actionRule = setUpActionRule(actionPlan);
    actionRuleRepository.saveAndFlush(actionRule);

    ResponseManagementEvent caseCreatedEvent =
        getResponseManagementEvent(actionPlan.getId().toString());
    caseCreatedEvent.getEvent().setType(EventType.CASE_CREATED);
    Uac uac = getUac(caseCreatedEvent);
    ResponseManagementEvent uacUpdatedEvent =
        getResponseManagementEvent(actionPlan.getId().toString());
    uacUpdatedEvent.getEvent().setType(EventType.UAC_UPDATED);
    uacUpdatedEvent.getPayload().setUac(uac);

    ResponseManagementEvent badCaseCreatedEvent =
        getResponseManagementEvent(actionPlan.getId().toString());
    badCaseCreatedEvent.getEvent().setType(EventType.CASE_CREATED);

    // WHEN
    rabbitQueueHelper.sendMessage(inboundQueue, caseCreatedEvent);
    rabbitQueueHelper.sendMessage(inboundQueue, uacUpdatedEvent);
    rabbitQueueHelper.sendMessage(inboundQueue, badCaseCreatedEvent);

    // THEN
    rabbitQueueHelper.checkNoMessagesSent(outputQueue);
  }

  private Uac getUac(ResponseManagementEvent caseCreatedEvent) {
    Uac uac = easyRandom.nextObject(Uac.class);
    uac.setCaseId(caseCreatedEvent.getPayload().getCollectionCase().getId());
    return uac;
  }

  private ActionPlan setUpActionPlan(String name, String desc) {
    ActionPlan actionPlan = new ActionPlan();
    actionPlan.setName(name);
    actionPlan.setDescription(desc);
    actionPlan.setId(UUID.randomUUID());
    return actionPlan;
  }

  private ResponseManagementEvent getResponseManagementEvent(String actionPlanId) {
    ResponseManagementEvent responseManagementEvent =
        easyRandom.nextObject(ResponseManagementEvent.class);

    responseManagementEvent
        .getPayload()
        .getCollectionCase()
        .setCaseRef(Integer.toString(easyRandom.nextInt()));
    responseManagementEvent.getPayload().getCollectionCase().setId(UUID.randomUUID().toString());
    responseManagementEvent.getPayload().getCollectionCase().setState("ACTIONABLE");
    responseManagementEvent.getPayload().getCollectionCase().setActionPlanId(actionPlanId);

    Random random = new Random();
    responseManagementEvent
        .getPayload()
        .getCollectionCase()
        .getAddress()
        .setLatitude(Double.toString(random.nextDouble()));
    responseManagementEvent
        .getPayload()
        .getCollectionCase()
        .getAddress()
        .setLongitude(Double.toString(random.nextDouble()));

    responseManagementEvent.getPayload().getCollectionCase().setReceiptReceived(false);
    responseManagementEvent.getPayload().getCollectionCase().setRefusalReceived(false);
    responseManagementEvent.getPayload().getCollectionCase().setAddressInvalid(false);

    responseManagementEvent.getPayload().setFulfilmentRequest(null);

    return responseManagementEvent;
  }

  private ActionRule setUpActionRule(ActionPlan actionPlan) {
    ActionRule actionRule = new ActionRule();
    UUID actionRuleId = UUID.randomUUID();
    actionRule.setId(actionRuleId);
    actionRule.setTriggerDateTime(OffsetDateTime.now().plusSeconds(DELAY_ACTION_BY_SECONDS));
    actionRule.setHasTriggered(false);
    actionRule.setClassifiers(new HashMap<>());
    actionRule.setActionType(ActionType.ICL1E);
    actionRule.setActionPlan(actionPlan);

    return actionRule;
  }

  private ActionRule setUpFieldActionRule(ActionPlan actionPlan) {
    ActionRule actionRule = new ActionRule();
    UUID actionRuleId = UUID.randomUUID();
    actionRule.setId(actionRuleId);
    actionRule.setTriggerDateTime(OffsetDateTime.now().plusSeconds(DELAY_ACTION_BY_SECONDS));
    actionRule.setHasTriggered(false);
    actionRule.setClassifiers(new HashMap<>());
    actionRule.setActionType(ActionType.FF2QE);
    actionRule.setActionPlan(actionPlan);

    return actionRule;
  }
}
