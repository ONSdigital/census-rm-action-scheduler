package uk.gov.ons.census.action.model.entity;

public enum ActionType {
  // Initial contact letters
  ICL1E(ActionHandler.PRINTER, "P_IC_ICL1"), // Census initial contact letter for England
  ICL2W(ActionHandler.PRINTER, "P_IC_ICL2B"), // Census initial contact letter for Wales
  ICL4N(ActionHandler.PRINTER, "P_IC_ICL4"), // Census initial contact letter for NI

  // Initial contact questionnaires
  ICHHQE(ActionHandler.PRINTER, "P_IC_H1"), // Census household questionnaire for England
  ICHHQW(ActionHandler.PRINTER, "P_IC_H2"), // Census household questionnaire for Wales
  ICHHQN(ActionHandler.PRINTER, "P_IC_H4"), // Census household questionnaire for NI
  CE_IC08(ActionHandler.PRINTER, "D_FDCE_I4"), // Individual CE Estab questionnaire for NI
  CE_IC09(ActionHandler.PRINTER, "D_FDCE_I1"), // Individual CE Estab questionnaire for England
  CE_IC10(ActionHandler.PRINTER, "D_FDCE_I2"), // Individual CE Estab questionnaire for Wales

  // CE1 for initial contact
  CE1_IC01(ActionHandler.PRINTER, "D_CE1A_ICLCR1"),
  CE1_IC02(ActionHandler.PRINTER, "D_CE1A_ICLCR2B"),

  // Individual addressed initial contact letters for CE Estabs
  CE_IC03(ActionHandler.PRINTER, "D_ICA_ICLR1"),
  CE_IC04(ActionHandler.PRINTER, "D_ICA_ICLR2B"),
  CE_IC05(ActionHandler.PRINTER, "D_CE4A_ICLR4"),
  CE_IC06(ActionHandler.PRINTER, "D_CE4A_ICLS4"),

  // Individual addressed initial contact letters for CE Units
  CE_IC03_1(
      ActionHandler.PRINTER,
      "D_ICA_ICLR1"), // Individual ICL with UAC for England (Hand Delivery) Addressed
  CE_IC04_1(
      ActionHandler.PRINTER,
      "D_ICA_ICLR2B"), // Individual ICL with UAC for Wales (Hand Delivery) Addressed

  // Initial contact letters for SPGs
  SPG_IC11(ActionHandler.PRINTER, "P_ICCE_ICL1"),
  SPG_IC12(ActionHandler.PRINTER, "P_ICCE_ICL2B"),

  // Initial contact SPG questionnaires
  SPG_IC13(ActionHandler.PRINTER, "D_FDCE_H1"),
  SPG_IC14(ActionHandler.PRINTER, "D_FDCE_H2"),

  // Generic actionType for use in Fieldwork followup action rules, tranches
  FIELD(ActionHandler.FIELD),

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
  P_TB_TBX(ActionHandler.PRINTER), // Household translation booklets

  P_UAC_HX(ActionHandler.PRINTER), // Household Unique Access Codes via paper

  P_OR_IX(ActionHandler.PRINTER), // Individual Response questionnaire print

  P_UAC_IX(ActionHandler.PRINTER), // Individual response UAC print

  P_ER_IL(ActionHandler.PRINTER), // Information leaflet

  //  response driven interventions
  P_RD_2RL1_1(ActionHandler.PRINTER), // Response driven reminder group 1 English
  P_RD_2RL2B_1(ActionHandler.PRINTER), // Response driven reminder group 1 Welsh
  P_RD_2RL1_2(ActionHandler.PRINTER), // Response driven reminder group 2 English
  P_RD_2RL2B_2(ActionHandler.PRINTER), // Response driven reminder group 2 Welsh
  P_RD_2RL1_3(ActionHandler.PRINTER), // Response driven reminder group 3 English
  P_RD_2RL2B_3(ActionHandler.PRINTER), // Response driven reminder group 3 Welsh

  // response driven reminders for survey launched, no new UACs needed
  P_RL_1RL1A(ActionHandler.PRINTER),
  P_RL_1RL2BA(ActionHandler.PRINTER),
  P_RL_2RL1A(ActionHandler.PRINTER),
  P_RL_2RL2BA(ActionHandler.PRINTER);

  private final ActionHandler handler;
  private final String packCode;

  ActionType(ActionHandler handler) {
    this.handler = handler;
    this.packCode = this.name();
  }

  ActionType(ActionHandler handler, String packCode) {
    this.handler = handler;
    this.packCode = packCode;
  }

  public ActionHandler getHandler() {
    return handler;
  }

  public String getPackCode() {
    return packCode;
  }
}
