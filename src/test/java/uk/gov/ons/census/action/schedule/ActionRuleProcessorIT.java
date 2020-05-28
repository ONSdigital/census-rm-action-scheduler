package uk.gov.ons.census.action.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.dto.RefusalType;
import uk.gov.ons.census.action.model.entity.ActionPlan;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.CaseToProcess;
import uk.gov.ons.census.action.model.repository.ActionPlanRepository;
import uk.gov.ons.census.action.model.repository.ActionRuleRepository;
import uk.gov.ons.census.action.model.repository.CaseRepository;
import uk.gov.ons.census.action.model.repository.CaseToProcessRepository;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class ActionRuleProcessorIT {

  @Autowired private CaseRepository caseRepository;
  @Autowired private ActionRuleRepository actionRuleRepository;
  @Autowired private ActionPlanRepository actionPlanRepository;
  @Autowired private CaseToProcessRepository caseToProcessRepository;

  private static final EasyRandom easyRandom = new EasyRandom();

  @Before
  @Transactional
  public void setUp() {
    caseToProcessRepository.deleteAllInBatch();
    caseRepository.deleteAllInBatch();
    actionRuleRepository.deleteAllInBatch();
    actionPlanRepository.deleteAll();
    actionRuleRepository.deleteAll();
    actionPlanRepository.deleteAllInBatch();
  }

  @Test
  public void testPrinterRule() throws InterruptedException {
    // Given
    ActionPlan actionPlan = setUpActionPlan();
    Case randomCase = setUpCase(actionPlan);
    setUpActionRule(ActionType.P_QU_H1, actionPlan);

    // When
    Thread.sleep(2000);

    // Then
    List<CaseToProcess> queuedCases = caseToProcessRepository.findAll();
    assertThat(queuedCases.size()).isEqualTo(1);
    assertThat(queuedCases.get(0).getActionRule().getActionType()).isEqualTo(ActionType.P_QU_H1);
    assertThat(queuedCases.get(0).getCaze()).isEqualTo(randomCase);
  }

  @Test
  public void testMultipleCaseRule() throws InterruptedException {
    // Given
    ActionPlan actionPlan = setUpActionPlan();
    for (int i = 0; i < 10; i++) {
      setUpCase(actionPlan);
    }
    setUpActionRule(ActionType.P_QU_H1, actionPlan);

    // When
    Thread.sleep(2000);

    // Then
    List<CaseToProcess> queuedCases = caseToProcessRepository.findAll();
    assertThat(queuedCases.size()).isEqualTo(10);
  }

  @Test
  public void testFieldRule() throws InterruptedException {
    // Given
    ActionPlan actionPlan = setUpActionPlan();
    Case randomCase = setUpCase(actionPlan);
    setUpActionRule(ActionType.FIELD, actionPlan);

    // When
    Thread.sleep(2000);

    // Then
    List<CaseToProcess> queuedCases = caseToProcessRepository.findAll();
    assertThat(queuedCases.size()).isEqualTo(1);
    assertThat(queuedCases.get(0).getActionRule().getActionType()).isEqualTo(ActionType.FIELD);
    assertThat(queuedCases.get(0).getCaze()).isEqualTo(randomCase);
  }

  @Test
  public void testIndividualCaseReminderNotSent() throws InterruptedException {
    // Given we have an HI case with a valid Treatment Code.
    ActionPlan actionPlan = setUpActionPlan();
    setUpIndividualCase(actionPlan);
    setUpActionRule(ActionType.P_QU_H2, actionPlan);

    // When
    Thread.sleep(2000);

    // Then
    List<CaseToProcess> queuedCases = caseToProcessRepository.findAll();
    assertThat(queuedCases.size()).isEqualTo(0);
  }

  @Test
  public void testCeEstabCase() throws InterruptedException {
    // Given we have an HI case with a valid Treatment Code.
    ActionPlan actionPlan = setUpActionPlan();
    setUpCeEstabCase(actionPlan, 23);
    setUpCeEstabCase(actionPlan, 77);
    setUpCeEstabCase(actionPlan, null);
    setUpActionRule(ActionType.CE_IC03, actionPlan);

    // When
    Thread.sleep(2000);

    // Then
    List<CaseToProcess> queuedCases = caseToProcessRepository.findAll();
    assertThat(queuedCases.size()).isEqualTo(3);
    assertThat(queuedCases.get(0).getBatchQuantity()).isEqualTo(100);
  }

  @Test
  public void testCaseDoesNotProcessWhenFlagPresent() throws InterruptedException {
    // Given
    ActionPlan actionPlan = setUpActionPlan();
    setUpCaseWithFlag(actionPlan, "receipt");
    setUpCaseWithFlag(actionPlan, "refusal");
    setUpCaseWithFlag(actionPlan, "invalid");
    setUpCaseWithFlag(actionPlan, "skeleton");
    setUpCase(actionPlan);
    setUpActionRule(ActionType.P_QU_H1, actionPlan);

    // When
    Thread.sleep(2000);

    // Then
    List<CaseToProcess> queuedCases = caseToProcessRepository.findAll();
    assertThat(queuedCases.size()).isEqualTo(1);
  }

  @Test
  public void testSetUpCasesWithPrinterRule() throws InterruptedException {
    // Given
    ActionPlan actionPlan = setUpActionPlan();
    Case unRefusedCase = setUpCase(actionPlan);
    Case hardRefusalCase = setUpCase(actionPlan);
    hardRefusalCase.setRefusalReceived(RefusalType.HARD_REFUSAL.toString());
    caseRepository.saveAndFlush(hardRefusalCase);
    Case extraordinaryRefusalCase = setUpCase(actionPlan);
    extraordinaryRefusalCase.setRefusalReceived(RefusalType.EXTRAORDINARY_REFUSAL.toString());
    caseRepository.saveAndFlush(extraordinaryRefusalCase);
    setUpActionRule(ActionType.P_RL_1RL1_1, actionPlan);

    // When
    Thread.sleep(2000);

    // Then
    List<CaseToProcess> queuedCases = caseToProcessRepository.findAll();
    assertThat(queuedCases.size()).isEqualTo(2);
    List<UUID> caseIds =
        queuedCases.stream()
            .map(CaseToProcess::getCaze)
            .map(Case::getCaseId)
            .collect(Collectors.toList());
    assertThat(caseIds).containsOnly(unRefusedCase.getCaseId(), hardRefusalCase.getCaseId());
  }

  @Test
  public void testSetUpCasesWithFieldRule() throws InterruptedException {
    // Given
    ActionPlan actionPlan = setUpActionPlan();
    Case unRefusedCase = setUpCase(actionPlan);
    Case hardRefusalCase = setUpCase(actionPlan);
    hardRefusalCase.setRefusalReceived(RefusalType.HARD_REFUSAL.toString());
    caseRepository.saveAndFlush(hardRefusalCase);
    Case extraordinaryRefusalCase = setUpCase(actionPlan);
    extraordinaryRefusalCase.setRefusalReceived(RefusalType.EXTRAORDINARY_REFUSAL.toString());
    caseRepository.saveAndFlush(extraordinaryRefusalCase);
    setUpActionRule(ActionType.FIELD, actionPlan);

    // When
    Thread.sleep(2000);

    // Then
    List<CaseToProcess> queuedCases = caseToProcessRepository.findAll();
    assertThat(queuedCases.size()).isEqualTo(1);
    List<UUID> caseIds =
        queuedCases.stream()
            .map(CaseToProcess::getCaze)
            .map(Case::getCaseId)
            .collect(Collectors.toList());
    assertThat(caseIds).containsOnly(unRefusedCase.getCaseId());
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

    return actionRuleRepository.saveAndFlush(actionRule);
  }

  private Case setUpCase(ActionPlan actionPlan) {
    Case randomCase = easyRandom.nextObject(Case.class);
    randomCase.setActionPlanId(actionPlan.getId().toString());
    randomCase.setReceiptReceived(false);
    randomCase.setRefusalReceived(null);
    randomCase.setAddressInvalid(false);
    randomCase.setSkeleton(false);
    randomCase.setTreatmentCode("HH_LF2R1E");
    caseRepository.saveAndFlush(randomCase);
    return randomCase;
  }

  private void setUpIndividualCase(ActionPlan actionPlan) {
    Case randomCase = easyRandom.nextObject(Case.class);
    randomCase.setActionPlanId(actionPlan.getId().toString());
    randomCase.setReceiptReceived(false);
    randomCase.setRefusalReceived(null);
    randomCase.setAddressInvalid(false);
    randomCase.setSkeleton(false);
    randomCase.setTreatmentCode("HH_LF2R1E");
    randomCase.setCaseType("HI");
    caseRepository.saveAndFlush(randomCase);
  }

  private void setUpCeEstabCase(ActionPlan actionPlan, Integer ceExpectedCapacity) {
    Case randomCase = easyRandom.nextObject(Case.class);
    randomCase.setActionPlanId(actionPlan.getId().toString());
    randomCase.setReceiptReceived(false);
    randomCase.setRefusalReceived(null);
    randomCase.setAddressInvalid(false);
    randomCase.setSkeleton(false);
    randomCase.setTreatmentCode("CE_LDIEE");
    randomCase.setCaseType("CE");
    randomCase.setCeExpectedCapacity(ceExpectedCapacity);
    caseRepository.saveAndFlush(randomCase);
  }

  private Case setUpCaseWithFlag(ActionPlan actionPlan, String flag) {
    Case randomCase = easyRandom.nextObject(Case.class);
    randomCase.setActionPlanId(actionPlan.getId().toString());
    randomCase.setReceiptReceived(false);
    randomCase.setRefusalReceived(null);
    randomCase.setAddressInvalid(false);
    randomCase.setSkeleton(false);
    switch (flag) {
      case "receipt":
        randomCase.setReceiptReceived(true);
        break;
      case "refusal":
        randomCase.setRefusalReceived(RefusalType.EXTRAORDINARY_REFUSAL.toString());
        break;
      case "invalid":
        randomCase.setAddressInvalid(true);
        break;
      case "skeleton":
        randomCase.setSkeleton(true);
        break;
    }
    randomCase.setTreatmentCode("HH_LF2R1E");
    caseRepository.saveAndFlush(randomCase);
    return randomCase;
  }
}
