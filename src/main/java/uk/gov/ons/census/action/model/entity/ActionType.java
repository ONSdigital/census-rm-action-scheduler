package uk.gov.ons.census.action.model.entity;

public enum ActionType {

  // Initial contact letters
  ICL1E(ActionHandler.PRINTER), // Census initial contact letter for England
  ICL2W(ActionHandler.PRINTER), // Census initial contact letter for Wales
  ICL4N(ActionHandler.PRINTER), // Census initial contact letter for NI

  // Initial contact questionnaires
  ICHHQE(ActionHandler.PRINTER), // Census household questionnaire for England
  ICHHQW(ActionHandler.PRINTER), // Census household questionnaire for Wales
  ICHHQN(ActionHandler.PRINTER), // Census household questionnaire for NI

  // generic actionType for use in Fieldwork followup actionplans, tranches
  FIELD(ActionHandler.FIELD), // Fieldwork follow up F2 questionnaire for England

  // Reminder letters
  P_RL_1RL1_1(ActionHandler.PRINTER), // 1st Reminder, Letter - for England addresses
  P_RL_1RL2B_1(
      ActionHandler
          .PRINTER), // 1st Reminder, Letter - for Wales addresses (bilingual Welsh and English)
  P_RL_1RL4(ActionHandler.PRINTER), // 1st Reminder, Letter - for Ireland addresses
  P_RL_1RL1_2(ActionHandler.PRINTER), // 2nd Reminder, Letter - for England addresses
  P_RL_1RL2B_2(
      ActionHandler
          .PRINTER), // 2nd Reminder, Letter - for Wales addresses (bilingual Welsh and English)
  P_RL_2RL1_3a(ActionHandler.PRINTER), // 3rd Reminder, Letter - for England addresses
  P_RL_2RL2B_3a(ActionHandler.PRINTER), // 3rd Reminder, Letter - for Wales addresses

  // Reminder questionnaires
  P_QU_H1(ActionHandler.PRINTER),
  P_QU_H2(ActionHandler.PRINTER),
  P_QU_H4(ActionHandler.PRINTER),

  // Ad hoc fulfilment requests
  P_OR_HX(ActionHandler.PRINTER), // Household questionnaires
  P_LP_HLX(ActionHandler.PRINTER), // Household questionnaires large print
  P_TB_TBX(ActionHandler.PRINTER); // Household translation booklets

  private final ActionHandler handler;

  ActionType(ActionHandler handler) {
    this.handler = handler;
  }

  public ActionHandler getHandler() {
    return handler;
  }
}
