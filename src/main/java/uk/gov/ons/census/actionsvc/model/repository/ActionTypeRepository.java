package uk.gov.ons.census.actionsvc.model.repository;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import uk.gov.ons.census.actionsvc.model.entity.ActionType;

public interface ActionTypeRepository extends CrudRepository<ActionType, UUID> {}
