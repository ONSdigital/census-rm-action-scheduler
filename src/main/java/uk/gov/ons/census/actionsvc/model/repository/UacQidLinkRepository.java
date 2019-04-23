package uk.gov.ons.census.actionsvc.model.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.census.actionsvc.model.entity.UacQidLink;

public interface UacQidLinkRepository extends JpaRepository<UacQidLink, UUID> {
  List<UacQidLink> findByCaseId(String caseId);
}
