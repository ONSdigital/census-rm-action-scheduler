package uk.gov.ons.census.action.schedule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.ons.census.action.model.entity.ActionPlan;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;

public class CaseClassifierTest {

  @Test
  public void testEnqueueCasesForActionRuleField() {
    // Given
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    CaseClassifier underTest = new CaseClassifier(jdbcTemplate);
    String userDefinedWhereClause = " AND treatment_code IN ('abc','xyz')";

    ActionPlan actionPlan = new ActionPlan();
    actionPlan.setId(UUID.randomUUID());
    ActionRule actionRule = new ActionRule();
    actionRule.setId(UUID.randomUUID());
    actionRule.setActionPlan(actionPlan);
    actionRule.setUserDefinedWhereClause(userDefinedWhereClause);
    actionRule.setActionType(ActionType.FIELD);

    // When
    underTest.enqueueCasesForActionRule(actionRule);

    // Then
    StringBuilder expectedSql = new StringBuilder();
    expectedSql.append("INSERT INTO actionv2.case_to_process (batch_id, batch_quantity,");
    expectedSql.append(" action_rule_id, caze_case_ref) SELECT ?, COUNT(*) OVER (), ?, case_ref");
    expectedSql.append(" FROM actionv2.cases WHERE action_plan_id=");
    expectedSql.append("'" + actionPlan.getId().toString() + "'");
    expectedSql.append(" AND receipt_received='f'");
    expectedSql.append(" AND address_invalid='f' AND case_type != 'HI'");
    expectedSql.append(" AND skeleton='f'");
    expectedSql.append(" AND refusal_received IS NULL");
    expectedSql.append("  AND treatment_code IN ('abc','xyz')");
    verify(jdbcTemplate)
        .update(eq(expectedSql.toString()), any(UUID.class), eq(actionRule.getId()));
  }

  @Test
  public void testEnqueueCasesForActionRulePrinter() {
    // Given
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    CaseClassifier underTest = new CaseClassifier(jdbcTemplate);
    String userDefinedWhereClause = " AND treatment_code IN ('abc','xyz')";
    ActionPlan actionPlan = new ActionPlan();
    actionPlan.setId(UUID.randomUUID());
    ActionRule actionRule = new ActionRule();
    actionRule.setId(UUID.randomUUID());
    actionRule.setActionPlan(actionPlan);
    actionRule.setUserDefinedWhereClause(userDefinedWhereClause);
    // Action Type for Printed Reminder Letter
    actionRule.setActionType(ActionType.P_RL_1RL1_1);

    // When
    underTest.enqueueCasesForActionRule(actionRule);

    // Then
    StringBuilder expectedSql = new StringBuilder();
    expectedSql.append("INSERT INTO actionv2.case_to_process (batch_id, batch_quantity,");
    expectedSql.append(" action_rule_id, caze_case_ref) SELECT ?, COUNT(*) OVER (), ?, case_ref");
    expectedSql.append(" FROM actionv2.cases WHERE action_plan_id=");
    expectedSql.append("'" + actionPlan.getId().toString() + "'");
    expectedSql.append(" AND receipt_received='f'");
    expectedSql.append(" AND address_invalid='f' AND case_type != 'HI'");
    expectedSql.append(" AND skeleton='f'");
    expectedSql.append(" AND refusal_received IS DISTINCT FROM 'EXTRAORDINARY_REFUSAL'");
    expectedSql.append("  AND treatment_code IN ('abc','xyz')");
    verify(jdbcTemplate)
        .update(eq(expectedSql.toString()), any(UUID.class), eq(actionRule.getId()));
  }

  @Test
  public void testEnqueueCeEstabCasesForActionRulePrinter() {
    // Given
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    CaseClassifier underTest = new CaseClassifier(jdbcTemplate);
    String userDefinedWhereClause = " AND treatment_code IN ('abc','xyz')";
    ActionPlan actionPlan = new ActionPlan();
    actionPlan.setId(UUID.randomUUID());
    ActionRule actionRule = new ActionRule();
    actionRule.setId(UUID.randomUUID());
    actionRule.setActionPlan(actionPlan);
    actionRule.setUserDefinedWhereClause(userDefinedWhereClause);
    actionRule.setActionType(ActionType.CE_IC03);

    // When
    underTest.enqueueCasesForActionRule(actionRule);

    // Then
    StringBuilder expectedSql = new StringBuilder();
    expectedSql.append("INSERT INTO actionv2.case_to_process (batch_id, batch_quantity,");
    expectedSql.append(" action_rule_id, caze_case_ref, ce_expected_capacity) SELECT ?,");
    expectedSql.append(" SUM(ce_expected_capacity) OVER(), ?, case_ref, ce_expected_capacity");
    expectedSql.append(" FROM actionv2.cases WHERE action_plan_id=");
    expectedSql.append("'" + actionPlan.getId().toString() + "'");
    expectedSql.append(" AND receipt_received='f'");
    expectedSql.append(" AND address_invalid='f'");
    expectedSql.append(" AND case_type != 'HI'");
    expectedSql.append(" AND skeleton='f'");
    expectedSql.append(" AND refusal_received IS DISTINCT FROM 'EXTRAORDINARY_REFUSAL'");
    expectedSql.append("  AND treatment_code IN ('abc','xyz')");
    expectedSql.append(" GROUP BY case_ref");
    verify(jdbcTemplate)
        .update(eq(expectedSql.toString()), any(UUID.class), eq(actionRule.getId()));
  }
}
