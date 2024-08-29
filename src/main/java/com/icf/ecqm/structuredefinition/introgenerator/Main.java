package com.icf.ecqm.structuredefinition.introgenerator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.util.*;

public class Main {
    public static final String ASSIGN_ID = "{% assign id = {{include.id}} %}";
    public static final String FIXED_CODE = "fixedCode";
    private static final String SNAPSHOT = "snapshot";
    private static final String ELEMENT = "element";
    private static final String STRUCTURE_DEFINITION = "StructureDefinition";
    private static final String ID = "id";
    private static final String SHORT = "short";
    private static final String MUST_SUPPORT = "mustSupport";
    //TODO: Possible align with fhir-qi-core which uses "intro-notes" as folder name:
    private static final String pageContentFolder = "input" + File.separator + "pagecontent";
    private static final String outputFolder = "output";
    private static final String beginTag = "<!--Begin Generated Intro Tag (DO NOT REMOVE)-->";
    private static final String endTag = "<!--End Generated Intro (DO NOT REMOVE)-->";
    private static final String mustHaveTag = "Each [type] Must Have:";
    private static final String mustSupportTag = "Each [type] Must Support:";
    private static final String MIN = "min";
    private static final String MAX = "max";

    private static final String mainTitle = "### Mandatory Data Elements and Terminology\nThe following data-elements are mandatory (i.e data MUST be present).\n\n";

    private static final String[] PARENT_TYPES = {"extension", "entry"};


    /**
     * This tool will generate intro files in html within the davinci-deqm\input\intro-notes xml files corresponding to each StructureDefinition file.
     * Matches are based on filename. For example:
     * davinci-deqm\output\StructureDefinition-qicore-allergyintolerance.json
     * matches to
     * davinci-deqm\input\intro-notes\StructureDefinition-qicore-allergyintolerance-intro.xml
     * Looping through output folder guarantees we are not wasting time editing files that aren't included in the project, so generating
     * the IG before running the script will be necessary.
     *
     * @param args -ms indicates QI utilizes must support flag
     */
    public static void main(String[] args) {

        runMain();
//        runTest();
    }

