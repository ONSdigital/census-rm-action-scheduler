package uk.gov.ons.census.action.model.repository;

import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import uk.gov.ons.census.action.model.entity.Case;

@Repository
public class CustomCaseRepository {

  private EntityManager entityManager;

  public CustomCaseRepository(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public Stream<Case> streamAll(Specification<Case> spec) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Case> query = cb.createQuery(Case.class);
    Root<Case> root = query.from(Case.class);
    query.where(spec.toPredicate(root, query, cb));

    return entityManager.createQuery(query).getResultStream();
  }
}
