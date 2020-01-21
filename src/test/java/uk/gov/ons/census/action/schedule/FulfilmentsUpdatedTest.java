package uk.gov.ons.census.action.schedule;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import uk.gov.ons.census.action.model.entity.FulfilmentMapper;

public class FulfilmentsUpdatedTest {
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
    verify(jdbcTemplate)
        .update(
            any(String.class),
            eq(fulfilmentMapper.getCount()),
            any(UUID.class),
            eq(fulfilmentMapper.getFulfilmentCode()));
  }

  @Test
  public void TestNoFulfilmentsInDatabase() {

    // When
    FulfilmentProcessor fulfilmentProcessor = new FulfilmentProcessor(jdbcTemplate);
    fulfilmentProcessor.addFulfilmentBatchIdAndQuantity();

    // Then
    verify(jdbcTemplate, never()).update((PreparedStatementCreator) any(), any());
  }
}
