package uk.gov.ons.census.action.utility;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import uk.gov.ons.census.action.model.dto.FulfilmentToSend;

public class FulfilmentMapper implements RowMapper<FulfilmentToSend> {

  @Override
  public FulfilmentToSend mapRow(ResultSet rs, int rowNum) throws SQLException {
    FulfilmentToSend fulfilmentToSend = new FulfilmentToSend();

    fulfilmentToSend.setFulfilment_code(rs.getString("fulfilment_code"));
    fulfilmentToSend.setCount(rs.getInt("count"));

    return fulfilmentToSend;
  }
}
