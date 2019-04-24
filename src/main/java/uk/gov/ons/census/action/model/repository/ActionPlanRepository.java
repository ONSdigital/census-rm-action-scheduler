package uk.gov.ons.census.action.model.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.census.action.model.entity.ActionPlan;

public interface ActionPlanRepository extends JpaRepository<ActionPlan, UUID> {}