    public static void runTest() {
        StringBuilder contentBuilder = new StringBuilder();
        // Read the input stream and convert it to a string
        try (InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("StructureDefinition-qicore-adverseevent.json")) {
            assert inputStream != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    contentBuilder.append(line).append(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        System.out.println("File read successfully. Content length: " + contentBuilder.length());
        System.out.println(buildStructureDefinitionIntro(contentBuilder.toString()));
    }

    private static void runMain() {
        //cycle through all json structure defintion files in output folder:
        File outputDir = new File(outputFolder);
        File[] outputFiles = outputDir.listFiles((dir, name) -> name.toLowerCase().startsWith(STRUCTURE_DEFINITION.toLowerCase()) &&
                (name.toLowerCase().endsWith(".json")));

        System.out.println("outputFiles: \n\n" + Arrays.toString(outputFiles));

        assert outputFiles != null;
        if (outputFiles.length == 0) {
            System.out.println("Output folder is empty!");
            return;
        } else {
            Map<String, String> structureDefinitionIntroMap = new HashMap<>();
            Map<String, String> mdMap = new HashMap<>();

            for (File outputFile : outputFiles) {
                System.out.println("\r\nProcessing " + outputFile.getAbsolutePath());

                try {
                    JsonObject outputJson = parseJsonFromFile(outputFile);

                    String id = outputJson.get(ID).getAsString();

                    //TODO: Align with fhir-qi-core to have intro files be .xml (or align fhir-qi-core with .md approach here)
                    String introNoteFileName = "StructureDefinition-" + id + "-intro.md";

                    String structureDefinitionIntro = buildStructureDefinitionIntro(outputJson.toString());

                    String thisTitle = JsonParser.parseString(outputJson.toString()).getAsJsonObject().get("title").getAsString();
                    String htmlFileName = "StructureDefinition-" + id + ".html";

                    //key is title:htmlFileName (split later for titling on generated page.)
                    mdMap.put(thisTitle + ":" + htmlFileName, structureDefinitionIntro);

                    if (!structureDefinitionIntro.isEmpty()) {
                        System.out.println("Intro generated: " + introNoteFileName + ": \n" + structureDefinitionIntro);
                        structureDefinitionIntroMap.put(introNoteFileName, structureDefinitionIntro);
                    } else {
                        System.out.println("No intro generated (no elements pass criteria): " + introNoteFileName + ": \n" + structureDefinitionIntro);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing file: " + outputFile.getName());
                    e.printStackTrace();
                }
            }


            //create our collection md file:
            try {
                outputMDMapToFile(mdMap);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            File inputDir = new File(pageContentFolder);

            File[] inputFiles = inputDir.listFiles((dir, name) -> name.equalsIgnoreCase(buildIntroFileNameFromJsonName(name)));

            System.out.println("Found matching files in " + pageContentFolder + ": " + Arrays.toString(inputFiles));
            System.out.println("\r\n");

            //write generated intro to corresponding file:
            if (inputFiles != null) {
                for (File inputFile : inputFiles) {
                    //write to intro file below last </div>
                    System.out.println("inputFile: " + inputFile.getName());
                    if (structureDefinitionIntroMap.containsKey(inputFile.getName())) {

                        String injectableIntroBody = structureDefinitionIntroMap.get(inputFile.getName());
                        if (injectableIntroBody.isEmpty()) continue;
                        try {
                            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                            StringBuilder content = buildContent(reader, injectableIntroBody);

                            reader.close();

                            BufferedWriter writer = new BufferedWriter(new FileWriter(inputFile));
                            writer.write(content.toString());
                            writer.close();

                            System.out.println("Injectable intro body added to: " + inputFile.getName());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                System.err.println("Error: Unable to list files in the input folder.");
            }
            System.out.println("\r\n");
        }

        System.out.println("File modification is done. Generating the IG should show updated element list in files above.");

    }

    /**
     * Turns map into string:
     * *[key](key.html)**
     * Must Have:
     * musthave entry
     * QI Elements:
     * qi entry
     *
     * @param mdMap
     */
    private static void outputMDMapToFile(Map<String, String> mdMap) throws IOException {



        StringBuilder mdPageBuilder = new StringBuilder();

        List<String> sortableKeyList = new ArrayList<>(mdMap.keySet());
        Collections.sort(sortableKeyList);

        for (String key : sortableKeyList) {
            if (mdMap.get(key).isEmpty()) continue;

//            if (!mdMap.get(key).contains(mustHaveTag)
//                    && !mdMap.get(key).contains(mustSupportTag)) {
//                continue;
//            }

            String pageContent = mdMap.get(key).replace(mainTitle, "")
                    .replace(beginTag + "\n", "")
                    .replace(endTag, "");

            if (pageContent.contains(mustHaveTag)) {
                pageContent = pageContent.replace("**" + mustSupportTag + "**\n", "\n**" + mustSupportTag + "**\n");
            }
            String title = "";
            String fileName = "";
            try{
                title = key.split(":")[0];
                fileName = key.split(":")[1];

            }catch (Exception e){
                System.out.println("Broken: " + key);
                e.printStackTrace();
                continue;
            }


            mdPageBuilder.append("### [")
                    .append(title)
                    .append("](")
                    .append(fileName)
                    .append(") ###\n");

            mdPageBuilder.append(pageContent)
                    .append("<br>\n<br>\n\n");

        }


        if (mdPageBuilder.length() > 0) {
            BufferedWriter writer = new BufferedWriter(new FileWriter("musthave-qi-list.md"));
            writer.write(mdPageBuilder.toString());
            writer.close();
        }
    }

    private static StringBuilder buildContent(BufferedReader reader, String injectableIntroBody) throws IOException {
        StringBuilder content = new StringBuilder();
        String line;
        boolean skipLines = false;
        boolean injected = false;

        while ((line = reader.readLine()) != null) {
            // Skip lines between the begin and end tags
            if (line.trim().equals(beginTag)) {
                skipLines = true;
                continue;
            }
            if (line.trim().equals(endTag)) {
                skipLines = false;
                continue;
            }
            if (skipLines) {
                continue;
            }

            // Check for the first line starting with <div and inject if not done yet
            if (!injected) {
                content.append(ASSIGN_ID + "\n").append(injectableIntroBody);
                content.append(line.replace(ASSIGN_ID, "")).append("\n");
                injected = true;
            } else {
                content.append(line.replace(ASSIGN_ID, "")).append("\n");
            }
        }

        // If the <div line was never found, inject at the top
        if (!injected) {
            content.insert(0, injectableIntroBody);
        }


        return content;
    }

    private static String buildIntroFileNameFromJsonName(String name) {
        return name.replace(".json", "-intro.xml");
    }


//    private static String getIdFromJson(File file) throws Exception {
//        JsonObject jsonData = parseJsonFromFile(file);
//        return jsonData.get(ID).getAsString();
//    }


    private static JsonObject parseJsonFromFile(File file) throws Exception {
        try (Reader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }


    /**
     * Using a similar approach/modification to the QI-Core script that created this content in that IG, create a script to generate these sections in DEQM.
     * <p>
     * "Each MeasureReport must have” section
     * <p>
     * The element name and short description from the structured definition will display for each element with a cardinality of 1..x
     * <p>
     * “Each MeasureReport Must support” section
     * <p>
     * The element name and short description from the structured definition will display for each element that has a Must Support flag (mustSupport=true in structured definition)
     * <p>
     * No changes at this time to the additional profile specific implementation guidance.
     */
    public static String buildStructureDefinitionIntro(String jsonString) {


        JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();

        String type = root.get("type").getAsString();

        List<String> mustHaveElements = new ArrayList<>();
        List<String> mustSupportElements = new ArrayList<>();

        JsonArray elements = root.getAsJsonObject(SNAPSHOT).getAsJsonArray(ELEMENT);

        List<String> parentExtensions = getParentIdentifiers(elements);


        for (JsonElement element : elements) {
            String parentExtensionEntry = "";

            JsonObject elementObj = element.getAsJsonObject();
            String elementIdentifier = elementObj.get("path").getAsString();
            String elementId = elementObj.get("id").getAsString();
            boolean childExtensionEntry = false;
            //check if this is a child of a parent extension (will have .extension in path, but won't END in .extension
            for (String entry : parentExtensions){
                if (elementId.contains(entry)){
                    parentExtensionEntry = entry;
                    break;
                }
            }
            //we only analyze and add the parent extension entry, all children to be ignored.
            if (!parentExtensionEntry.isEmpty()){
                System.out.println("Skipping entry with path: " + elementIdentifier + " and  id " + elementId + ", matched as child to on " + parentExtensionEntry);
                continue;
            }

            //if path ends in ".extension" use sliceName (skip it if it doesn't have a slicename and ends in .extension)
            if (elementIdentifier.endsWith(".extension") && !elementObj.has("sliceName")) {
                continue;
            }else if (elementIdentifier.endsWith(".extension") && elementObj.has("sliceName")) {
                elementIdentifier = elementObj.get("sliceName").getAsString();
            } else {
                //strip resource type
                int dotIndex = elementIdentifier.indexOf('.');
                if (dotIndex != -1) {
                    elementIdentifier = elementIdentifier.substring(dotIndex + 1);
                }
            }

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
                mustHaveElements.add(elementIdentifier + ": " + shortDesc);
            } else {

                //“Each MeasureReport Must support” section
                //The element name and short description from the structured definition will display for each element that has a Must Support flag (mustSupport=true in structured definition)
                if (elementObj.has(MUST_SUPPORT) && elementObj.get(MUST_SUPPORT).getAsBoolean()) {
                    mustSupportElements.add(elementIdentifier + ": " + shortDesc);
                }
            }
        }


        //TODO: Eventually we will want this to retain html as we did in fhir-qi-core, for now it is .md files:

        String thisMustHaveTag = mustHaveTag.replace("[type]", "{{site.data.structuredefinitions.[id].type}}");
        String thisMustSupportTag = mustSupportTag.replace("[type]", "{{site.data.structuredefinitions.[id].type}}");


        StringBuilder output = new StringBuilder();
        if (!mustHaveElements.isEmpty()) {
            output.append("<b>").append(thisMustHaveTag).append("</b>\n")
                    .append("<ul>\n");
            int counter = 1;
            for (String element : mustHaveElements) {
                output.append("<li>")
                        .append(counter)
                        .append(". ")
                        .append(element)
                        .append("</li>\n");
                counter++;
            }
            output.append("</ul>")
                    .append("\n\n");
        }

        if (!mustSupportElements.isEmpty()) {
            output.append("<b>").append(thisMustSupportTag).append("</b>\n")
                    .append("<ul>\n");
            int counter = 1;
            for (String element : mustSupportElements) {
                output.append("<li>")
                        .append(counter)
                        .append(". ")
                        .append(element)
                        .append("</li>\n");
                counter++;
            }
            output.append("</ul>")
                    .append("\n\n");
        }


        if (output.length() > 0) {
            //TODO: remove call to processToMDOutput here, put it in outputMDMapToFile at "String pageContent = mdMap.get(key)":
            return processToMDOutput(beginTag + "\n" + mainTitle + output + endTag, thisMustHaveTag, thisMustSupportTag);
        } else {
            return "";
        }
    }

    private static List<String> getParentIdentifiers(JsonArray elements) {
        List<String> parentExtensions = new ArrayList<>();

        for (JsonElement element : elements) {
            JsonObject elementObj = element.getAsJsonObject();

            //parent extension entries follow an id path ending in .extension:sliceName. Record these first, avoid children.
            //store the string ".extension:" + sliceName + "." to check id in later check to confirm child extension entry.
            // Children id will contain string.
            for (String parentType : PARENT_TYPES){
                if (elementObj.has("sliceName") &&
                        (elementObj.get("id").getAsString().endsWith("." + parentType + ":" + elementObj.get("sliceName").getAsString()))
                ) {
                    parentExtensions.add("." + parentType + ":" + elementObj.get("sliceName").getAsString() + ".");
                }
            }


        }
        return parentExtensions;
    }

    private static String processToMDOutput(String input, String mustHaveTag, String mustSupportTag) {
        return input.replace("<ul>\n", "")
                .replace("</ul>\n", "")
                .replace("<li>", "")
//                .replace("<li>", "* ")
                .replace("</li>", "")
                .replace("<b>" + mustHaveTag + "</b>\n", "**" + mustHaveTag + "**\n")
                .replace("<b>" + mustSupportTag + "</b>\n", "**" + mustSupportTag + "**\n")
//                .replace(beginTag + "\n", "")
//                .replace(endTag, "")
                .replace("|", "\\|")
                .replace("</br>", "");
    }

}