package uk.gov.ons.census.action.schedule;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.ons.census.action.model.entity.FulfilmentMapper;
import uk.gov.ons.census.action.model.repository.FulfilmentToSendRepository;

public class FulfilmentsProcessorTest {
  private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
  private final FulfilmentToSendRepository fulfilmentToSendRepository =
      mock(FulfilmentToSendRepository.class);

  @Test
  public void TestAddBatchIdAndQuantity() {
    FulfilmentMapper fulfilmentMapper = new FulfilmentMapper("P_OR_H1", 1L);
    List<FulfilmentMapper> fulfilmentsToSend = new ArrayList<>();
    fulfilmentsToSend.add(fulfilmentMapper);

    when(fulfilmentToSendRepository.findCountOfFulfilments()).thenReturn(fulfilmentsToSend);

    // When
    FulfilmentProcessor fulfilmentProcessor =
        new FulfilmentProcessor(jdbcTemplate, fulfilmentToSendRepository);
    fulfilmentProcessor.addFulfilmentBatchIdAndQuantity();

    // Then
    String EXPECTED_UPDATE_QUERY =
        "UPDATE actionv2.fulfilment_to_send SET quantity = ?, batch_id = ? where fulfilment_code = ? ";
    verify(jdbcTemplate, times(1))
        .update(
            eq(EXPECTED_UPDATE_QUERY),
            eq(fulfilmentMapper.getCount()),
            any(UUID.class),
            eq(fulfilmentMapper.getFulfilmentCode()));
  }

  @Test
  public void TestwhenNoFulfilmentsAreInDatabase() {

    List<FulfilmentMapper> emptyFulfilmentsList = new ArrayList<>();
    when(fulfilmentToSendRepository.findCountOfFulfilments()).thenReturn(emptyFulfilmentsList);

    // When
    FulfilmentProcessor fulfilmentProcessor =
        new FulfilmentProcessor(jdbcTemplate, fulfilmentToSendRepository);
    fulfilmentProcessor.addFulfilmentBatchIdAndQuantity();
    // Then
    String EXPECTED_QUERY =
        "select fulfilment_code, count(*) from actionv2.fulfilment_to_send group by fulfilment_code";
    verify(fulfilmentToSendRepository, times(1)).findCountOfFulfilments();
    verifyNoMoreInteractions(fulfilmentToSendRepository);
  }
}
