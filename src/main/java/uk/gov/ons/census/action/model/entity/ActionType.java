package uk.gov.ons.census.action.model.entity;

public enum ActionType {
  ICL1E("Printer"); // Census initial contact letter for England

  private final String handler;

  ActionType(String handler) {
    this.handler = handler;
  }

  String getHandler() {
    return handler;
  }
}
