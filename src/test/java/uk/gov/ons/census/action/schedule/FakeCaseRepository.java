package uk.gov.ons.census.action.schedule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.assertj.core.api.AssertionsForClassTypes;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.ons.census.action.model.entity.Case;
import uk.gov.ons.census.action.model.repository.CaseRepository;

public class FakeCaseRepository implements CaseRepository {

  private final List<Case> returnCases;
  private final Specification<Case> expectedSpec;

  public FakeCaseRepository(List<Case> returnCases, Specification<Case> expectedSpec) {
    this.returnCases = returnCases;
    this.expectedSpec = expectedSpec;
  }

  @Override
  public Optional<Case> findByCaseId(UUID caseId) {
    return Optional.empty();
  }

  @Override
  public Stream<Case> findByActionPlanIdAndReceiptReceivedIsFalse(String actionPlanId) { return Stream.empty(); }

  @Override
  public Optional<Case> findOne(Specification<Case> specification) {
    return Optional.empty();
  }

  public List<Case> findAll(Specification<Case> specification) {
    AssertionsForClassTypes.assertThat(specification)
        .isEqualToComparingFieldByFieldRecursively(expectedSpec);

    return returnCases;
  }

  @Override
  public Page<Case> findAll(Specification<Case> specification, Pageable pageable) {
    return null;
  }

  @Override
  public List<Case> findAll(Specification<Case> specification, Sort sort) {
    return null;
  }

  @Override
  public long count(Specification<Case> specification) {
    return 0;
  }

  @Override
  public List<Case> findAll() {
    return null;
  }

  @Override
  public List<Case> findAll(Sort sort) {
    return null;
  }

  @Override
  public Page<Case> findAll(Pageable pageable) {
    return null;
  }

  @Override
  public List<Case> findAllById(Iterable<UUID> iterable) {
    return null;
  }

  @Override
  public long count() {
    return 0;
  }

  @Override
  public void deleteById(UUID uuid) {}

  @Override
  public void delete(Case aCase) {}

  @Override
  public void deleteAll(Iterable<? extends Case> iterable) {}

  @Override
  public void deleteAll() {}

  @Override
  public <S extends Case> S save(S s) {
    return null;
  }

  @Override
  public <S extends Case> List<S> saveAll(Iterable<S> iterable) {
    return null;
  }

  @Override
  public Optional<Case> findById(UUID uuid) {
    return Optional.empty();
  }

  @Override
  public boolean existsById(UUID uuid) {
    return false;
  }

  @Override
  public void flush() {}

  @Override
  public <S extends Case> S saveAndFlush(S s) {
    return null;
  }

  @Override
  public void deleteInBatch(Iterable<Case> iterable) {}

  @Override
  public void deleteAllInBatch() {}

  @Override
  public Case getOne(UUID uuid) {
    return null;
  }

  @Override
  public <S extends Case> Optional<S> findOne(Example<S> example) {
    return Optional.empty();
  }

  @Override
  public <S extends Case> List<S> findAll(Example<S> example) {
    return null;
  }

  @Override
  public <S extends Case> List<S> findAll(Example<S> example, Sort sort) {
    return null;
  }

  @Override
  public <S extends Case> Page<S> findAll(Example<S> example, Pageable pageable) {
    return null;
  }

  @Override
  public <S extends Case> long count(Example<S> example) {
    return 0;
  }

  @Override
  public <S extends Case> boolean exists(Example<S> example) {
    return false;
  }
}
