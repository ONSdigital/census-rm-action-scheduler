package uk.gov.ons.census.action.builders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.ons.census.action.builders.QidUacBuilder.HOUSEHLD_INITIAL_CONTACT_QUESTIONNIARE_TREATMENT_CODE_PREFIX;
import static uk.gov.ons.census.action.builders.QidUacBuilder.WALES_TREATMENT_CODE_SUFFIX;

import java.util.ArrayList;
import java.util.List;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import uk.gov.ons.census.action.model.UacQidTuple;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.entity.UacQidLink;
import uk.gov.ons.census.action.model.repository.UacQidLinkRepository;

public class QidUacBuilderTest {

  @Test
  public void testEnglishAndWelshQiTupledReturned() {
    // Given
    EasyRandom easyRandom = new EasyRandom();
    Case testCase = easyRandom.nextObject(Case.class);
    String uacEng = easyRandom.nextObject(String.class);
    String uacWal = easyRandom.nextObject(String.class);
    String qidEng = "0220000010732199";
    String qidWal = "0320000002861455";

    List<UacQidLink> uacQidLinks = new ArrayList<>();
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setCaseId(testCase.getCaseId().toString());
    uacQidLink.setUac(uacEng);
    uacQidLink.setQid(qidEng);
    uacQidLinks.add(uacQidLink);

    uacQidLink = new UacQidLink();
    uacQidLink.setCaseId(testCase.getCaseId().toString());
    uacQidLink.setUac(uacWal);
    uacQidLink.setQid(qidWal);
    uacQidLinks.add(uacQidLink);
    UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
    when(uacQidLinkRepository.findByCaseId(testCase.getCaseId().toString()))
        .thenReturn(uacQidLinks);
    QidUacBuilder qidUacBuilder = new QidUacBuilder(uacQidLinkRepository);

    testCase.setTreatmentCode(
        HOUSEHLD_INITIAL_CONTACT_QUESTIONNIARE_TREATMENT_CODE_PREFIX
            + "BLAH"
            + WALES_TREATMENT_CODE_SUFFIX);

    // when
    UacQidTuple uacQidTuple = qidUacBuilder.getUacQidLinks(testCase);

    UacQidLink actualEnglandUacQidLink = uacQidTuple.getUacQidLink();
    assertThat(actualEnglandUacQidLink.getCaseId()).isEqualTo(testCase.getCaseId().toString());
    assertThat(actualEnglandUacQidLink.getQid()).isEqualTo(qidEng);
    assertThat(actualEnglandUacQidLink.getUac()).isEqualTo(uacEng);
    assertThat(actualEnglandUacQidLink.isActive()).isEqualTo(false);

    UacQidLink actualWalesdUacQidLink = uacQidTuple.getUacQidLinkWales().get();
    assertThat(actualWalesdUacQidLink.getCaseId()).isEqualTo(testCase.getCaseId().toString());
    assertThat(actualWalesdUacQidLink.getQid()).isEqualTo(qidWal);
    assertThat(actualWalesdUacQidLink.getUac()).isEqualTo(uacWal);
    assertThat(actualWalesdUacQidLink.isActive()).isEqualTo(false);
  }

  @Test
  public void testEnglishOnlyTupleReturned() {
    // Given
    EasyRandom easyRandom = new EasyRandom();
    Case testCase = easyRandom.nextObject(Case.class);
    String uacEng = easyRandom.nextObject(String.class);
    String qidEng = "0220000010732199";

    List<UacQidLink> uacQidLinks = new ArrayList<>();
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setCaseId(testCase.getCaseId().toString());
    uacQidLink.setUac(uacEng);
    uacQidLink.setQid(qidEng);
    uacQidLinks.add(uacQidLink);

    UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
    when(uacQidLinkRepository.findByCaseId(testCase.getCaseId().toString()))
        .thenReturn(uacQidLinks);
    QidUacBuilder qidUacBuilder = new QidUacBuilder(uacQidLinkRepository);

    testCase.setTreatmentCode("NotWelshTreatmentCode");

    // when
    UacQidTuple uacQidTuple = qidUacBuilder.getUacQidLinks(testCase);

    UacQidLink actualEnglandUacQidLink = uacQidTuple.getUacQidLink();
    assertThat(actualEnglandUacQidLink.getCaseId()).isEqualTo(testCase.getCaseId().toString());
    assertThat(actualEnglandUacQidLink.getQid()).isEqualTo(qidEng);
    assertThat(actualEnglandUacQidLink.getUac()).isEqualTo(uacEng);
    assertThat(actualEnglandUacQidLink.isActive()).isEqualTo(false);

    assertThat(uacQidTuple.getUacQidLinkWales().isPresent()).isEqualTo(false);
  }

