package uk.gov.ons.census.action.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import uk.gov.ons.census.action.model.dto.PrintFileDto;

public class JsonHelper {
  private static final ObjectMapper objectMapper;

  static {
    objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  public static String convertObjectToJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed converting Object To Json", e);
    }
  }

  public static PrintFileDto convertJsonToObject(String printFileString) {
    try {
      return objectMapper.readValue(printFileString, PrintFileDto.class);
    } catch (IOException e) {
      throw new RuntimeException("Failed converting Json To Object", e);
    }
  }
}
