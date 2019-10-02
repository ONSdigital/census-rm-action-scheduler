package uk.gov.ons.census.action.model.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.model.entity.ActionRule;

@Component
public class CaseClassifier {

  private final JdbcTemplate jdbcTemplate;

  public CaseClassifier(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void enqueueCasesForActionRule(ActionRule actionRule) {
    UUID batchId = UUID.randomUUID();

    jdbcTemplate.update(
        "insert into actionv2.case_to_process (batch_id, batch_quantity, action_rule_id, "
            + "caze_case_ref) select ?, count(*) OVER (), ?, case_ref from "
            + "actionv2.cases "
            + buildWhereClause(actionRule.getActionPlan().getId(), actionRule.getClassifiers()),
        batchId,
        actionRule.getId());
  }

  private String buildWhereClause(UUID actionPlanId, Map<String, List<String>> classifiers) {
    StringBuilder whereClause = new StringBuilder();
    whereClause.append(String.format("where action_plan_id='%s'", actionPlanId.toString()));
    whereClause.append(" and receipt_received='f'");
    whereClause.append(" and refusal_received='f'");
    whereClause.append(" and address_invalid='f'");
    whereClause.append(" and case_type != 'HI'");

    for (Map.Entry<String, List<String>> classifier : classifiers.entrySet()) {
      whereClause.append(String.format(" and %s in (", classifier.getKey()));

      whereClause.append(
          String.join(
              ",",
              classifier.getValue().stream()
                  .map(value -> ("'" + value + "'"))
                  .collect(Collectors.toList())));

      whereClause.append(String.format(")"));
    }

    return whereClause.toString();
  }
}
