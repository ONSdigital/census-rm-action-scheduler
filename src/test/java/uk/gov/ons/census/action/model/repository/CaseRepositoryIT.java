package uk.gov.ons.census.action.model.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.jpa.domain.Specification.where;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.criteria.CriteriaBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.entity.Case;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class CaseRepositoryIT {

  private static final String TEST_ACTION_PLAN_ID;
  private static final Map<String, List<String>> TEST_CLASSIFIERS;

  @Autowired
  private CaseRepository caseRepository;

  static {
    TEST_ACTION_PLAN_ID = "ddbd26c2-15af-4055-aab9-591b3735b8d3";

    TEST_CLASSIFIERS = new HashMap<>();
    TEST_CLASSIFIERS.put("treatmentCode", Arrays
        .asList("HH_LF2R3AE", "HH_LF3R3BE", "HH_LF3R3AE", "HH_LF2R1E", "HH_LFNR2E", "HH_LFNR3AE", "HH_LF2R3BE",
            "HH_LF3R2E", "HH_LF2R2E", "HH_LF3R1E"));
  }

  @Transactional
  @Test
  @Sql(scripts = {"/data-test-10of10-unreceipted.sql"})
  public void shouldRetrieveTenCasesWhenNoneReceiptedAndWithoutClassifiers() {
    int expectedCaseCount = 10;

    Specification<Case> expectedSpecification = getSpecificationWithoutClassifiers();

    List<Case> cases = caseRepository.findAll(expectedSpecification);

    assertThat(cases.size()).isEqualTo(expectedCaseCount);
  }

  @Transactional
  @Test
  @Sql(scripts = {"/data-test-03of10-receipted.sql"})
  public void shouldRetrieveSevenCasesWhenThreeReceiptedAndWithoutClassifiers() {
    int expectedCaseSize = 7;

    Specification<Case> expectedSpecification = getSpecificationWithoutClassifiers();

    List<Case> cases = caseRepository.findAll(expectedSpecification);

    assertThat(cases.size()).isEqualTo(expectedCaseSize);
  }

  @Transactional
  @Test
  @Sql(scripts = {"/data-test-10of10-unreceipted.sql"})
  public void shouldRetrieveTenCasesWhenZeroReceiptedAndWithClassifiers() {
    int expectedCaseSize = 10;

    Specification<Case> expectedSpecification = getSpecificationWithClassifiers();

    List<Case> cases = caseRepository.findAll(expectedSpecification);

    assertThat(cases.size()).isEqualTo(expectedCaseSize);
  }

  @Transactional
  @Test
  @Sql(scripts = {"/data-test-03of10-receipted.sql"})
  public void shouldRetrieveSevenCasesWhenThreeReceiptedAndWithClassifiers() {
    int expectedCaseSize = 7;

    Specification<Case> expectedSpecification = getSpecificationWithClassifiers();

    List<Case> cases = caseRepository.findAll(expectedSpecification);

    assertThat(cases.size()).isEqualTo(expectedCaseSize);
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
    return where(isActionPlanIdEqualTo())
        .and(excludeReceiptedCases());
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