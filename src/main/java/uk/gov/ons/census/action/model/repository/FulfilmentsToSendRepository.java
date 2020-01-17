package uk.gov.ons.census.action.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RestResource;
import uk.gov.ons.census.action.model.entity.FulfilmentsToSend;

@RestResource(exported = false)
public interface FulfilmentsToSendRepository extends JpaRepository<FulfilmentsToSend, String> {
  //    Optional<FulfilmentsToSend> findByFulfilmentCode(String fulfilment_code);
}
