package uk.gov.ons.census.action.model.repository;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import uk.gov.ons.census.action.model.entity.Case;

@RestResource(exported = false)
public interface CaseRepository extends JpaRepository<Case, Long>, JpaSpecificationExecutor<Case> {
  Optional<Case> findByCaseId(UUID caseId);

  @Query(value ="select sum(c.ceExpectedCapacity) from Case as c where c.treatmentCode in (:treatmentCodes)" +
          " and c.actionPlanId =:actionPlanId", nativeQuery = false)
  Integer sumAllExpectedCapacity(@Param("actionPlanId") String actionPlanId,
                             @Param("treatmentCodes") List<String> treatmentCodes);
}
