package uk.gov.ons.census.actionsvc.model.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.census.actionsvc.model.entity.ActionType;

public interface ActionTypeRepository extends JpaRepository<ActionType, UUID> {}
