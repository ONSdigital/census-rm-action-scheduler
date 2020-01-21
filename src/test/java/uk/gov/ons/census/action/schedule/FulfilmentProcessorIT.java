package uk.gov.ons.census.action.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
import uk.gov.ons.census.action.model.entity.FulfilmentToSend;
import uk.gov.ons.census.action.model.repository.FulfilmentToSendRepository;

@ContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
public class FulfilmentProcessorIT {

  @Autowired FulfilmentToSendRepository fulfilmentToSendRepository;
  @Autowired FulfilmentProcessor fulfilmentProcessor;

  @Before
  @Transactional
  public void setUp() {
    fulfilmentToSendRepository.deleteAll();
  }

  @Test
  public void TestAddingBatchIdAndQuantity() {
    // Given

    EasyRandom easyRandom = new EasyRandom();
    PrintFileDto printFileDto = easyRandom.nextObject(PrintFileDto.class);

    FulfilmentToSend fulfilmentsBeforeQuantityAndBatchId = new FulfilmentToSend();

    fulfilmentsBeforeQuantityAndBatchId.setFulfilmentCode("P_OR_H1");
    fulfilmentsBeforeQuantityAndBatchId.setMessageData(printFileDto);

    fulfilmentToSendRepository.saveAndFlush(fulfilmentsBeforeQuantityAndBatchId);

    // When
    fulfilmentProcessor.addFulfilmentBatchIdAndQuantity();

    // Then

    List<FulfilmentToSend> fulfilmentToSend = fulfilmentToSendRepository.findAll();
    assertThat(fulfilmentToSend.get(0).getBatchId()).isNotNull();
    assertThat(fulfilmentToSend.get(0).getQuantity()).isNotNull();
    assertThat(fulfilmentToSend.get(0).getQuantity()).isEqualTo(1);
  }
}
