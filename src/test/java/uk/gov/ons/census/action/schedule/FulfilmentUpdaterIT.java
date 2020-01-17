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
public class FulfilmentUpdaterIT {

  @Autowired FulfilmentsToSendRepository fulfilmentsToSendRepository;
  @Autowired FulfilmentUpdater fulfilmentUpdater;

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

    FulfilmentsToSend fulfilmentsToSend = new FulfilmentsToSend();

    fulfilmentsToSend.setFulfilmentCode("P_OR_H1");
    fulfilmentsToSend.setMessageData(stringPrintFile);

    fulfilmentsToSendRepository.saveAndFlush(fulfilmentsToSend);

    // When
    fulfilmentUpdater.addFulfilmentBatchIdAndQuantity();

    // Then

    FulfilmentsToSend fulfilmentsToSend1 =
        fulfilmentsToSendRepository.findByFulfilmentCode("P_OR_H1");
    assertThat(fulfilmentsToSend1.getBatchId()).isNotNull();
    assertThat(fulfilmentsToSend1.getQuantity()).isNotNull();
    assertThat(fulfilmentsToSend1.getQuantity()).isEqualTo(1);
  }
}
