package com.icf.ecqm.structuredefinition.introgenerator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.util.*;

public class Main {

    private static final String SNAPSHOT = "snapshot";
    private static final String ELEMENT = "element";
    private static final String STRUCTURE_DEFINITION = "StructureDefinition";
    private static final String ID = "id";
    private static final String SHORT = "short";
    private static final String MUST_SUPPORT = "mustSupport";
    private static final String introNotesFolder = "input" + File.separator + "intro-notes";
    private static final String outputFolder = "output";
    private static final String beginTag = "<!--Begin Generated Intro Tag (DO NOT REMOVE)-->";
    private static final String endTag = "<!--End Generated Intro (DO NOT REMOVE)-->";
    private static final String mustHaveTag = "Must Have:";
    private static final String qiTag = "QI Elements:";
    private static final String MIN = "min";
    private static final String MAX = "max";
    private static final String PRIMARY_CODE_PATH = "Primary code path:";
    private static final String CODE_PATH_URL = "http://hl7.org/fhir/StructureDefinition/cqf-modelInfo-primaryCodePath";
    private static final String KEY_ELEMENT_PATH_URL = "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-keyelement";
    private static final String URL = "url";
    private static final String EXTENSION = "extension";
    private static final String VALUE_STRING = "valueString";
    private static final String PC_PATH_HREF = "<a href='https://cql.hl7.org/02-authorsguide.html#filtering-with-terminology'>CQL Retrieve</a>";
    private static final String PC_PATH_MD_LINK = "[CQL Retrieve](https://cql.hl7.org/02-authorsguide.html#filtering-with-terminology)";

    private static final String NOTE_TO_BALLOTERS_HTML = "<br></br>\n<b>NOTE TO BALLOT REVIEWERS:</b>\n" +
            "<ul>\n" +
            "<li>US Core 7.0, and thus QI-Core 7.0, has a new approach to USCDI requirements.</li>\n" +
            "<ul>\n" +
            "<li>As noted in the US Core 7.0 <a href='https://hl7.org/fhir/us/core/must-support.html#must-support-elements'>Must Support</a> section, US Core 7.0 no longer highlights mandatory (cardinality 1..* or 1..1) and Must Support elements with a (USCDI) indicator as such items must be supported for interoperability.</li>\n" +
            "<li>Those USCDI elements that are not mandatory or Must Support now include an indicator (ADDITIONAL USCDI) in US Core. QI-Core 7.0 does not reference USCDI elements; rather, users should access US Core 7.0 to understand its implementation of USCDI version 4.</li>\n" +
            "</ul>\n" +
            "<li>We invite comments about the approach and suggestions for other options that would also avoid unnecessary noise or reading load to the QI-Core profile representation.</li>\n" +
            "<li>Further, QI-Core 7.0 does not discuss <a href='https://uscdiplus.healthit.gov/uscdi'>USCDI+Quality</a> because at the time of ballot preparation, no published version of USCDI+Quality is available. We seek reviewer advice regarding how QI-Core might address future USCDI+Quality.</li>\n" +
            "</ul>\n<br></br>\n";

    private static boolean MS_ARG = false;

    /**
     * This tool will generate intro files in html within the fhir-qi-core\input\intro-notes xml files corresponding to each StructureDefinition file.
     * Matches are based on filename. For example:
     * fhir-qi-core\output\StructureDefinition-qicore-allergyintolerance.json
     * matches to
     * fhir-qi-core\input\intro-notes\StructureDefinition-qicore-allergyintolerance-intro.xml
     * Looping through output folder guarantees we are not wasting time editing files that aren't included in the project, so generating
     * the IG before running the script will be necessary.
     *
     * @param args -ms indicates QI utilizes must support flag
     */
    public static void main(String[] args) {

        for (String arg : args) {
            if (arg.contains("ms")) {
                MS_ARG = true;
                break;
            }
        }

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
                name.toLowerCase().endsWith(".json"));

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

                    String id = getIdFromJson(outputFile);

                    String introNoteFileName = "StructureDefinition-" + id + "-intro.xml";

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


            File inputDir = new File(introNotesFolder);

            File[] inputFiles = inputDir.listFiles((dir, name) -> name.equalsIgnoreCase(buildIntroFileNameFromJsonName(name)));

            System.out.println("Found matching files in " + introNotesFolder + ": " + Arrays.toString(inputFiles));
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

        System.out.println("File modification is done. Generating the IG should show updated element list in files above. MS arg: " + MS_ARG);

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

            if (!mdMap.get(key).contains(mustHaveTag)
                    && !mdMap.get(key).contains(qiTag)) {
                continue;
            }

            String pageContent = mdMap.get(key)
                    .replace(NOTE_TO_BALLOTERS_HTML, "")
                    .replace("<ul>\n", "")
                    .replace("</ul>\n", "")
                    .replace("<li>", "* ")
                    .replace("</li>", "")
                    .replace("<b>" + mustHaveTag + "</b>\n", "**" + mustHaveTag + "**\n")
                    .replace("<b>" + qiTag + "</b>\n", "**" + qiTag + "**\n")
                    .replace(PC_PATH_HREF + "\n<br></br>\n<br></br>", PC_PATH_HREF + "\n<br></br>")
                    .replace("<b>" + PRIMARY_CODE_PATH + "</b>", "**" + PRIMARY_CODE_PATH + "**")
                    .replace(PC_PATH_HREF, PC_PATH_MD_LINK)
                    .replace(beginTag + "\n", "")
                    .replace(endTag, "")
                    .replace("|", "\\|")
                    .replace("</br>", "");

