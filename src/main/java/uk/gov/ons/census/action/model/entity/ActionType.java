package uk.gov.ons.census.action.model.entity;

public enum ActionType {
  ICL1E(ActionHandler.PRINTER), // Census initial contact letter for England
  ICL2W(ActionHandler.PRINTER), // Census initial contact letter for Wales
  ICHHQW(ActionHandler.PRINTER), // Census household questionnaire for Wales
  FF2QE(ActionHandler.FIELD); // Fieldwork follow up F2 questionnaire for England

  private final ActionHandler handler;

  ActionType(ActionHandler handler) {
    this.handler = handler;
  }

  public ActionHandler getHandler() {
    return handler;
  }
}
