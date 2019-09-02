// package uk.gov.ons.census.action.model;
//
// import java.util.HashMap;
// import java.util.Map;
// import java.util.Optional;
// import uk.gov.ons.census.action.model.entity.ActionType;
//
// public enum PackCodeQuestionnaireActionType {
//  ENGLAND_HOUSEHOLD_QUESTIONNAIRE("P_OR_H1", "1", ActionType.P_OR_HX),
//  WALES_HOUSEHOLD_QUESTIONNAIRE("P_OR_H2", "2", ActionType.P_OR_HX),
//  WALES_WELSH_HOUSEHOLD_QUESTIONNAIRE("P_OR_H2W", "3", ActionType.P_OR_HX),
//  NIRELAND_HOUSEHOLD_QUESTIONNAIRE("P_OR_H4", "4", ActionType.P_OR_HX),
//  ENGLAND_HOUSEHOLD_CONTINUATION("P_OR_HC1", "11", ActionType.P_OR_HX),
//  WALES_HOUSEHOLD_CONTINUATION("P_OR_HC2", "12", ActionType.P_OR_HX),
//  WALES_WELSH_HOUSEHOLD_CONTINUATION("P_OR_HC2W", "13", ActionType.P_OR_HX),
//  NIRELAND_HOUSEHOLD_CONTINUATION("P_OR_HC4", "14", ActionType.P_OR_HX),
//  ENGLAND_INDIVIDUAL_QUESTIONNAIRE_PRINT("P_OR_I1", "21", ActionType.P_OR_I1),
//  WALES_INDIVIDUAL_QUESTIONNAIRE_PRINT("P_OR_I2", "22", ActionType.P_OR_I2),
//  WALES_WELSH_INDIVIDUAL_QUESTIONNAIRE_PRINT("P_OR_I2W", "23", ActionType.P_OR_I2),
//  NIRELAND_INDIVIDUAL_QUESTIONNAIRE_PRINT("P_OR_I4", "24", ActionType.P_OR_I4);
//
//  private static final Map<String, PackCodeQuestionnaireActionType> packCodeQuestionnaireTypeMap =
//      getFulfilmentToQuestionnaireTypesMap();
//  private String packCode;
//  private String questionannaireType;
//  private ActionType actionType;
//
//  PackCodeQuestionnaireActionType(
//      String packCode, String questionannaireType, ActionType actionType) {
//    this.packCode = packCode;
//    this.questionannaireType = questionannaireType;
//    this.actionType = actionType;
//  }
//
//  public String getPackCode() {
//    return packCode;
//  }
//
//  public String getQuestionnaireType() {
//    return questionannaireType;
//  }
//
//  public ActionType getActionType() {
//    return actionType;
//  }
//
//  public static Optional<PackCodeQuestionnaireActionType> getFromPackCode(String packCode) {
//    PackCodeQuestionnaireActionType packCodeQuestionnaireActionType =
//        packCodeQuestionnaireTypeMap.get(packCode);
//
//    return Optional.ofNullable(packCodeQuestionnaireActionType);
//  }
//
//  private static Map<String, PackCodeQuestionnaireActionType>
//      getFulfilmentToQuestionnaireTypesMap() {
//    Map<String, PackCodeQuestionnaireActionType> packCodeToQuestionnaireTypeMap = new HashMap<>();
//    for (PackCodeQuestionnaireActionType packCodeQuestionnaireType :
//        PackCodeQuestionnaireActionType.values()) {
//      packCodeToQuestionnaireTypeMap.put(
//          packCodeQuestionnaireType.getPackCode(), packCodeQuestionnaireType);
//    }
//
//    return packCodeToQuestionnaireTypeMap;
//  }
// }
