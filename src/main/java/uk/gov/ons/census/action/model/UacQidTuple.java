package uk.gov.ons.census.action.model;

import java.util.Optional;
import lombok.Data;
import uk.gov.ons.census.action.model.entity.UacQidLink;

@Data
public class UacQidTuple {
  private UacQidLink uacQidLink;
  private Optional<UacQidLink> uacQidLinkWales = Optional.ofNullable(null);
}
