package uk.gov.ons.census.action.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.census.action.utility.JsonHelper.convertObjectToJson;

import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ons.census.action.model.dto.PrintFileDto;
import uk.gov.ons.census.action.model.entity.FulfilmentsToSend;
import uk.gov.ons.census.action.model.repository.FulfilmentsToSendRepository;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class FulfilmentProcessorIT {

  @Autowired FulfilmentsToSendRepository fulfilmentsToSendRepository;
  @Autowired FulfilmentProcessor fulfilmentProcessor;

  @Before
  @Transactional
  public void setUp() {
    fulfilmentsToSendRepository.deleteAll();
  }

  @Test
  public void TestAddingBatchIdAndQuantity() {
    // Given

    EasyRandom easyRandom = new EasyRandom();
    PrintFileDto printFileDto = easyRandom.nextObject(PrintFileDto.class);

    String stringPrintFile = convertObjectToJson(printFileDto);

    FulfilmentsToSend fulfilmentsBeforeQuantityAndBatchId = new FulfilmentsToSend();

    fulfilmentsBeforeQuantityAndBatchId.setFulfilmentCode("P_OR_H1");
    fulfilmentsBeforeQuantityAndBatchId.setMessageData(stringPrintFile);

    fulfilmentsToSendRepository.saveAndFlush(fulfilmentsBeforeQuantityAndBatchId);

    // When
    fulfilmentProcessor.addFulfilmentBatchIdAndQuantity();

    // Then

    FulfilmentsToSend fulfilmentsToSend =
        fulfilmentsToSendRepository.findByFulfilmentCode("P_OR_H1");
    assertThat(fulfilmentsToSend.getBatchId()).isNotNull();
    assertThat(fulfilmentsToSend.getQuantity()).isNotNull();
    assertThat(fulfilmentsToSend.getQuantity()).isEqualTo(1);
  }
}
