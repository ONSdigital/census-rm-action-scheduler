package uk.gov.ons.census.action.model.entity;

public enum ActionHandler {
  PRINTER("Printer"),
  FIELD("Field");

  private final String routingKey;

  ActionHandler(String routingKey) {
    this.routingKey = routingKey;
  }

  public String getRoutingKey() {
    return routingKey;
  }
}
