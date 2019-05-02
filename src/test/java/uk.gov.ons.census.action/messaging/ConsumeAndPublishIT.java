package uk.gov.ons.census.action.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.dto.EventType;
import uk.gov.ons.census.action.model.dto.ResponseManagementEvent;
import uk.gov.ons.census.action.model.dto.Uac;
import uk.gov.ons.census.action.model.dto.instruction.ActionInstruction;
import uk.gov.ons.census.action.model.entity.ActionPlan;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.ActionPlanRepository;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertNotNull;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
public class EventReceiverIT {
  private static final int DELAY_ACTION_BY_SECONDS = 5;

  @Value("${queueconfig.inbound-queue}")
  private String inboundQueue;

  @Value("${queueconfig.outbound-printer-queue}")
  private String outboundPrinterQueue;

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
  public void checkSentEventsArePersistedAndEmitted() throws IOException, InterruptedException, JAXBException {
    //Given
    BlockingQueue<String> outputQueue = rabbitQueueHelper.listen(outboundPrinterQueue);

    ActionPlan actionPlan = setUpActionPlan();
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

    assertThat(actionInstruction.getActionRequest().getActionPlan()).isEqualTo(actionPlan.getId().toString());
    assertThat(actionInstruction.getActionRequest().getCaseId())
            .isEqualTo(caseCreatedEvent.getPayload().getCollectionCase().getId());
    assertThat(actionInstruction.getActionRequest().getCaseRef())
            .isEqualTo(caseCreatedEvent.getPayload().getCollectionCase().getCaseRef());
  }

  private Uac getUac(ResponseManagementEvent caseCreatedEvent) {
    Uac uac = easyRandom.nextObject(Uac.class);
    uac.setCaseId(caseCreatedEvent.getPayload().getCollectionCase().getId());
    return uac;
  }

  private ActionPlan setUpActionPlan() {
    ActionPlan actionPlan = new ActionPlan();
    actionPlan.setDescription("Testing testing");
    actionPlan.setName("Test");
    actionPlan.setId(UUID.randomUUID());
    return actionPlan;
  }

  private ActionInstruction checkExpectedMessageReceived(BlockingQueue<String> queue)
          throws IOException, InterruptedException, JAXBException {
    String actualMessage = queue.poll(10, TimeUnit.SECONDS);
    assertNotNull("Did not receive message before timeout", actualMessage);

    JAXBContext jaxbContext = JAXBContext.newInstance(ActionInstruction.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

    StringReader reader = new StringReader(actualMessage);
    return  (ActionInstruction) unmarshaller.unmarshal(reader);
  }

  private ResponseManagementEvent getResponseManagementEvent(String actionPlanId) {
    ResponseManagementEvent responseManagementEvent =
        easyRandom.nextObject(ResponseManagementEvent.class);

    responseManagementEvent.getPayload().getCollectionCase().setCaseRef("123");
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

  private void setUpQidLinksForCases(List<Case> cases) {
    cases.forEach(
        caze -> {
          String uac = caze.getCaseId().toString() + "uac";
          UacQidLink uacQidLink = new UacQidLink();
          uacQidLink.setUac(uac);

          uacQidLinkRepository.save(uacQidLink);
        });
  }
}
