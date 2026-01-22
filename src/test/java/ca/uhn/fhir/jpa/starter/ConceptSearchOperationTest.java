package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {Application.class}, properties = {
		"hapi.fhir.fhir_version=r4",
		"spring.datasource.url=jdbc:h2:mem:dbr4_concept_search"
})
class ConceptSearchOperationTest {

	@LocalServerPort
	private int port;

	private IGenericClient client;
	private String ourServerBase;

	@BeforeEach
	void setUp() {
		FhirContext ctx = FhirContext.forR4();
		ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourServerBase = "http://localhost:" + port + "/fhir/";
		client = ctx.newRestfulGenericClient(ourServerBase);
	}

	@Test
	void testConceptSearch_DataCorrectness() {
		String csUrl = "http://example.com/cs-data";
		createCodeSystemWithConcepts(csUrl, 100);

		Bundle bundle = client.operation()
			.onType(CodeSystem.class)
			.named("$concept-search")
			.withParameter(Parameters.class, "url", new UriType(csUrl))
			.andParameter("value", new StringType("Concept 42"))
			.returnResourceType(Bundle.class)
			.execute();

		assertNotNull(bundle);
		assertEquals(1, bundle.getEntry().size());
		Parameters p = (Parameters) bundle.getEntry().get(0).getResource();
		Coding coding = (Coding) p.getParameter().get(0).getValue();
		assertEquals("code42", coding.getCode());
		assertEquals("Concept 42", coding.getDisplay());
		assertEquals(csUrl, coding.getSystem());
	}

	@Test
	void testConceptSearch_Paging() {
		String csUrl = "http://example.com/cs-paging";
		createCodeSystemWithConcepts(csUrl, 1000);

		// We use a raw search/GET to test paging parameters like _count
		String url = ourServerBase + "CodeSystem/$concept-search?url=" + csUrl + "&value=Concept&_count=10";
		Bundle bundle = client.fetchResourceFromUrl(Bundle.class, url);

		assertNotNull(bundle);
		assertEquals(10, bundle.getEntry().size());
	}

	private void createCodeSystemWithConcepts(String url, int count) {
		CodeSystem cs = new CodeSystem();
		cs.setUrl(url);
		cs.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE);
		cs.setContent(CodeSystem.CodeSystemContentMode.COMPLETE);
		for (int i = 0; i < count; i++) {
			cs.addConcept()
				.setCode("code" + i)
				.setDisplay("Concept " + i);
		}
		client.create().resource(cs).execute();
	}
}
