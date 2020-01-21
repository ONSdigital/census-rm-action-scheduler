package uk.gov.ons.census.action.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RestResource;
import uk.gov.ons.census.action.model.entity.FulfilmentToSend;

@RestResource(exported = false)
public interface FulfilmentToSendRepository extends JpaRepository<FulfilmentToSend, String> {}
