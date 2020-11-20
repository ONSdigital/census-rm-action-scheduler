package uk.gov.ons.census.action.model.entity;

public enum ActionType {
  // Initial contact pack for English households
  ICL1E(ActionHandler.PRINTER, "P_IC_ICL1"),
  // Initial contact pack for Welsh households
  ICL2W(ActionHandler.PRINTER, "P_IC_ICL2B"),
  // ICL with UAC HH (Post Out) Addressed Northern Ireland
  ICL4N(ActionHandler.PRINTER, "P_IC_ICL4"),

  // Household Questionnaire for England
  ICHHQE(ActionHandler.PRINTER, "P_IC_H1"),
  // Household Questionnaire for Wales
  ICHHQW(ActionHandler.PRINTER, "P_IC_H2"),
  // Household Questionnaire for Northern Ireland
  ICHHQN(ActionHandler.PRINTER, "P_IC_H4"),
  // Individual Questionnaire for NI (Hand delivery) Addressed
  CE_IC08(ActionHandler.PRINTER, "D_FDCE_I4"),
  // Individual Questionnaire for England (Hand delivery) Addressed
  CE_IC09(ActionHandler.PRINTER, "D_FDCE_I1"),
  // Individual Questionnaire for Wales (Hand delivery) Addressed
  CE_IC10(ActionHandler.PRINTER, "D_FDCE_I2"),

  // CE1 Packs (Hand Delivery) Addressed England
  CE1_IC01(ActionHandler.PRINTER, "D_CE1A_ICLCR1"),
  // CE1 Packs (Hand Delivery) Addressed Wales
  CE1_IC02(ActionHandler.PRINTER, "D_CE1A_ICLCR2B"),

  // ICL with UAC Individual (Hand Delivery)  Addressed England
  CE_IC03(ActionHandler.PRINTER, "D_ICA_ICLR1"),
  // ICL with UAC Individual (Hand Delivery) Addressed Wales
  CE_IC04(ActionHandler.PRINTER, "D_ICA_ICLR2B"),
  // ICL with UAC Individual Resident (Hand Delivery) Addressed
  CE_IC05(ActionHandler.PRINTER, "D_CE4A_ICLR4"),
  // ICL with UAC Individual Student (Hand Delivery) Addressed
  CE_IC06(ActionHandler.PRINTER, "D_CE4A_ICLS4"),

  // ICL with UAC HH (Post Out) Addressed England
  SPG_IC11(ActionHandler.PRINTER, "P_ICCE_ICL1"),
  // ICL with UAC HH (Post Out) Addressed Wales
  SPG_IC12(ActionHandler.PRINTER, "P_ICCE_ICL2B"),

  // Household Questionnaire for England (Hand delivery) Addressed
  SPG_IC13(ActionHandler.PRINTER, "D_FDCE_H1"),
  // Household Questionnaire for Wales (Hand delivery) Addressed
  SPG_IC14(ActionHandler.PRINTER, "D_FDCE_H2"),

  // Generic actionType for use in Fieldwork followup action rules, tranches
  FIELD(ActionHandler.FIELD),

  // R1e England-1st reminder, UAC first households,  haven't launched EQ - batch a, b, c, d, e
  P_RL_1RL1_1(ActionHandler.PRINTER),
  // R1e Wales-1st reminder, UAC first households,  haven't launched EQ - batch a, b, c, d, e
  P_RL_1RL2B_1(ActionHandler.PRINTER),
  // R2a England-2nd reminder, UAC first households,  haven't launched EQ - batch a, b, c, d, e
  P_RL_2RL1(ActionHandler.PRINTER),
  // R1B NI - reminder 3 letter
  P_RL_2RL4(ActionHandler.PRINTER),
  // R2a Wales- 2nd reminder, UAC first households,  haven't launched EQ - batch
  P_RL_2RL2B(ActionHandler.PRINTER),

  // RP1 - England Paper questionnaire going to HtC willingness 4&5
  P_QU_H1(ActionHandler.PRINTER),
  // RP1 - Wales Paper questionnaire going to HtC willingness 4&5
  P_QU_H2(ActionHandler.PRINTER),
  // Reminder 2 NI PQ
  P_QU_H4(ActionHandler.PRINTER),

  // R3 - England   Third reminder letter going to anyone except those getting RP1/2/3
  P_RL_3RL1(ActionHandler.PRINTER),
  // R3 - Wales   Third reminder letter going to anyone except those getting RP1/2/3
  P_RL_3RL2B(ActionHandler.PRINTER),

  // RDR1 - England Response-driven reminder 1 going to worst performing areas
  P_RD_RNP41(ActionHandler.PRINTER),
  // RDR1 - Wales Response-driven reminder 1 going to worst performing areas
  P_RD_RNP42B(ActionHandler.PRINTER),
  // RDR2 - England Response-driven reminder 2 going to worst performing areas
  P_RD_RNP51(ActionHandler.PRINTER),
  // RDR2 - Wales Response-driven reminder 2 going to worst performing areas
  P_RD_RNP52B(ActionHandler.PRINTER),

  // R1a NI - first reminder, have launched EQ
  P_RL_1RL4A(ActionHandler.PRINTER),
  // RU1 England- First reminder going to those who have launched EQ
  P_RL_1RL1A(ActionHandler.PRINTER),
  // RU1 Wales- First reminder going to those who have launched EQ
  P_RL_1RL2BA(ActionHandler.PRINTER),
  // RU2 England- First reminder going to those who have launched EQ
  P_RL_2RL1A(ActionHandler.PRINTER),
  // RU2 Wales- First reminder going to those who have launched EQ
  P_RL_2RL2BA(ActionHandler.PRINTER),

  // IRL England - going to those who have requested an individual form via eQ only
  P_RL_1IRL1(ActionHandler.PRINTER),
  // IRL Wales - going to those who have requested an individual form via eQ only
  P_RL_1IRL2B(ActionHandler.PRINTER),

  // RPF1 England -First reminder going to paper first households,  haven't launched EQ
  P_RL_1RL1B(ActionHandler.PRINTER),
  // RPF1 Wales -First reminder going to paper first households, haven't launched EQ
  P_RL_1RL2BB(ActionHandler.PRINTER),
  // R1a NI - first reminder, have launched EQ
  P_RL_1RL4(ActionHandler.PRINTER),

  // Non-compliance letter - England
  P_NC_NCLTA1(ActionHandler.PRINTER),
  // Non-compliance letter - Wales
  P_NC_NCLTA2B(ActionHandler.PRINTER);

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