  @Test(expected = RuntimeException.class)
  public void testWalesQuestionnaireWithTwoQidUacsWrongEnglish() {
    // Given
    EasyRandom easyRandom = new EasyRandom();
    Case testCase = easyRandom.nextObject(Case.class);
    String uacEng = easyRandom.nextObject(String.class);
    String uacWal = easyRandom.nextObject(String.class);
    String qidEng = "9920000010732199";
    String qidWal = "0320000002861455";

    List<UacQidLink> uacQidLinks = new ArrayList<>();
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setUac(uacEng);
    uacQidLink.setQid(qidEng);
    uacQidLinks.add(uacQidLink);

    uacQidLink = new UacQidLink();
    uacQidLink.setUac(uacWal);
    uacQidLink.setQid(qidWal);
    uacQidLinks.add(uacQidLink);
    UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
    when(uacQidLinkRepository.findByCaseId(testCase.getCaseId().toString()))
        .thenReturn(uacQidLinks);
    QidUacBuilder qidUacBuilder = new QidUacBuilder(uacQidLinkRepository);

    // when
    qidUacBuilder.getUacQidLinks(testCase);
  }

  @Test(expected = RuntimeException.class)
  public void testWalesQuestionnaireWithTwoQidUacsWrongWelsh() {
    // Given
    EasyRandom easyRandom = new EasyRandom();
    Case testCase = easyRandom.nextObject(Case.class);
    String uacEng = easyRandom.nextObject(String.class);
    String uacWal = easyRandom.nextObject(String.class);
    String qidEng = "0220000010732199";
    String qidWal = "9920000002861455";

    List<UacQidLink> uacQidLinks = new ArrayList<>();
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setUac(uacEng);
    uacQidLink.setQid(qidEng);
    uacQidLinks.add(uacQidLink);

    uacQidLink = new UacQidLink();
    uacQidLink.setUac(uacWal);
    uacQidLink.setQid(qidWal);
    uacQidLinks.add(uacQidLink);
    UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
    when(uacQidLinkRepository.findByCaseId(testCase.getCaseId().toString()))
        .thenReturn(uacQidLinks);
    QidUacBuilder qidUacBuilder = new QidUacBuilder(uacQidLinkRepository);

    // when
    qidUacBuilder.getUacQidLinks(testCase);
  }

  @Test(expected = RuntimeException.class)
  public void testWalesQuestionnaireWithTooManyQidUacs() {
    // Given
    EasyRandom easyRandom = new EasyRandom();
    Case testCase = easyRandom.nextObject(Case.class);
    String uacEng = easyRandom.nextObject(String.class);
    String uacWal = easyRandom.nextObject(String.class);
    String qidEng = "0220000010732199";
    String qidWal = "0320000002861455";

    List<UacQidLink> uacQidLinks = new ArrayList<>();
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setCaseId(testCase.getCaseId().toString());
    uacQidLink.setUac(uacEng);
    uacQidLink.setQid(qidEng);
    uacQidLinks.add(uacQidLink);

    uacQidLink = new UacQidLink();
    uacQidLink.setCaseId(testCase.getCaseId().toString());
    uacQidLink.setUac(uacWal);
    uacQidLink.setQid(qidWal);
    uacQidLinks.add(uacQidLink);

    // add the 3rd and fatal Link
    uacQidLinks.add(uacQidLink);

    UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
    when(uacQidLinkRepository.findByCaseId(testCase.getCaseId().toString()))
        .thenReturn(uacQidLinks);
    QidUacBuilder qidUacBuilder = new QidUacBuilder(uacQidLinkRepository);

    testCase.setTreatmentCode(
        HOUSEHLD_INITIAL_CONTACT_QUESTIONNIARE_TREATMENT_CODE_PREFIX
            + "BLAH"
            + WALES_TREATMENT_CODE_SUFFIX);

    // when
    qidUacBuilder.getUacQidLinks(testCase);
  }

  @Test(expected = RuntimeException.class)
  public void testWalesQuestionnaireWithMissingQidUac() {
    // Given
    EasyRandom easyRandom = new EasyRandom();
    Case testCase = easyRandom.nextObject(Case.class);
    String uacEng = easyRandom.nextObject(String.class);
    String qidEng = "0220000010732199";

    List<UacQidLink> uacQidLinks = new ArrayList<>();
    UacQidLink uacQidLink = new UacQidLink();
    uacQidLink.setCaseId(testCase.getCaseId().toString());
    uacQidLink.setUac(uacEng);
    uacQidLink.setQid(qidEng);
    uacQidLinks.add(uacQidLink);

    UacQidLinkRepository uacQidLinkRepository = mock(UacQidLinkRepository.class);
    when(uacQidLinkRepository.findByCaseId(testCase.getCaseId().toString()))
        .thenReturn(uacQidLinks);
    QidUacBuilder qidUacBuilder = new QidUacBuilder(uacQidLinkRepository);

    testCase.setTreatmentCode(
        HOUSEHLD_INITIAL_CONTACT_QUESTIONNIARE_TREATMENT_CODE_PREFIX
            + "BLAH"
            + WALES_TREATMENT_CODE_SUFFIX);

    // when
    qidUacBuilder.getUacQidLinks(testCase);
  }
}
