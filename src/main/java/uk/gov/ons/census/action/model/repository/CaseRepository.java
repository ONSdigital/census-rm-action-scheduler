package uk.gov.ons.census.action.model.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RestResource;
import uk.gov.ons.census.action.model.entity.Case;

@RestResource(exported = false)
public interface CaseRepository extends JpaRepository<Case, UUID>, JpaSpecificationExecutor<Case> {

  Optional<Case> findByCaseId(UUID caseId);

  Stream<Case> findByActionPlanIdAndReceiptReceivedIsFalse(String actionPlanID);
}
