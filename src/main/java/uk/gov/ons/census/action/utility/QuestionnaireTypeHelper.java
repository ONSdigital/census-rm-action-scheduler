package uk.gov.ons.census.action.utility;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class QuestionnaireTypeHelper {
  private static final Logger log = LoggerFactory.getLogger(QuestionnaireTypeHelper.class);

  private static final String CCS_POSTBACK_FOR_ENGLAND_AND_WALES_ENGLISH = "51";
  private static final String CCS_POSTBACK_FOR_WALES_WELSH = "53";
  private static final String CCS_POSTBACK_CONTINUATION_QUESTIONNAIRE_FOR_ENGLAND_AND_WALES = "61";
  private static final String CCS_POSTBACK_CONTINUATION_QUESTIONNAIRE_FOR_WALES_WELSH = "63";
  private static final String CCS_INTERVIEWER_HOUSEHOLD_QUESTIONNAIRE_FOR_ENGLAND_AND_WALES = "71";
  private static final String CCS_INTERVIEWER_HOUSEHOLD_QUESTIONNAIRE_FOR_WALES_WELSH = "73";
  private static final String CCS_INTERVIEWER_CE_MANAGER_FOR_ENGLAND_AND_WALES_ENGLISH = "81";
  private static final String CCS_INTERVIEWER_CE_MANAGER_FOR_WALES_WELSH = "83";
  private static final Set<String> ccsQuestionnaireTypes =
      new HashSet<>(
          Arrays.asList(
              CCS_POSTBACK_FOR_ENGLAND_AND_WALES_ENGLISH,
              CCS_POSTBACK_FOR_WALES_WELSH,
              CCS_POSTBACK_CONTINUATION_QUESTIONNAIRE_FOR_ENGLAND_AND_WALES,
              CCS_POSTBACK_CONTINUATION_QUESTIONNAIRE_FOR_WALES_WELSH,
              CCS_INTERVIEWER_HOUSEHOLD_QUESTIONNAIRE_FOR_ENGLAND_AND_WALES,
              CCS_INTERVIEWER_HOUSEHOLD_QUESTIONNAIRE_FOR_WALES_WELSH,
              CCS_INTERVIEWER_CE_MANAGER_FOR_ENGLAND_AND_WALES_ENGLISH,
              CCS_INTERVIEWER_CE_MANAGER_FOR_WALES_WELSH));

  public static boolean isCCSQuestionnaireType(String questionnaireId) {
    String questionnaireType = questionnaireId.substring(0, 2);

    return ccsQuestionnaireTypes.contains(questionnaireType);
  }
}
