package uk.gov.ons.census.actionsvc.model.repository;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import uk.gov.ons.census.actionsvc.model.entity.ActionPlan;

public interface ActionPlanRepository extends CrudRepository<ActionPlan, UUID> {}
