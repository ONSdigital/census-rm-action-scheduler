package uk.gov.ons.census.action.model.entity;

public enum ActionType {
  ICL1E(Constants.PRINTER), // Census initial contact letter for England
  ICL2W(Constants.PRINTER), // Census initial contact letter for Wales
  ICL4E(Constants.PRINTER), // Census intiial contact letter for NI

  ICHHQE(Constants.PRINTER), // Census household questionnaire for England
  ICHHQW(Constants.PRINTER), // Census household questionnaire for Wales
  ICHHQN(Constants.PRINTER); // Census household questionnaire for NI

  private final String handler;

  ActionType(String handler) {
    this.handler = handler;
  }

  public String getHandler() {
    return handler;
  }

  private static class Constants {
    public static final String PRINTER = "Printer";
  }
}
