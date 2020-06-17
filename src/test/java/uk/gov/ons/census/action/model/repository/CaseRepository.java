package uk.gov.ons.census.action.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.census.action.model.entity.Case;

@ActiveProfiles("test")
public interface CaseRepository extends JpaRepository<Case, Long>, JpaSpecificationExecutor<Case> {}
