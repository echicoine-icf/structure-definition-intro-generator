package com.icf.ecqm.structuredefinition.introgenerator.deqm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.icf.ecqm.structuredefinition.introgenerator.common.AbstractProcessor;

import java.util.ArrayList;
import java.util.List;

public class DEQMProcessor extends AbstractProcessor {

    private static final String mustHaveTag = "Each [type] Must Have:";
    private static final String mustSupportTag = "Each [type] Must Support:";

    public DEQMProcessor() {
        super("output", "input/pagecontent");
    }

    @Override
    public String buildStructureDefinitionIntro(String jsonString) {
        JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();
        List<String> mustHaveElements = new ArrayList<>();
        List<String> mustSupportElements = new ArrayList<>();

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
            } else if (elementObj.has(MUST_SUPPORT) && elementObj.get(MUST_SUPPORT).getAsBoolean()) {
                mustSupportElements.add(elementName + ": " + shortDesc);
            }
        }

        String thisMustHaveTag = mustHaveTag.replace("[type]", "{{site.data.structuredefinitions.[id].type}}");
        String thisMustSupportTag = mustSupportTag.replace("[type]", "{{site.data.structuredefinitions.[id].type}}");

        StringBuilder output = new StringBuilder();
        if (!mustHaveElements.isEmpty()) {
            output.append("<b>").append(thisMustHaveTag).append("</b>\n<ul>\n");
            for (String element : mustHaveElements) {
                output.append("<li>").append(element).append("</li>\n");
            }
            output.append("</ul>\n\n");
        }

        if (!mustSupportElements.isEmpty()) {
            output.append("<b>").append(thisMustSupportTag).append("</b>\n<ul>\n");
            for (String element : mustSupportElements) {
                output.append("<li>").append(element).append("</li>\n");
            }
            output.append("</ul>\n\n");
        }

        return output.length() > 0 ? beginTag + "\n" + output + endTag : "";
    }
}
