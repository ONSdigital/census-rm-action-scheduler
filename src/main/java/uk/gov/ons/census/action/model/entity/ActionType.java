package uk.gov.ons.census.action.model.entity;

public enum ActionType {

  ICL1E("Census initial contact letter for England", "Printer");

  private final String description;
  private final String handler;
  ActionType(String description, String handler) {
    this.description = description;
    this.handler = handler;
  }

  String getDescription() {
    return description;
  }
  String getHandler() {
    return handler;
  }
}