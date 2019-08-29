package uk.gov.ons.census.action.model;

public enum QuestionnaireType {
  ENGLAND_HOUSEHOLD(1),
  WALES_HOUSEHOLD(2),
  WALES_WELSH_HOUSEHOLD(3),
  NIRELAND_HOUSEHOLD(4),
  ENGLAND_HOUSEHOLD_CONTINUATION(11),
  WALES_HOUSEHOLD_CONTINUATION(12),
  WALES_WELSH_HOUSEHOLD_CONTINUATION(13),
  NIRELAND_HOUSEHOLD_CONTINUATION(14),
  ENGLAND_INDIVIDUAL(21),
  WALES_INDIVIDUAL(22),
  WALES_WELSH_INDIVIDUAL(23),
  NIRELAND_INDIVIDUAL(24);

  private int value;

  QuestionnaireType(int val) {
    this.value = val;
  }

  public int getValue() {
    return value;
  }
}
