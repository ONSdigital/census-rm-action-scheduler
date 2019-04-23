package uk.gov.ons.census.actionsvc.model.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import uk.gov.ons.census.actionsvc.model.entity.ActionRule;

public interface ActionRuleRepository extends CrudRepository<ActionRule, UUID> {
  List<ActionRule> findByTriggerDateTimeBeforeAndHasTriggeredIsFalse(
      OffsetDateTime triggerDateTime);
}
