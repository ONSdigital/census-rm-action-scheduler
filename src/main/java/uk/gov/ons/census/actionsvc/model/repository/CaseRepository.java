package uk.gov.ons.census.actionsvc.model.repository;

import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uk.gov.ons.census.actionsvc.model.entity.Case;

public interface CaseRepository extends JpaRepository<Case, UUID>, JpaSpecificationExecutor<Case> {
  Stream<Case> findByActionPlanId(String actionPlanId);

  Case findByCaseId(UUID caseId);
}
