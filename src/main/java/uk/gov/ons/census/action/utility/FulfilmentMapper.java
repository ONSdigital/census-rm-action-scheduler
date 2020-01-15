package uk.gov.ons.census.action.utility;

import org.springframework.jdbc.core.RowMapper;
import uk.gov.ons.census.action.model.dto.FulfilmentToSend;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FulfilmentMapper implements RowMapper<FulfilmentToSend> {

  @Override
  public FulfilmentToSend mapRow(ResultSet rs, int rowNum) throws SQLException {
    FulfilmentToSend fulfilmentToSend = new FulfilmentToSend();

//    fulfilmentToSend.setEvent_payload(rs.getString("message_data"));
    fulfilmentToSend.setFulfilment_code(rs.getString("fulfilment_code"));
    fulfilmentToSend.setCount(rs.getInt("count"));


    return fulfilmentToSend;
  }
}
