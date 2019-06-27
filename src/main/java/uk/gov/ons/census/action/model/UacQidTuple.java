package uk.gov.ons.census.action.model;

import lombok.Data;
import uk.gov.ons.census.action.model.entity.UacQidLink;

import java.util.Optional;

@Data
public class UacQidTuple {
    private UacQidLink uacQidLink;
    private Optional<UacQidLink> uacQidLinkWales = Optional.ofNullable(null);
}