package uk.gov.ons.census.action.schedule;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.ons.census.action.model.entity.FulfilmentMapper;

public class FulfilmentsProcessorTest {
  private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

  @Test
  public void TestAddBatchIdAndQuantity() {
    FulfilmentMapper fulfilmentMapper = new FulfilmentMapper();
    fulfilmentMapper.setCount(1);
    fulfilmentMapper.setFulfilmentCode("P_OR_H1");
    List<FulfilmentMapper> fulfilmentsToSend = new ArrayList<>();
    fulfilmentsToSend.add(fulfilmentMapper);

    when(jdbcTemplate.query(
            any(String.class), any(uk.gov.ons.census.action.utility.FulfilmentMapper.class)))
        .thenReturn(fulfilmentsToSend);

    // When
    FulfilmentProcessor fulfilmentProcessor = new FulfilmentProcessor(jdbcTemplate);
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
    when(jdbcTemplate.query(
            any(String.class), any(uk.gov.ons.census.action.utility.FulfilmentMapper.class)))
        .thenReturn(emptyFulfilmentsList);
    // When
    FulfilmentProcessor fulfilmentProcessor = new FulfilmentProcessor(jdbcTemplate);
    fulfilmentProcessor.addFulfilmentBatchIdAndQuantity();
    // Then
    String EXPECTED_QUERY =
        "select fulfilment_code, count(*) from actionv2.fulfilment_to_send group by fulfilment_code";
    verify(jdbcTemplate, times(1))
        .query(eq(EXPECTED_QUERY), any(uk.gov.ons.census.action.utility.FulfilmentMapper.class));
    verifyNoMoreInteractions(jdbcTemplate);
  }
}
