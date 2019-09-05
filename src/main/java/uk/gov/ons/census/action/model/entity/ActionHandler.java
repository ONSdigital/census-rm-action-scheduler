package uk.gov.ons.census.action.model.entity;

public enum ActionHandler {
  PRINTER("Action.Printer.binding"),
  FIELD("Action.Field.binding");

  private final String routingKey;

  ActionHandler(String routingKey) {
    this.routingKey = routingKey;
  }

  public String getRoutingKey() {
    return routingKey;
  }
}
