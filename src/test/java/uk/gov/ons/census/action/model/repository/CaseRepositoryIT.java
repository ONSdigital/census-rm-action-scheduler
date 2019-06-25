package uk.gov.ons.census.action.model.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.jpa.domain.Specification.where;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.criteria.CriteriaBuilder;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.entity.Case;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class CaseRepositoryIT {

  private static final String TEST_ACTION_PLAN_ID;
  private static final Map<String, List<String>> TEST_CLASSIFIERS;
  private EasyRandom easyRandom = new EasyRandom();

  @Autowired private CaseRepository caseRepository;

  static {
    TEST_ACTION_PLAN_ID = "ddbd26c2-15af-4055-aab9-591b3735b8d3";

    TEST_CLASSIFIERS = new HashMap<>();
    TEST_CLASSIFIERS.put(
        "treatmentCode",
        Arrays.asList(
            "HH_LF2R3AE",
            "HH_LF3R3BE",
            "HH_LF3R3AE",
            "HH_LF2R1E",
            "HH_LFNR2E",
            "HH_LFNR3AE",
            "HH_LF2R3BE",
            "HH_LF3R2E",
            "HH_LF2R2E",
            "HH_LF3R1E"));
  }

  @Transactional
  @Test
  public void shouldRetrieveTenCasesWhenNoneReceiptedAndWithoutClassifiers() {
    int expectedUnreceiptedCaseSize = 10;
    setupTestCases(expectedUnreceiptedCaseSize, false);

    Specification<Case> expectedSpecification = getSpecificationWithoutClassifiers();

    List<Case> cases = caseRepository.findAll(expectedSpecification);

    assertThat(cases.size()).isEqualTo(expectedUnreceiptedCaseSize);
  }

  @Transactional
  @Test
  public void shouldRetrieveSevenCasesWhenThreeReceiptedAndWithoutClassifiers() {
    int expectedUnreceiptedCaseSize = 7;
    setupTestCases(expectedUnreceiptedCaseSize, false);
    setupTestCases(3, true);

    Specification<Case> expectedSpecification = getSpecificationWithoutClassifiers();

    List<Case> cases = caseRepository.findAll(expectedSpecification);

    assertThat(cases.size()).isEqualTo(expectedUnreceiptedCaseSize);
  }

  @Transactional
  @Test
  public void shouldRetrieveTenCasesWhenZeroReceiptedAndWithClassifiers() {
    int expectedUnreceiptedCaseSize = 10;
    setupTestCases(expectedUnreceiptedCaseSize, false);

    Specification<Case> expectedSpecification = getSpecificationWithClassifiers();

    List<Case> cases = caseRepository.findAll(expectedSpecification);

    assertThat(cases.size()).isEqualTo(expectedUnreceiptedCaseSize);
  }

  @Transactional
  @Test
  public void shouldRetrieveSevenCasesWhenThreeReceiptedAndWithClassifiers() {
    int expectedUnreceiptedCaseSize = 7;
    setupTestCases(expectedUnreceiptedCaseSize, false);
    setupTestCases(3, true);

    Specification<Case> expectedSpecification = getSpecificationWithClassifiers();

    List<Case> cases = caseRepository.findAll(expectedSpecification);

    assertThat(cases.size()).isEqualTo(expectedUnreceiptedCaseSize);
  }

  private void setupTestCases(int caseCount, boolean receipted) {
    List<Case> unReceiptedCases = new ArrayList<>();

    for (int i = 0; i < caseCount; i++) {
      Case caze = easyRandom.nextObject(Case.class);
      caze.setActionPlanId(TEST_ACTION_PLAN_ID);
      caze.setReceiptReceived(receipted);
      unReceiptedCases.add(caze);
      caze.setTreatmentCode("HH_LF3R1E");
    }

    caseRepository.saveAll(unReceiptedCases);
  }

  private Specification<Case> getSpecificationWithoutClassifiers() {
    return createSpecificationForUnreceiptedCases();
  }

  private Specification<Case> getSpecificationWithClassifiers() {
    Specification<Case> specification = createSpecificationForUnreceiptedCases();

    for (Map.Entry<String, List<String>> classifier : TEST_CLASSIFIERS.entrySet()) {
      specification = specification.and(isClassifierIn(classifier.getKey(), classifier.getValue()));
    }

    return specification;
  }

  private Specification<Case> createSpecificationForUnreceiptedCases() {
    return where(isActionPlanIdEqualTo()).and(excludeReceiptedCases());
  }

  private Specification<Case> isActionPlanIdEqualTo() {
    return (Specification<Case>)
        (root, query, builder) -> builder.equal(root.get("actionPlanId"), TEST_ACTION_PLAN_ID);
  }

  private Specification<Case> excludeReceiptedCases() {
    return (Specification<Case>)
        (root, query, builder) -> builder.equal(root.get("receiptReceived"), false);
  }

  private Specification<Case> isClassifierIn(
      final String fieldName, final List<String> inClauseValues) {
    return (Specification<Case>)
        (root, query, builder) -> {
          CriteriaBuilder.In<String> inClause = builder.in(root.get(fieldName));
          for (String inClauseValue : inClauseValues) {
            inClause.value(inClauseValue);
          }
          return inClause;
        };
  }
}
