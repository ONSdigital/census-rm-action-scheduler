package uk.gov.ons.census.action.schedule;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.model.entity.FulfilmentMapper;

@Component
public class FulfilmentProcessor {

  private JdbcTemplate jdbcTemplate;

  public FulfilmentProcessor(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void addFulfilmentBatchIdAndQuantity() {
    String query =
        "select fulfilment_code, count(*) from actionv2.fulfilment_to_send group by fulfilment_code";
    List<FulfilmentMapper> fulfilmentsToSend =
        jdbcTemplate.query(query, new uk.gov.ons.census.action.utility.FulfilmentMapper());

    fulfilmentsToSend.forEach(
        (fulfilment) -> {
          jdbcTemplate.update(
              "UPDATE actionv2.fulfilment_to_send SET quantity = ?, batch_id = ? "
                  + "where fulfilment_code = ? ",
              fulfilment.getCount(),
              UUID.randomUUID(),
              fulfilment.getFulfilmentCode());
        });
  }
}
