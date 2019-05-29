package uk.gov.ons.census.action.model.entity;

public enum ActionType {
  ICL1E("Printer"), // Census initial contact letter for England
  ICL2W("Printer"), // Census initial contact letter for Wales
  ICHHQW("Printer"); // Census household questionnaire for Wales

  private final String handler;

  ActionType(String handler) {
    this.handler = handler;
  }

  public String getHandler() {
    return handler;
  }
}
