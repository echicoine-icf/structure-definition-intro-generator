package com.icf.ecqm.structuredefinition.introgenerator.qicore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.icf.ecqm.structuredefinition.introgenerator.common.AbstractProcessor;

import java.util.ArrayList;
import java.util.List;

public class QICoreProcessor extends AbstractProcessor {

    private static final String mustHaveTag = "Must Have:";
    private static final String qiTag = "QI Elements:";
    private static final String PRIMARY_CODE_PATH = "Primary code path:";
    private static final String CODE_PATH_URL = "http://hl7.org/fhir/StructureDefinition/cqf-modelInfo-primaryCodePath";
    private static final String URL = "url";
    private static final String EXTENSION = "extension";
    private static final String VALUE_STRING = "valueString";

    public QICoreProcessor() {
        super("output", "input/intro-notes");
    }

    @Override
    public String buildStructureDefinitionIntro(String jsonString) {
        JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();

        List<String> mustHaveElements = new ArrayList<>();
        List<String> qiElements = new ArrayList<>();

        // Check for Primary Code Path extension
        String primaryCodePath = "";
        if (root.has(EXTENSION)) {
            JsonArray extensions = root.getAsJsonArray(EXTENSION);
            for (JsonElement extElement : extensions) {
                JsonObject extObj = extElement.getAsJsonObject();
                if (extObj.has(URL) && extObj.get(URL).getAsString().equals(CODE_PATH_URL)) {
                    if (extObj.has(VALUE_STRING)) {
                        primaryCodePath = extObj.get(VALUE_STRING).getAsString();
                        break;
                    }
                }
            }
        }

        JsonArray elements = root.getAsJsonObject(SNAPSHOT).getAsJsonArray(ELEMENT);

        for (JsonElement element : elements) {
            JsonObject elementObj = element.getAsJsonObject();
            String elementName = elementObj.get("path").getAsString();
            String shortDesc = elementObj.has(SHORT) ? elementObj.get(SHORT).getAsString() : "";

            boolean isMustHave = false;
            if (elementObj.has(MIN) && elementObj.has(MAX)) {
                int min = elementObj.get(MIN).getAsInt();
                String max = elementObj.get(MAX).getAsString();
                if (min == 1 && (max.equals("1") || max.equals("*"))) {
                    isMustHave = true;
                }
            }

            if (isMustHave) {
                mustHaveElements.add(elementName + ": " + shortDesc);
            } else {
                if (elementObj.has(EXTENSION)) {
                    JsonArray extensions = elementObj.getAsJsonArray(EXTENSION);
                    for (JsonElement extElement : extensions) {
                        JsonObject extObj = extElement.getAsJsonObject();
                        if (extObj.has(URL) && extObj.get(URL).getAsString().equals(CODE_PATH_URL)) {
                            qiElements.add(elementName + ": " + shortDesc);
                        }
                    }
                }
            }
        }

        StringBuilder output = new StringBuilder();
        if (!mustHaveElements.isEmpty()) {
            output.append("<b>").append(mustHaveTag).append("</b>\n<ul>\n");
            for (String element : mustHaveElements) {
                output.append("<li>").append(element).append("</li>\n");
            }
            output.append("</ul>\n\n");
        }

        if (!qiElements.isEmpty()) {
            output.append("<b>").append(qiTag).append("</b>\n<ul>\n");
            for (String element : qiElements) {
                output.append("<li>").append(element).append("</li>\n");
            }
            output.append("</ul>\n\n");
        }

        if (!primaryCodePath.isEmpty()) {
            output.append("<b>").append(PRIMARY_CODE_PATH).append("</b> ")
                    .append(primaryCodePath)
                    .append("\n<br></br>\n(PCPath) This element is the primary code path for this resource")
                    .append("\n\n");
        }

        return output.length() > 0 ? beginTag + "\n" + output + endTag : "";
    }
}
