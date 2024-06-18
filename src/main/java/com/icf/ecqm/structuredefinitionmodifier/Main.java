package com.icf.ecqm.structuredefinitionmodifier;

import java.io.*;
import java.util.*;

import com.google.gson.*;

public class Main {

    public static final String QICORE_KEYELEMENT = "qicore-keyelement";
    public static final String USCDI_REQUIREMENT = "uscdi-requirement";
    public static final String URL = "url";
    public static final String SNAPSHOT = "snapshot";
    public static final String ELEMENT = "element";
    public static final String EXTENSION = "extension";
    public static final String STRUCTURE_DEFINITION = "StructureDefinition";
    public static final String DIFFERENTIAL = "differential";
    public static final String ID = "id";
    public static final String SHORT = "short";
    public static final String MUST_SUPPORT = "mustSupport";

    /**
     * This branch processes StructureDefinition files in the output folder. Any element that meets this condition:
     *      mustSupport not present or false, extension list does not have qicore entry but does have uscdi entry
     * Then the following change will occur to matching StructureDefinition file input folder:
     *      json bloc will be added to elements array in input file, short will only have "(USCDI)" prepended
     * @param args
     */
    public static void main(String[] args) {

        String inputFolder = "input" + File.separator + "profiles";
        String outputFolder = "output";

        File outputDir = new File(outputFolder);
        File[] outputFiles = outputDir.listFiles((dir, name) -> name.toLowerCase().startsWith(STRUCTURE_DEFINITION.toLowerCase()) &&
                name.toLowerCase().endsWith(".json"));

        if (outputFiles != null) {
            Map<String, List<JsonObject>> shortDescriptionFields = new HashMap<>();

            for (File outputFile : outputFiles) {
                System.out.println("\r\nProcessing " + outputFile.getAbsolutePath());
                List<JsonObject> jsonBodiesToMigrate = getJsonBodyList(outputFile);


                if (!jsonBodiesToMigrate.isEmpty()) {
                    String identifier = "";
                    try {
                        identifier = getIdFromJson(outputFile).toLowerCase();
                    } catch (Exception e) {
                        identifier = outputFile.getName().toLowerCase();
                    }
                    shortDescriptionFields.put(identifier, jsonBodiesToMigrate);
                }
            }
            System.out.println("\r\n");
            System.out.println("Files that met criteria: " + String.join(",", shortDescriptionFields.keySet()));
            System.out.println("\r\n");
            File inputDir = new File(inputFolder);
            //now loop through files found to have descriptions changed:
            File[] inputFiles = inputDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));

            System.out.println("Found matching files in " + inputFolder + ": " + Arrays.toString(inputFiles));
            System.out.println("\r\n");
            if (inputFiles != null) {
                for (File inputFile : inputFiles) {

                    String identifier = "";
                    try {
                        identifier = getIdFromJson(inputFile).toLowerCase();
                    } catch (Exception e) {
                        identifier = inputFile.getName().toLowerCase();
                    }
                    List<JsonObject> shortDescriptionsToChangeInInputFile = shortDescriptionFields.get(identifier);

                    if (shortDescriptionsToChangeInInputFile != null && !shortDescriptionsToChangeInInputFile.isEmpty()) {
                        System.out.println("Processing json blocs in " + inputFile.getAbsolutePath());
                        addJsonBlocsToFile(inputFile, shortDescriptionsToChangeInInputFile);
                    }
                }
            } else {
                System.err.println("Error: Unable to list files in the input folder.");
            }

