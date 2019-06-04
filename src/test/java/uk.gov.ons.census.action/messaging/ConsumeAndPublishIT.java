package uk.gov.ons.census.action.messaging;

import static junit.framework.TestCase.assertNull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertNotNull;

import java.io.StringReader;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
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
import uk.gov.ons.census.action.model.dto.EventType;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.dto.Uac;
import uk.gov.ons.census.action.model.dto.instruction.printer.ActionInstruction;
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
public class ConsumeAndPublishIT {
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

  @Before
  @Transactional
  public void setUp() {
    rabbitQueueHelper.purgeQueue(inboundQueue);
    rabbitQueueHelper.purgeQueue(outboundPrinterQueue);
    uacQidLinkRepository.deleteAllInBatch();
    caseRepository.deleteAllInBatch();
    actionRuleRepository.deleteAllInBatch();
    actionPlanRepository.deleteAllInBatch();
  }

  @Test
  public void checkRecievedEventsAreEmitted() throws InterruptedException, JAXBException {
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
    ActionInstruction actionInstruction = checkExpectedMessageReceived(outputQueue);

    assertThat(actionInstruction.getActionRequest().getActionPlan())
        .isEqualTo(actionPlan.getId().toString());
    assertThat(actionInstruction.getActionRequest().getCaseId())
        .isEqualTo(caseCreatedEvent.getPayload().getCollectionCase().getId());
    assertThat(actionInstruction.getActionRequest().getCaseRef())
        .isEqualTo(caseCreatedEvent.getPayload().getCollectionCase().getCaseRef());
  }

  @Test
  public void checkFieldEventsAreEmitted() throws InterruptedException, JAXBException {
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
    uk.gov.ons.census.action.model.dto.instruction.field.ActionInstruction actionInstruction =
        checkExpectedFieldMessageReceived(outputQueue);

    assertThat(actionInstruction.getActionRequest().getActionPlan())
        .isEqualTo(actionPlan.getId().toString());
    assertThat(actionInstruction.getActionRequest().getCaseId())
        .isEqualTo(caseCreatedEvent.getPayload().getCollectionCase().getId());
    assertThat(actionInstruction.getActionRequest().getCaseRef())
        .isEqualTo(caseCreatedEvent.getPayload().getCollectionCase().getCaseRef());
  }

  @Test
  public void checkCaseWithNoLinkedUACQuidDoesntSendThenWorksWhenUACAdded()
      throws InterruptedException, JAXBException {
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
    checkExpectedMessageNotReceived(outputQueue);

    // Now add the UAC and check that it then runs successfully
    Uac uac = getUac(caseCreatedEvent);
    ResponseManagementEvent uacUpdatedEvent =
        getResponseManagementEvent(actionPlan.getId().toString());
    uacUpdatedEvent.getEvent().setType(EventType.UAC_UPDATED);
    uacUpdatedEvent.getPayload().setUac(uac);
    rabbitQueueHelper.sendMessage(inboundQueue, uacUpdatedEvent);

    // THEN
    ActionInstruction actionInstruction = checkExpectedMessageReceived(outputQueue);

    assertThat(actionInstruction.getActionRequest().getActionPlan())
        .isEqualTo(actionPlan.getId().toString());
    assertThat(actionInstruction.getActionRequest().getCaseId())
        .isEqualTo(caseCreatedEvent.getPayload().getCollectionCase().getId());
    assertThat(actionInstruction.getActionRequest().getCaseRef())
        .isEqualTo(caseCreatedEvent.getPayload().getCollectionCase().getCaseRef());
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
    rabbitQueueHelper.sendMessage(inboundQueue, badCaseCreatedEvent);
    rabbitQueueHelper.sendMessage(inboundQueue, caseCreatedEvent);
    rabbitQueueHelper.sendMessage(inboundQueue, uacUpdatedEvent);

    // THEN
    checkExpectedMessageNotReceived(outputQueue);
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

  private void checkExpectedMessageNotReceived(BlockingQueue<String> queue)
      throws InterruptedException {
    String actualMessage = queue.poll(10, TimeUnit.SECONDS);
    assertNull("Recieved Message, expected none", actualMessage);
  }

  private ActionInstruction checkExpectedMessageReceived(BlockingQueue<String> queue)
      throws InterruptedException, JAXBException {
    String actualMessage = queue.poll(10, TimeUnit.SECONDS);
    assertNotNull("Did not receive message before timeout", actualMessage);

    JAXBContext jaxbContext = JAXBContext.newInstance(ActionInstruction.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

    StringReader reader = new StringReader(actualMessage);
    return (ActionInstruction) unmarshaller.unmarshal(reader);
  }

  private uk.gov.ons.census.action.model.dto.instruction.field.ActionInstruction
      checkExpectedFieldMessageReceived(BlockingQueue<String> queue)
          throws InterruptedException, JAXBException {
    String actualMessage = queue.poll(10, TimeUnit.SECONDS);
    assertNotNull("Did not receive message before timeout", actualMessage);

    JAXBContext jaxbContext =
        JAXBContext.newInstance(
            uk.gov.ons.census.action.model.dto.instruction.field.ActionInstruction.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

    StringReader reader = new StringReader(actualMessage);
    return (uk.gov.ons.census.action.model.dto.instruction.field.ActionInstruction)
        unmarshaller.unmarshal(reader);
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
