package uk.gov.ons.census.action.utility;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class QuestionnaireTypeHelperTest {

  @Test
  public void testValidQuestionnaireTypeEnglandAndWalesInEnglishPostbackCCS() {
    // Given

    // When
    boolean actual = QuestionnaireTypeHelper.isCCSQuestionnaireType("51");

    // Then
    assertThat(actual).isTrue();
  }

  @Test
  public void testValidQuestionnaireTypeWalesInWelshPostbackCCS() {
    // Given

    // When
    boolean actual = QuestionnaireTypeHelper.isCCSQuestionnaireType("53");

    // Then
    assertThat(actual).isTrue();
  }

  @Test
  public void testValidQuestionnaireTypeEnglandAndWalesInEnglishContinuationCCS() {
    // Given

    // When
    boolean actual = QuestionnaireTypeHelper.isCCSQuestionnaireType("61");

    // Then
    assertThat(actual).isTrue();
  }

  @Test
  public void testValidQuestionnaireTypeWalesInWelshContinuationCCS() {
    // Given

    // When
    boolean actual = QuestionnaireTypeHelper.isCCSQuestionnaireType("63");

    // Then
    assertThat(actual).isTrue();
  }

  @Test
  public void testValidQuestionnaireTypeEnglandAndWalesInterviewerHHCCS() {
    // Given

    // When
    boolean actual = QuestionnaireTypeHelper.isCCSQuestionnaireType("71");

    // Then
    assertThat(actual).isTrue();
  }

  @Test
  public void testValidQuestionnaireTypeWalesInWelshInterviewerHHCCS() {
    // Given

    // When
    boolean actual = QuestionnaireTypeHelper.isCCSQuestionnaireType("73");

    // Then
    assertThat(actual).isTrue();
  }

  @Test
  public void testValidQuestionnaireTypeEnglandAndWalesInEnglishInterviewerCECCS() {
    // Given

    // When
    boolean actual = QuestionnaireTypeHelper.isCCSQuestionnaireType("81");

    // Then
    assertThat(actual).isTrue();
  }

  @Test
  public void testValidQuestionnaireTypeWalesInEnglishInterviewerCECCS() {
    // Given

    // When
    boolean actual = QuestionnaireTypeHelper.isCCSQuestionnaireType("81");

    // Then
    assertThat(actual).isTrue();
  }

  @Test
  public void testIsNotCCSQuestionnaireType() {
    // Given

    // When
    boolean actual = QuestionnaireTypeHelper.isCCSQuestionnaireType("99");

    // Then
    assertThat(actual).isFalse();
  }
}