            if (pageContent.contains(mustHaveTag)) {
                pageContent = pageContent.replace("**" + qiTag + "**\n", "\n**" + qiTag + "**\n");
            }

            String title = key.split(":")[0];
            String fileName = key.split(":")[1];

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
            if (!injected && line.trim().startsWith("<div")) {
                content.append(line).append("\n");
                content.append(injectableIntroBody).append("\n");
                injected = true;
            } else {
                content.append(line).append("\n");
            }
        }

        // If the <div line was never found, inject at the top
        if (!injected) {
            content.insert(0, injectableIntroBody + "\n");
        }


        return content;
    }

    private static String buildIntroFileNameFromJsonName(String name) {
        return name.replace(".json", "-intro.xml");
    }


    private static String getIdFromJson(File file) throws Exception {
        JsonObject jsonData = parseJsonFromFile(file);
        return jsonData.get(ID).getAsString();
    }


    private static JsonObject parseJsonFromFile(File file) throws Exception {
        try (Reader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    /**
     * https://jira.hl7.org/browse/FHIR-46030
     * <p>
     * Must Have: List all elements with a cardinality of 1..x
     * [element name]: [short description from structured definition] Description in this section should exclude "QI" indicator
     * <p>
     * QI Elements: List all elements with key element (QI) extension that do not have a cardinality of 1..x
     * [element name]: [short description from structured definition] Description in this section should exclude "QI" indicator
     * Also must contain extension for qicore-keyelement:
     * "extension": [
     * {
     * "url": "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-keyelement",
     * "valueBoolean": true
     * }
     * ],
     * <p>
     * Primary code path: [element with primarycodepath extension]
     * (PCPath) This element is the primary code path for this resource [CQL Retrieve](https://cql.hl7.org/02-authorsguide.html#filtering-with-terminology)
     */
    public static String buildStructureDefinitionIntro(String jsonString) {
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

            //if path ends in ".extension" use sliceName
            if (elementName.endsWith(".extension") && elementObj.has("sliceName")) {
                elementName = elementObj.get("sliceName").getAsString();
            } else {
                //strip resource type
                int dotIndex = elementName.indexOf('.');
                if (dotIndex != -1) {
                    elementName = elementName.substring(dotIndex + 1);
                }
            }

            String shortDesc = elementObj.has(SHORT) ? elementObj.get(SHORT).getAsString()
                    .replace("(QI-Core)", "")
                    .replace("(USCDI)", "")
                    .replace("  ", " ")
                    :
                    "";

            boolean isMustHave = false;
            if (elementObj.has(MIN) && elementObj.has(MAX)) {
                int min = elementObj.get(MIN).getAsInt();
                String max = elementObj.get(MAX).getAsString();
                if (min == 1 && (max.equals("1") || max.equals("*"))) {
                    if (elementObj.has(MUST_SUPPORT) && elementObj.get(MUST_SUPPORT).getAsBoolean()) {
                        isMustHave = true;
                    }
                }
            }

            if (isMustHave) {
                mustHaveElements.add(elementName + ": " + shortDesc);
            } else {

                //QI rule: look for key element path url for one version, look for min = 0 and mustSupport = false in other version
                //delegated by arg -ms at runtime:

                if (MS_ARG) {
                    if (elementObj.has(MIN) && elementObj.get(MIN).getAsString().equals("0")) {
                        if (elementObj.has(MUST_SUPPORT) && !elementObj.get(MUST_SUPPORT).getAsBoolean()) {
                            qiElements.add(elementName + ": " + shortDesc);
                        }
                    }
                } else {

                    if (elementObj.has(EXTENSION)) {
                        JsonArray extensions = elementObj.getAsJsonArray(EXTENSION);
                        for (JsonElement extElement : extensions) {

                            JsonObject extObj = extElement.getAsJsonObject();
                            if (extObj.has(URL) && extObj.get(URL).getAsString().equals(KEY_ELEMENT_PATH_URL)) {
                                qiElements.add(elementName + ": " + shortDesc);
                            }
                        }
                    }
                }
            }
        }

        StringBuilder output = new StringBuilder();
        if (!mustHaveElements.isEmpty()) {
            output.append("<b>" + mustHaveTag + "</b>\n")
                    .append("<ul>\n");
            for (String element : mustHaveElements) {
                output.append("<li>").append(element).append("</li>\n");
            }
            output.append("</ul>")
                    .append("\n\n");
        }

        if (!qiElements.isEmpty()) {
            output.append("<b>" + qiTag + "</b>\n")
                    .append("<ul>\n");
            for (String element : qiElements) {
                output.append("<li>").append(element).append("</li>\n");
            }
            output.append("</ul>")
                    .append("\n\n");
        }


        if (!primaryCodePath.isEmpty()) {
            output.append("<b>" + PRIMARY_CODE_PATH + "</b> ")
                    .append(primaryCodePath)
                    .append("\n")
                    .append("<br></br>\n(PCPath) This element is the primary code path for this resource " + PC_PATH_HREF + "\n<br></br>\n<br></br>")
                    .append("\n\n");
        }

        output.append(NOTE_TO_BALLOTERS_HTML)
                .append("\n\n");


        if (output.length() > 0) {
            return beginTag + "\n" + output + endTag;
        } else {
            return "";
        }
    }

}