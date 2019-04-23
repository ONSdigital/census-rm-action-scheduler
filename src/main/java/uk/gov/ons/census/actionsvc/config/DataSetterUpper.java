package uk.gov.ons.census.actionsvc.config;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import uk.gov.ons.census.actionsvc.model.entity.ActionPlan;
import uk.gov.ons.census.actionsvc.model.entity.ActionRule;
import uk.gov.ons.census.actionsvc.model.entity.ActionType;
import uk.gov.ons.census.actionsvc.model.repository.ActionPlanRepository;
import uk.gov.ons.census.actionsvc.model.repository.ActionRuleRepository;
import uk.gov.ons.census.actionsvc.model.repository.ActionTypeRepository;

@Configuration
public class DataSetterUpper {
  ActionPlanRepository actionPlanRepository;
  ActionRuleRepository actionRuleRepository;
  ActionTypeRepository actionTypeRepository;

  public DataSetterUpper(
      ActionPlanRepository actionPlanRepository,
      ActionRuleRepository actionRuleRepository,
      ActionTypeRepository actionTypeRepository) {
    this.actionPlanRepository = actionPlanRepository;
    this.actionRuleRepository = actionRuleRepository;
    this.actionTypeRepository = actionTypeRepository;
  }

  @PostConstruct
  public void setUpActionPlanAndRules() {
    ActionType actionType = new ActionType();
    actionType.setId(UUID.randomUUID());
    actionType.setHandler("Printer");
    actionType = actionTypeRepository.save(actionType);

    ActionPlan actionPlan = new ActionPlan();
    actionPlan.setId(UUID.fromString("fb5008f5-90c0-414c-83d7-dbbfb71746e5"));
    actionPlan = actionPlanRepository.save(actionPlan);

    String[] treatmentCodeArray = {
      //        "HH_LF3R2E", "HH_LF3R3AE", "HH_LF3R3BE", "HH_LFNR1E", "HH_LF2R3BE"
      "HH_LF3R2E", "HH_LF3R3AE", "HH_LF2R3BE"
    };
    Map<String, List<String>> classifiers = new HashMap<>();

    classifiers.put("treatmentCode", Arrays.asList(treatmentCodeArray));

    ActionRule actionRule = new ActionRule();
    actionRule.setId(UUID.randomUUID());
    actionRule.setActionPlan(actionPlan);
    actionRule.setActionType(actionType);
    actionRule.setHasTriggered(false);
    actionRule.setTriggerDateTime(LocalDateTime.now().plusMinutes(5).atOffset(ZoneOffset.UTC));
    actionRule.setClassifiers(classifiers);
    actionRuleRepository.save(actionRule);
  }
}
