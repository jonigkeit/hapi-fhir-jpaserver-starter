package ca.uhn.fhir.jpa.provider.search.concept;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.springframework.stereotype.Component;

@Component
public class CodeSystemConceptSearchOperationProvider {

	private final ConceptSearchSvc myConceptSearchSvc;

	public CodeSystemConceptSearchOperationProvider(ConceptSearchSvc theConceptSearchSvc) {
		myConceptSearchSvc = theConceptSearchSvc;
	}

	@Operation(name = "$concept-search", idempotent = true, type = CodeSystem.class)
	public IBundleProvider conceptSearch(
		RequestDetails theRequestDetails,
		@OperationParam(name = "url", min = 1, max = 1) UriType theUrl,
		@OperationParam(name = "value", min = 1, max = 1) StringType theValue
	) {
		String url = theUrl != null ? theUrl.getValueAsString() : null;
		String value = theValue != null ? theValue.getValue() : null;

		if (url == null || url.isBlank()) {
			throw new InvalidRequestException("Missing required parameter: url");
		}
		if (value == null) {
			throw new InvalidRequestException("Missing required parameter: value");
		}

		return myConceptSearchSvc.searchConceptsPaged(theRequestDetails, url, value);
	}
}