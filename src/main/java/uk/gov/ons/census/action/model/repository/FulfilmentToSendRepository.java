package uk.gov.ons.census.action.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import uk.gov.ons.census.action.model.entity.FulfilmentToProcess;

@RepositoryRestResource(exported = false)
public interface FulfilmentToSendRepository extends JpaRepository<FulfilmentToProcess, Long> {

  @Query("SELECT DISTINCT f.fulfilmentCode FROM FulfilmentToProcess f")
  List<String> findDistinctFulfilmentCode();
}
