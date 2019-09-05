package uk.gov.ons.census.action.client;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.ons.census.action.model.dto.UacCreateDTO;
import uk.gov.ons.census.action.model.dto.UacQidDTO;

@Component
public class CaseClient {
  @Value("${caseapi.host}")
  private String host;

  @Value("${caseapi.port}")
  private String port;

  public UacQidDTO getUacQid(UUID caseId, String questionnaireType) {
    RestTemplate restTemplate = new RestTemplate();
    UacCreateDTO caseDetails = new UacCreateDTO();
    caseDetails.setCaseId(caseId);
    caseDetails.setQuestionnaireType(questionnaireType);

    UriComponents uriComponents = createUriComponents("/uacqid/create/");
    return restTemplate.postForObject(uriComponents.toUri(), caseDetails, UacQidDTO.class);
  }

  private UriComponents createUriComponents(String path) {
    return UriComponentsBuilder.newInstance()
        .scheme("http")
        .host(host)
        .port(port)
        .path(path)
        .buildAndExpand();
  }
}
