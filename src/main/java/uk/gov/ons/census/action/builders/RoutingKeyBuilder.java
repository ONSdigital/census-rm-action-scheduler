package uk.gov.ons.census.action.builders;

import uk.gov.ons.census.action.model.entity.ActionHandler;

public class RoutingKeyBuilder {
  private static final String ROUTING_KEY_PREFIX = "Action.";
  private static final String ROUTING_KEY_SUFFIX = ".binding";

  public static String getRoutingKey(ActionHandler handler) {
    return String.format("%s%s%s", ROUTING_KEY_PREFIX, handler.getRoutingKey(), ROUTING_KEY_SUFFIX);
  }
}
