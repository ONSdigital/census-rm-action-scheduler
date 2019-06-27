package uk.gov.ons.census.action.builders;




public class QidUacBuilderTest {
  //  @Test(expected = RuntimeException.class)
  //  public void testWalesQuestionnaireWithTwoQidUacsWrongEnglish() {
  //    // Given
  //    EasyRandom easyRandom = new EasyRandom();
  //    ActionPlan actionPlan = easyRandom.nextObject(ActionPlan.class);
  //    ActionRule actionRule = new ActionRule();
  //    actionRule.setActionPlan(actionPlan);
  //    actionRule.setActionType(ActionType.ICL1E);
  //    Case testCase = easyRandom.nextObject(Case.class);
  //    testCase.setTreatmentCode("HH_QF2R1W");
  //    String uacEng = easyRandom.nextObject(String.class);
  //    String uacWal = easyRandom.nextObject(String.class);
  //    String qidEng = "9920000010732199";
  //    String qidWal = "0320000002861455";
  //
  //    List<UacQidLink> uacQidLinks = new LinkedList<>();
  //
  //    UacQidLink uacQidLink = new UacQidLink();
  //    uacQidLink.setUac(uacEng);
  //    uacQidLink.setQid(qidEng);
  //    uacQidLinks.add(uacQidLink);
  //
  //    UacQidTuple uacQidTuple = new UacQidTuple();
  //    uacQidTuple.setUacQidLink(uacQidLink);
  //
  //    uacQidLink = new UacQidLink();
  //    uacQidLink.setUac(uacWal);
  //    uacQidLink.setQid(qidWal);
  //    uacQidLinks.add(uacQidLink);
  //
  //    uacQidTuple.setUacQidLinkWales(Optional.of(uacQidLink));
  //
  ////    when(qidUacBuilder.getUacQidLinks(testCase)).thenReturn(uacQidTuple);
  //
  //    // When
  ////    ActionInstructionBuilder underTest = new ActionInstructionBuilder(qidUacBuilder);
  //    underTest.buildPrinterActionInstruction(testCase, actionRule);
  //
  //    // Then
  //    // Expect exception to be thrown
  //  }
  //
  //  @Test(expected = RuntimeException.class)
  //  public void testWalesQuestionnaireWithTwoQidUacsWrongWelsh() {
  //    // Given
  //    EasyRandom easyRandom = new EasyRandom();
  //    ActionPlan actionPlan = easyRandom.nextObject(ActionPlan.class);
  //    ActionRule actionRule = new ActionRule();
  //    actionRule.setActionPlan(actionPlan);
  //    actionRule.setActionType(ActionType.ICL1E);
  //    Case testCase = easyRandom.nextObject(Case.class);
  //    testCase.setTreatmentCode("HH_QF2R1W");
  //    String uacEng = easyRandom.nextObject(String.class);
  //    String uacWal = easyRandom.nextObject(String.class);
  //    String qidEng = "0220000010732199";
  //    String qidWal = "9920000002861455";
  //
  //    List<UacQidLink> uacQidLinks = new LinkedList<>();
  //
  //    UacQidLink uacQidLink = new UacQidLink();
  //    uacQidLink.setUac(uacEng);
  //    uacQidLink.setQid(qidEng);
  //    uacQidLinks.add(uacQidLink);
  //
  //    UacQidTuple uacQidTuple = new UacQidTuple();
  //    uacQidTuple.setUacQidLink(uacQidLink);
  //
  //    uacQidLink = new UacQidLink();
  //    uacQidLink.setUac(uacWal);
  //    uacQidLink.setQid(qidWal);
  //    uacQidLinks.add(uacQidLink);
  //
  //    uacQidTuple.setUacQidLinkWales(Optional.of(uacQidLink));
  //
  //    when(qidUacBuilder.getUacQidLinks(testCase)).thenReturn(uacQidTuple);
  //
  //    // When
  //    ActionInstructionBuilder underTest = new ActionInstructionBuilder(qidUacBuilder);
  //    underTest.buildPrinterActionInstruction(testCase, actionRule);
  //
  //    // Then
  //    // Expect exception to be thrown
  //  }
  //
  //  @Test(expected = RuntimeException.class)
  //  public void testWalesQuestionnaireWithTooManyQidUacs() {
  //    // Given
  //    EasyRandom easyRandom = new EasyRandom();
  //    ActionPlan actionPlan = easyRandom.nextObject(ActionPlan.class);
  //    ActionRule actionRule = new ActionRule();
  //    actionRule.setActionPlan(actionPlan);
  //    actionRule.setActionType(ActionType.ICL1E);
  //    Case testCase = easyRandom.nextObject(Case.class);
  //    testCase.setTreatmentCode("HH_QF2R1W");
  //    String uacEng = easyRandom.nextObject(String.class);
  //    String uacWal = easyRandom.nextObject(String.class);
  //    String uacRogue = easyRandom.nextObject(String.class);
  //    String qidEng = "0220000010732199";
  //    String qidWal = "0320000002861455";
  //    String qidRogue = "9920000002874536";
  //
  //    List<UacQidLink> uacQidLinks = new LinkedList<>();
  //
  //    UacQidLink uacQidLink = new UacQidLink();
  //    uacQidLink.setUac(uacEng);
  //    uacQidLink.setQid(qidEng);
  //    uacQidLinks.add(uacQidLink);
  //
  //    uacQidLink = new UacQidLink();
  //    uacQidLink.setUac(uacWal);
  //    uacQidLink.setQid(qidWal);
  //    uacQidLinks.add(uacQidLink);
  //
  //    uacQidLink = new UacQidLink();
  //    uacQidLink.setUac(uacRogue);
  //    uacQidLink.setQid(qidRogue);
  //    uacQidLinks.add(uacQidLink);
  //
  //    when(uacQidLinkRepository.findByCaseId(eq(testCase.getCaseId().toString())))
  //        .thenReturn(uacQidLinks);
  //
  //    // When
  //    ActionInstructionBuilder underTest = new ActionInstructionBuilder(uacQidLinkRepository);
  //    underTest.buildPrinterActionInstruction(testCase, actionRule);
  //
  //    // Then
  //    // Expect exception to be thrown
  //  }
  //
  //  @Test(expected = RuntimeException.class)
  //  public void testWalesQuestionnaireWithMissingQidUac() {
  //    // Given
  //    EasyRandom easyRandom = new EasyRandom();
  //    ActionPlan actionPlan = easyRandom.nextObject(ActionPlan.class);
  //    ActionRule actionRule = new ActionRule();
  //    actionRule.setActionPlan(actionPlan);
  //    actionRule.setActionType(ActionType.ICL1E);
  //    Case testCase = easyRandom.nextObject(Case.class);
  //    testCase.setTreatmentCode("HH_QF2R1W");
  //    String uacEng = easyRandom.nextObject(String.class);
  //    String qidEng = "0220000010732199";
  //
  //    List<UacQidLink> uacQidLinks = new LinkedList<>();
  //
  //    UacQidLink uacQidLink = new UacQidLink();
  //    uacQidLink.setUac(uacEng);
  //    uacQidLink.setQid(qidEng);
  //    uacQidLinks.add(uacQidLink);
  //
  //    when(uacQidLinkRepository.findByCaseId(eq(testCase.getCaseId().toString())))
  //        .thenReturn(uacQidLinks);
  //
  //    // When
  //    ActionInstructionBuilder underTest = new ActionInstructionBuilder(uacQidLinkRepository);
  //    underTest.buildPrinterActionInstruction(testCase, actionRule);
  //
  //    // Then
  //    // Expect exception to be thrown
  //  }
}
