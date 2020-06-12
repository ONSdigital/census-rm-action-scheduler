package uk.gov.ons.census.action.model.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.census.action.model.entity.CaseToProcess;

@ActiveProfiles("test")
public interface CaseToProcessRepository extends JpaRepository<CaseToProcess, UUID> {}
