package uk.gov.ons.census.action.model.entity;

public enum ActionType {
  ICL1E(ActionHandler.PRINTER), // Census initial contact letter for England
  ICL2W(ActionHandler.PRINTER), // Census initial contact letter for Wales
  ICL4N(ActionHandler.PRINTER), // Census intiial contact letter for NI

  ICHHQE(ActionHandler.PRINTER), // Census household questionnaire for England
  ICHHQW(ActionHandler.PRINTER), // Census household questionnaire for Wales
  ICHHQN(ActionHandler.PRINTER), // Census household questionnaire for NI

  FF2QE(ActionHandler.FIELD); // Fieldwork follow up F2 questionnaire for England

  private final ActionHandler handler;

  ActionType(ActionHandler handler) {
    this.handler = handler;
  }

  public ActionHandler getHandler() {
    return handler;
  }
}
