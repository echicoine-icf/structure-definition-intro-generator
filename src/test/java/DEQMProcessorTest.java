import com.icf.ecqm.structuredefinition.introgenerator.deqm.DEQMProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DEQMProcessorTest {

    private DEQMProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new DEQMProcessor();
    }

    @Test
    void testMustHaveAndMustSupportElements() {
        String jsonInput = "{"
                + "\"id\": \"test-structure\","
                + "\"type\": \"Observation\","
                + "\"snapshot\": {"
                + "  \"element\": ["
                + "    { \"path\": \"Observation.status\", \"short\": \"Status\", \"min\": 1, \"max\": \"1\" },"
                + "    { \"path\": \"Observation.value\", \"short\": \"Value\", \"mustSupport\": true }"
                + "  ]"
                + "}"
                + "}";

        String output = processor.buildStructureDefinitionIntro(jsonInput);

        // Verify Must-Have Section
        assertTrue(output.contains("Each {{site.data.structuredefinitions.[id].type}} Must Have:"));
        assertTrue(output.contains("Observation.status: Status"));

        // Verify Must-Support Section
        assertTrue(output.contains("Each {{site.data.structuredefinitions.[id].type}} Must Support:"));
        assertTrue(output.contains("Observation.value: Value"));
    }

    @Test
    void testEmptyElementsShouldReturnEmptyString() {
        String jsonInput = "{"
                + "\"id\": \"test-structure\","
                + "\"type\": \"Observation\","
                + "\"snapshot\": { \"element\": [] }"
                + "}";

        String output = processor.buildStructureDefinitionIntro(jsonInput);
        assertEquals("", output);
    }
}
