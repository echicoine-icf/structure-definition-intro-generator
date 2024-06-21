package com.icf.ecqm.structuredefinition.introgenerator;

import com.google.gson.*;

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

    /**
     * This tool will generate intro files in html within the fhir-qi-core\input\intro-notes xml files corresponding to each StructureDefinition file.
     * Matches are based on filename. For example:
     * fhir-qi-core\output\StructureDefinition-qicore-allergyintolerance.json
     * matches to
     * fhir-qi-core\input\intro-notes\StructureDefinition-qicore-allergyintolerance-intro.xml
     * Looping through output folder guarantees we are not wasting time editing files that aren't included in the project, so generating
     * the IG before running the script will be necessary.
     *
     * @param args
     */
    public static void main(String[] args) {
        runMain();
        //runTest();
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

        //System.out.println("File read successfully. Content length: " + contentBuilder.length());
        System.out.println(buildStructureDefinitionIntros(contentBuilder.toString()));
    }

    private static void runMain() {

        //cycle through all json structure defintion files in output folder:
        File outputDir = new File(outputFolder);
        File[] outputFiles = outputDir.listFiles((dir, name) -> name.toLowerCase().startsWith(STRUCTURE_DEFINITION.toLowerCase()) &&
                name.toLowerCase().endsWith(".json"));

        if (outputFiles != null) {
            Map<String, String> structureDefinitionIntroMap = new HashMap<>();

            for (File outputFile : outputFiles) {
                //System.out.println("\r\nProcessing " + outputFile.getAbsolutePath());

                try {
                    JsonObject outputJson = parseJsonFromFile(outputFile);

                    String introNoteFileName = "StructureDefinition-" + getIdFromJson(outputFile) + "-intro.xml";

                    String introPortion = buildStructureDefinitionIntros(outputJson.toString());

                    System.out.println(outputFile.getAbsolutePath() + ": \n" + introPortion + "\n\n");

                    if (!introPortion.isEmpty()) {
                        //System.out.println("Intro generated: " + introNoteFileName + ": \n" + introPortion);

                        structureDefinitionIntroMap.put(introNoteFileName, introPortion);
                    } else {
                        //System.out.println("No intro generated (no elements pass criteria): " + introNoteFileName + ": \n" + introPortion);
                    }


                } catch (Exception e) {
                    System.err.println("Error processing file: " + outputFile.getName());
                    e.printStackTrace();
                }
            }

            //System.out.println("\r\n");
            //System.out.println("Files that met criteria: " + String.join(",", structureDefinitionIntroMap.keySet()));
            //System.out.println("\r\n");

            File inputDir = new File(introNotesFolder);

            File[] inputFiles = inputDir.listFiles((dir, name) -> name.equalsIgnoreCase(buildIntroFileNameFromJsonName(name)));

            //System.out.println("Found matching files in " + introNotesFolder + ": " + Arrays.toString(inputFiles));
            //System.out.println("\r\n");


            //write generated intro to corresponding file:
            //write generated intro to corresponding file:
            if (inputFiles != null) {
                for (File inputFile : inputFiles) {
                    //write to intro file below last </div>
                    //System.out.println("inputFile: " + inputFile.getName());
                    if (structureDefinitionIntroMap.containsKey(inputFile.getName())) {

                        String injectableIntroBody = structureDefinitionIntroMap.get(inputFile.getName());
                        if (injectableIntroBody.isEmpty()) continue;
                        try {
                            // Read the content of the file
                            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                            StringBuilder content = new StringBuilder();
                            String line;
                            boolean firstDivFound = false;
                            while ((line = reader.readLine()) != null) {
                                if (!line.trim().isEmpty()) {
                                    //put our stuff up top:
                                    if (line.trim().startsWith("<div") && !firstDivFound) {
                                        firstDivFound = true;
                                        content.append(line).append("\n");
                                        content.append(injectableIntroBody).append("\n");
                                    } else {
                                        content.append(line).append("\n");
                                    }
                                }
                            }
                            //no initial div found, build our own:
                            if (!firstDivFound) {
                                content.append("<div>").append(injectableIntroBody).append("</div>\n");
                            }
                            reader.close();

                            BufferedWriter writer = new BufferedWriter(new FileWriter(inputFile));
                            writer.write(content.toString());
                            writer.close();

                            //System.out.println("Injectable intro body added to: " + inputFile.getName());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                System.err.println("Error: Unable to list files in the input folder.");
            }
            //System.out.println("\r\n");

        } else {
            System.err.println("Error: Unable to list files in the output folder.");
        }

        //System.out.println("File modification is done. Generating the IG should show updated element list in files above.");
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

    private static void writeJsonToFile(File file, JsonObject json) throws Exception {
        try (Writer writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(json, writer);
            //System.out.println("Changes written to " + file.getAbsolutePath() + "\r\n");
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
     * <p>
     */
    public static String buildStructureDefinitionIntros(String jsonString) {
        JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();

        List<String> mustHaveElements = new ArrayList<>();
        List<String> qiElements = new ArrayList<>();

        JsonArray elements = root.getAsJsonObject(SNAPSHOT).getAsJsonArray(ELEMENT);

        for (JsonElement element : elements) {
            JsonObject elementObj = element.getAsJsonObject();
            String elementName = elementObj.get("path").getAsString();
            String shortDesc = elementObj.has(SHORT) ? elementObj.get(SHORT).getAsString().replace("(QI-Core)","").replace("(USCDI)","") : "";

            boolean isMustHave = false;
            if (elementObj.has("min") && elementObj.has("max")) {
                int min = elementObj.get("min").getAsInt();
                String max = elementObj.get("max").getAsString();
                if (min == 1 && (max.equals("1") || max.equals("*"))) {
                    if (elementObj.has(MUST_SUPPORT) && elementObj.get(MUST_SUPPORT).getAsBoolean()) {
                        isMustHave = true;
                    }
                }
            }

            if (isMustHave) {
                mustHaveElements.add(elementName + ": " + shortDesc);
            } else {
                if (elementObj.has("min") && elementObj.get("min").getAsString().equals("0")) {
                    if (elementObj.has(MUST_SUPPORT) && !elementObj.get(MUST_SUPPORT).getAsBoolean()) {
                        qiElements.add(elementName + ": " + shortDesc );
                    }
                }
            }
        }

        StringBuilder output = new StringBuilder();
        if (!mustHaveElements.isEmpty()) {
            output.append("<h3>Must Have</h3>\n");
            output.append("<ul>");
            for (String element : mustHaveElements) {
                output.append("<li>").append(element).append("</li>\n");
            }
            output.append("</ul>");
            output.append("\n");
        }

        if (!qiElements.isEmpty()) {
            output.append("<h3>QI Elements</h3>\n");
            output.append("<ul>");
            for (String element : qiElements) {
                output.append("<li>").append(element).append("</li>\n");
            }
            output.append("</ul>");
            output.append("\n");
        }
        return output.toString();
    }

}