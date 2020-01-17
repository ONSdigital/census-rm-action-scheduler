package uk.gov.ons.census.action.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;
import uk.gov.ons.census.action.model.entity.FulfilmentsToSend;

import java.util.List;

@RestResource(exported = false)
public interface FulfilmentsToSendRepository extends JpaRepository<FulfilmentsToSend, String> {
  @Query(
          value = "SELECT * FROM actionv2.fulfilments_to_send where fulfilment_code = :fulfilmentCode",
          nativeQuery = true)
  FulfilmentsToSend findByFulfilmentCode(@Param("fulfilmentCode") String fulfilmentCode);

}
