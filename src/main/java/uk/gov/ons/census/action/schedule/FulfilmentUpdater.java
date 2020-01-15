package uk.gov.ons.census.action.schedule;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.action.model.dto.FulfilmentToSend;
import uk.gov.ons.census.action.utility.FulfilmentMapper;

import java.util.List;
import java.util.UUID;

@Component
public class FulfilmentUpdater {

  private JdbcTemplate jdbcTemplate;

  public FulfilmentUpdater(JdbcTemplate jdbcTemplate){
    this.jdbcTemplate = jdbcTemplate;
  }

  public void addFulfilmentBatchIdAndQuantity(){
    String query = "select fulfilment_code, count(*) from actionv2.fulfilments_to_send group by fulfilment_code ";
    List<FulfilmentToSend> fulfilmentsToSend = jdbcTemplate.query(
            query, new FulfilmentMapper());

    jdbcTemplate.update("INSERT INTO actionv2.fulfilments_to_send(quantity, batch_id) VALUES(?, ?)", fulfilmentsToSend, UUID.randomUUID());



  }
}
