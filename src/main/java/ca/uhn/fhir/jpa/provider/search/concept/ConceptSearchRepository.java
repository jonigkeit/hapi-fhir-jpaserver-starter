package ca.uhn.fhir.jpa.provider.search.concept;

import ca.uhn.fhir.jpa.entity.TermConcept;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class ConceptSearchRepository {

	@PersistenceContext
	private EntityManager myEntityManager;

	@Transactional(readOnly = true)
	public List<TermConcept> findByCsvPidAndDisplayLike(Long theCsvPid, String theValue, int theOffset, int theLimit) {

		// Use LOWER(...) + LIKE for portable "contains" matching
		return myEntityManager.createQuery(
				"SELECT c " +
					"FROM TermConcept c " +
					"WHERE c.myCodeSystemVersionPid = :csvPid " +
					"AND LOWER(c.myDisplay) LIKE LOWER(CONCAT('%', :val, '%')) " +
					"ORDER BY c.myCode",
				TermConcept.class)
			.setParameter("csvPid", theCsvPid)
			.setParameter("val", theValue)
			.setFirstResult(theOffset)
			.setMaxResults(theLimit)
			.getResultList();
	}

	@Transactional(readOnly = true)
	public Long countByCsvPidAndDisplayLike(Long theCsvPid, String theValue) {
		return myEntityManager.createQuery(
				"SELECT COUNT(c) " +
					"FROM TermConcept c " +
					"WHERE c.myCodeSystemVersionPid = :csvPid " +
					"AND LOWER(c.myDisplay) LIKE LOWER(CONCAT('%', :val, '%'))",
				Long.class)
			.setParameter("csvPid", theCsvPid)
			.setParameter("val", theValue)
			.getSingleResult();
	}
}