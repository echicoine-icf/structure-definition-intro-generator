import com.icf.ecqm.structuredefinition.introgenerator.qicore.QICoreProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QICoreProcessorTest {

    private QICoreProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new QICoreProcessor();
    }

    @Test
    void testMustHaveAndQIElements() {
        String jsonInput = "{"
                + "\"id\": \"test-qicore\","
                + "\"snapshot\": {"
                + "  \"element\": ["
                + "    { \"path\": \"Observation.category\", \"short\": \"Category\", \"min\": 1, \"max\": \"1\" },"
                + "    { \"path\": \"Observation.value\", \"short\": \"Value\", \"extension\": ["
                + "      { \"url\": \"http://hl7.org/fhir/StructureDefinition/cqf-modelInfo-primaryCodePath\", \"valueString\": \"some-value\" }"
                + "    ] }"
                + "  ]"
                + "}"
                + "}";

        String output = processor.buildStructureDefinitionIntro(jsonInput);

        // Verify Must-Have Section
        assertTrue(output.contains("Must Have:"));
        assertTrue(output.contains("Observation.category: Category"));

        // Verify QI Elements Section
        assertTrue(output.contains("QI Elements:"));
        assertTrue(output.contains("Observation.value: Value"));
    }

    @Test
    void testEmptyElementsShouldReturnEmptyString() {
        String jsonInput = "{"
                + "\"id\": \"test-qicore\","
                + "\"snapshot\": { \"element\": [] }"
                + "}";

        String output = processor.buildStructureDefinitionIntro(jsonInput);
        assertEquals("", output);
    }
}
