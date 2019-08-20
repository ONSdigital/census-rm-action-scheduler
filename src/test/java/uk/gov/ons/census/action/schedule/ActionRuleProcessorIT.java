package uk.gov.ons.census.action.schedule;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.jeasy.random.EasyRandom;
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
import uk.gov.ons.census.action.messaging.RabbitQueueHelper;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.dto.UacQidDTO;
import uk.gov.ons.census.action.model.entity.ActionPlan;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.repository.ActionPlanRepository;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class ActionRuleProcessorIT {

  private static final int DELAY_ACTION_BY_SECONDS = 5;

  @Value("${queueconfig.outbound-printer-queue}")
  private String outboundPrinterQueue;

  @Autowired private RabbitQueueHelper rabbitQueueHelper;
  @Autowired private CaseRepository caseRepository;
  @Autowired private UacQidLinkRepository uacQidLinkRepository;
  @Autowired private ActionRuleRepository actionRuleRepository;
  @Autowired private ActionPlanRepository actionPlanRepository;
  @Autowired private ActionRuleProcessor actionRuleProcessor;
  private static final EasyRandom easyRandom = new EasyRandom();

  @Value("${caseapi.port}")
  private int caseApiPort;

  @Value("${caseapi.host}")
  private String caseApiHost;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Rule
  public WireMockRule mockCaseApi = new WireMockRule(wireMockConfig().port(8089).httpsPort(8443));

  @Test
  public void testReminderLetterActionCreatesNewUac() throws IOException, InterruptedException {
    // Given
    UacQidDTO uacQidDto = easyRandom.nextObject(UacQidDTO.class);
    String returnJson = objectMapper.writeValueAsString(uacQidDto);
    String uacCreateUrl = "/uacqid/create/";
    stubFor(
        post(urlEqualTo(uacCreateUrl))
            .willReturn(
                aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBody(returnJson)));
    BlockingQueue<String> printerQueue = rabbitQueueHelper.listen(outboundPrinterQueue);
    ActionPlan actionPlan = setUpActionPlan();
    Case randomCase = setUpCase(actionPlan);
    ActionRule actionRule = setUpActionRule(ActionType.P_RL_1RL1_1, actionPlan);

    // When the action plan triggers
    String actualMessage = printerQueue.poll(20, TimeUnit.SECONDS);

    // Then
    assertThat(actualMessage).isNotNull();
    PrintFileDto actualPrintFileDto = objectMapper.readValue(actualMessage, PrintFileDto.class);
    verify(exactly(1), postRequestedFor(urlEqualTo(uacCreateUrl)));

    assertThat(actualPrintFileDto.getActionType()).isEqualTo(ActionType.P_RL_1RL1_1.name());
    assertThat(actualPrintFileDto.getPackCode()).isEqualTo(ActionType.P_RL_1RL1_1.name());
    assertThat(actualPrintFileDto).isEqualToComparingOnlyGivenFields(uacQidDto, "uac", "qid");
    assertThat(actualPrintFileDto)
        .isEqualToIgnoringGivenFields(
            randomCase,
            "uac",
            "qid",
            "uacWales",
            "qidWales",
            "title",
            "forename",
            "surname",
            "batchId",
            "batchQuantity",
            "packCode",
            "actionType");
  }

  private ActionPlan setUpActionPlan() {
    ActionPlan actionPlan = new ActionPlan();
    actionPlan.setId(UUID.randomUUID());
    actionPlan.setDescription("Test Reminder Letters");
    actionPlan.setName("Test Reminder Letters");
    actionPlan.setActionRules(null);
    actionPlanRepository.saveAndFlush(actionPlan);
    return actionPlan;
  }

  private ActionRule setUpActionRule(ActionType actionType, ActionPlan actionPlan) {
    ActionRule actionRule = new ActionRule();
    actionRule.setId(UUID.randomUUID());
    actionRule.setTriggerDateTime(OffsetDateTime.now());
    actionRule.setHasTriggered(false);
    actionRule.setActionType(actionType);
    actionRule.setActionPlan(actionPlan);

    Map<String, List<String>> classifiers = new HashMap<>();
    actionRule.setClassifiers(classifiers);

    actionRuleRepository.saveAndFlush(actionRule);
    return actionRule;
  }

  private Case setUpCase(ActionPlan actionPlan) {
    Case randomCase = easyRandom.nextObject(Case.class);
    randomCase.setActionPlanId(actionPlan.getId().toString());
    randomCase.setReceiptReceived(false);
    randomCase.setRefusalReceived(false);
    randomCase.setTreatmentCode("HH_LF2R1E");
    caseRepository.saveAndFlush(randomCase);
    return randomCase;
  }
}
