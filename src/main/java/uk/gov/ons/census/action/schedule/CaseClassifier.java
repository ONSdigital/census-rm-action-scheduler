package uk.gov.ons.census.action.schedule;

import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.model.entity.ActionHandler;
import uk.gov.ons.census.action.model.entity.ActionRule;
import uk.gov.ons.census.action.model.entity.ActionType;
import uk.gov.ons.census.action.model.entity.RefusalType;

@Component
public class CaseClassifier {
  private static final Set<ActionType> ceIndividualActionTypes =
      Set.of(
          ActionType.CE_IC03,
          ActionType.CE_IC03_1,
          ActionType.CE_IC04,
          ActionType.CE_IC04_1,
          ActionType.CE_IC05,
          ActionType.CE_IC06,
          ActionType.CE_IC08,
          ActionType.CE_IC09,
          ActionType.CE_IC10);

  private final JdbcTemplate jdbcTemplate;

  public CaseClassifier(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void enqueueCasesForActionRule(ActionRule actionRule) {
    UUID batchId = UUID.randomUUID();

    if (isCeIndividualActionType(actionRule.getActionType())) {
      jdbcTemplate.update(
          "INSERT INTO actionv2.case_to_process (batch_id, batch_quantity, action_rule_id, "
              + "caze_case_ref, ce_expected_capacity) SELECT ?, SUM(ce_expected_capacity) OVER(), ?, "
              + "case_ref, ce_expected_capacity FROM actionv2.cases "
              + buildWhereClause(
                  actionRule.getActionPlan().getId(),
                  actionRule.getUserDefinedWhereClause(),
                  actionRule.getActionType().getHandler())
              + " GROUP BY case_ref",
          batchId,
          actionRule.getId());
    } else {
      jdbcTemplate.update(
          "INSERT INTO actionv2.case_to_process (batch_id, batch_quantity, action_rule_id, "
              + "caze_case_ref) SELECT ?, COUNT(*) OVER (), ?, case_ref FROM "
              + "actionv2.cases "
              + buildWhereClause(
                  actionRule.getActionPlan().getId(),
                  actionRule.getUserDefinedWhereClause(),
                  actionRule.getActionType().getHandler()),
          batchId,
          actionRule.getId());
    }
  }

  private String buildWhereClause(
      UUID actionPlanId, String userDefinedWhereClause, ActionHandler actionHandler) {
    StringBuilder whereClause = new StringBuilder();
    whereClause.append(String.format("WHERE action_plan_id='%s'", actionPlanId.toString()));
    whereClause.append(" AND receipt_received='f'");
    whereClause.append(" AND address_invalid='f'");
    whereClause.append(" AND skeleton='f'");

    if (actionHandler == ActionHandler.PRINTER) {
      whereClause.append(
          " AND refusal_received IS DISTINCT FROM '"
              + RefusalType.EXTRAORDINARY_REFUSAL.name()
              + "'");
    } else {
      whereClause.append(" AND refusal_received IS NULL");
    }

    /*
     " AND " + userDefinedWhereClause, if there's no userDefinedWhereClause (it can't be null) it will cause an error,
      as the SQL statement will end: " AND "
      However this may not be bad behaviour, we would always want to a userDefinedWhereClause
    */
    whereClause.append(" AND ").append(userDefinedWhereClause);

    return whereClause.toString();
  }

  private static boolean isCeIndividualActionType(ActionType actionType) {
    return ceIndividualActionTypes.contains(actionType);
  }
}
