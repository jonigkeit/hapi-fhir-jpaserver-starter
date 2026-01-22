package ca.uhn.fhir.jpa.provider.search.concept;

import ca.uhn.fhir.jpa.dao.data.ITermCodeSystemDao;
import ca.uhn.fhir.jpa.entity.TermCodeSystem;
import ca.uhn.fhir.jpa.entity.TermCodeSystemVersion;
import ca.uhn.fhir.jpa.entity.TermConcept;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConceptSearchSvc {

	private final ITermCodeSystemDao myTermCodeSystemDao;
	private final ConceptSearchRepository myConceptSearchRepository;

	public ConceptSearchSvc(
		ITermCodeSystemDao theTermCodeSystemDao,
		ConceptSearchRepository theConceptSearchRepository
	) {
		myTermCodeSystemDao = theTermCodeSystemDao;
		myConceptSearchRepository = theConceptSearchRepository;
	}

	@Transactional(readOnly = true)
	public IBundleProvider searchConceptsPaged(RequestDetails theRequestDetails, String theCodeSystemUrl, String theValue) {
		// 1) Resolve CodeSystem -> current version PID using HAPI terminology tables (fast, indexed)
		TermCodeSystem cs = myTermCodeSystemDao.findByCodeSystemUri(theCodeSystemUrl);
		if (cs == null) {
			throw new ResourceNotFoundException("No CodeSystem found for url: " + theCodeSystemUrl);
		}
		TermCodeSystemVersion currentVer = cs.getCurrentVersion();
		if (currentVer == null || currentVer.getPid() == null) {
			throw new ResourceNotFoundException("CodeSystem has no current version: " + theCodeSystemUrl);
		}
		final Long csvPid = currentVer.getPid();

		// 2) Return an IBundleProvider so HAPI paging kicks in
		return new IBundleProvider() {

			// Optional: let HAPI decide default page size unless client supplies _count
			@Override
			public Integer preferredPageSize() {
				return null;
			}

			@Override
			public Integer size() {
				// COUNT(*) with the same filters -> enables total if HAPI decides to use it
				return myConceptSearchRepository.countByCsvPidAndDisplayLike(csvPid, theValue).intValue();
			}

			@Override
			public List<IBaseResource> getResources(int theFromIndex, int theToIndex) {
				int pageSize = Math.max(0, theToIndex - theFromIndex);
				if (pageSize == 0) return List.of();

				List<TermConcept> concepts =
					myConceptSearchRepository.findByCsvPidAndDisplayLike(csvPid, theValue, theFromIndex, pageSize);

				// Represent each concept as a Parameters resource (bundle entries must be resources)
				return concepts.stream().map(c -> {
					Parameters p = new Parameters();
					p.setId(UUID.randomUUID().toString());
					p.addParameter()
						.setName("concept")
						.setValue(new Coding(theCodeSystemUrl, c.getCode(), c.getDisplay()));
					return p;
				}).collect(Collectors.toList());
			}

			@Override
			public IPrimitiveType<Date> getPublished() {
				return null;
			}

			@Override
			public String getUuid() {
				// OK with default FIFO paging provider (HAPI docs)
				return null;
			}
		};
	}
}