            System.out.println("\r\n");

        } else {
            System.err.println("Error: Unable to list files in the output folder.");
        }

        System.out.println("File modification is done. Generating the IG should show updated element list in files above.");
    }

    private static void addJsonBlocsToFile(File inputFile, List<JsonObject> jsonBlocs) {
        if (jsonBlocs == null || jsonBlocs.isEmpty()) {
            System.err.println("Error: List of elements for " + inputFile.getName() + " are blank.");
            return;
        }

        try {
            JsonObject inputJson = parseJsonFromFile(inputFile);
            JsonArray copiedElementsArray = new JsonArray();
            if (inputJson.has(DIFFERENTIAL) && inputJson.getAsJsonObject(DIFFERENTIAL).has(ELEMENT)) {

                List<String> jsonBlocIdentifiers = new ArrayList<>();

                JsonArray jsonArray = new JsonArray();
                for (JsonObject jsonObject : jsonBlocs) {
                    jsonBlocIdentifiers.add(jsonObject.get(ID).getAsString());
                    jsonArray.add(jsonObject);
                }

                //copy the elements to a new array but ignore any that have an id matching the json blocs coming in:
                JsonArray elementsArray = inputJson.getAsJsonObject(DIFFERENTIAL).getAsJsonArray(ELEMENT);

                for (JsonElement element : elementsArray){
                    if (!jsonBlocIdentifiers.contains(((JsonObject)element).get(ID).getAsString())){
                        copiedElementsArray.add(element);
                    }
                }

                //now add all the new
                copiedElementsArray.addAll(jsonArray);
            }
            inputJson.getAsJsonObject(DIFFERENTIAL).remove(ELEMENT);
            inputJson.getAsJsonObject(DIFFERENTIAL).add(ELEMENT, copiedElementsArray);

            writeJsonToFile(inputFile, inputJson);
            System.out.println (inputFile.getAbsolutePath() + " saw " + jsonBlocs.size() + " elements added.");

        } catch (Exception e) {
            System.err.println("Error processing file: " + inputFile.getName());
            e.printStackTrace();
        }

    }

    private static String getIdFromJson(File file) throws Exception {
        JsonObject jsonData = parseJsonFromFile(file);
        return jsonData.get(ID).getAsString();
    }

    private static List<JsonObject> getJsonBodyList(File outputFile) {

        List<JsonObject> jsonBodiesToMigrateOver = new ArrayList<>();
        try {
            // Parse JSON data from the file in output folder:
            JsonObject outputJson = parseJsonFromFile(outputFile);

            // Get the snapshot array
            if (outputJson.has(SNAPSHOT) && outputJson.getAsJsonObject(SNAPSHOT).has(ELEMENT)) {
                //get all the elements to loop through:
                JsonArray elementsArray = outputJson.getAsJsonObject(SNAPSHOT).getAsJsonArray(ELEMENT);

                for (JsonElement element : elementsArray) {

                    if (element instanceof JsonObject) {
                        JsonObject elementObj = (JsonObject) element;

                        String elementIdentifier = elementObj.get(ID).getAsString();

                        String mustSupport = "";
                            if (elementObj.has(MUST_SUPPORT)) {
                                mustSupport = elementObj.get(MUST_SUPPORT).getAsString();
                            }else{
                                mustSupport = "false";
                            }

                        if (elementObj.has(EXTENSION) && hasOnlyUscdiUrl(elementObj.getAsJsonArray(EXTENSION)) && mustSupport.equalsIgnoreCase("false")) {



                            //this needs to be at position 0:
                            JsonObject newEntry = new JsonObject();
                            newEntry.addProperty("url", "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-keyelement");
                            newEntry.addProperty("valueBoolean", true);

                            JsonArray newExtensionArray =  new JsonArray();
                            newExtensionArray.add(newEntry);
                            for (JsonElement el: elementObj.getAsJsonArray(EXTENSION)){
                                JsonObject extensionObj = (JsonObject) el;

                                if (extensionObj.has(URL) && extensionObj.getAsJsonPrimitive(URL).getAsString().endsWith(USCDI_REQUIREMENT)) {
                                    continue;
                                }else{
                                    newExtensionArray.add(el);
                                }
                            }
//                            newExtensionArray.addAll(elementObj.getAsJsonArray(EXTENSION));

                            // Find shortDescription
                            String shortDescription = elementObj.getAsJsonPrimitive(SHORT).getAsString();

                            // Modify shortDescription
                            String newShortDescription = "(QI-Core)" + shortDescription;

                            // Update shortDescription in the output JSON
                            System.out.println("Will update: " + elementIdentifier + " - " + newShortDescription);

                            elementObj.addProperty("short", newShortDescription);
                            elementObj.remove(EXTENSION);
                            elementObj.add(EXTENSION, newExtensionArray);
                            jsonBodiesToMigrateOver.add(elementObj);

                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing file: " + outputFile.getName());
            e.printStackTrace();
        }

        return jsonBodiesToMigrateOver;
    }

    private static boolean hasOnlyUscdiUrl(JsonArray extensionArray) {
        boolean qiCoreFound = false;
        boolean uscdiFound = false;

        for (JsonElement extensionElement : extensionArray) {
            if (extensionElement instanceof JsonObject) {
                JsonObject extensionObj = (JsonObject) extensionElement;
                if (extensionObj.has(URL) && extensionObj.getAsJsonPrimitive(URL).getAsString().endsWith(QICORE_KEYELEMENT)) {
                    qiCoreFound = true;
                }

                if (extensionObj.has(URL) && extensionObj.getAsJsonPrimitive(URL).getAsString().endsWith(USCDI_REQUIREMENT)) {
                    uscdiFound = true;
                }
            }
        }

        return !qiCoreFound && uscdiFound;
    }

    private static boolean hasBothURLTypes(JsonArray extensionArray) {
        boolean qiCoreFound = false;
        boolean uscdiFound = false;

        for (JsonElement extensionElement : extensionArray) {
            if (extensionElement instanceof JsonObject) {
                JsonObject extensionObj = (JsonObject) extensionElement;
                if (extensionObj.has(URL) && extensionObj.getAsJsonPrimitive(URL).getAsString().endsWith(QICORE_KEYELEMENT)) {
                    qiCoreFound = true;
                }

                if (extensionObj.has(URL) && extensionObj.getAsJsonPrimitive(URL).getAsString().endsWith(USCDI_REQUIREMENT)) {
                    uscdiFound = true;
                }
            }
        }

        return qiCoreFound && uscdiFound;
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
            System.out.println("Changes written to " + file.getAbsolutePath() + "\r\n");
        }
    }


}