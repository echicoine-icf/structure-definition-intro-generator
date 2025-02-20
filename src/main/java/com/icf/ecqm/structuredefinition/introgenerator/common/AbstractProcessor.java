package com.icf.ecqm.structuredefinition.introgenerator.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.util.*;

public abstract class AbstractProcessor {
    protected static final String SNAPSHOT = "snapshot";
    protected static final String ELEMENT = "element";
    protected static final String STRUCTURE_DEFINITION = "StructureDefinition";
    protected static final String ID = "id";
    protected static final String SHORT = "short";
    protected static final String MUST_SUPPORT = "mustSupport";
    protected static final String MIN = "min";
    protected static final String MAX = "max";

    protected static final String beginTag = "<!--Begin Generated Intro Tag (DO NOT REMOVE)-->";
    protected static final String endTag = "<!--End Generated Intro (DO NOT REMOVE)-->";

    protected final String outputFolder;
    protected final String pageContentFolder;

    public AbstractProcessor(String outputFolder, String pageContentFolder) {
        this.outputFolder = outputFolder;
        this.pageContentFolder = pageContentFolder;
    }

    public void runProcessor() {
        File outputDir = new File(outputFolder);
        File[] outputFiles = outputDir.listFiles((dir, name) ->
                name.toLowerCase().startsWith(STRUCTURE_DEFINITION.toLowerCase()) && name.toLowerCase().endsWith(".json")
        );

        if (outputFiles == null || outputFiles.length == 0) {
            System.out.println("Output folder is empty!");
            return;
        }

        Map<String, String> structureDefinitionIntroMap = new HashMap<>();
        Map<String, String> mdMap = new HashMap<>();

        for (File outputFile : outputFiles) {
            System.out.println("\nProcessing " + outputFile.getAbsolutePath());

            try {
                JsonObject outputJson = parseJsonFromFile(outputFile);
                String id = outputJson.get(ID).getAsString();
                String structureDefinitionIntro = buildStructureDefinitionIntro(outputJson.toString());

                if (!structureDefinitionIntro.isEmpty()) {
                    String introNoteFileName = "StructureDefinition-" + id + "-intro.md";
                    structureDefinitionIntroMap.put(introNoteFileName, structureDefinitionIntro);
                    mdMap.put(id, structureDefinitionIntro);
                }
            } catch (Exception e) {
                System.err.println("Error processing file: " + outputFile.getName());
                e.printStackTrace();
            }
        }

        try {
            outputMDMapToFile(mdMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        processIntroFiles(structureDefinitionIntroMap);
    }

    private void processIntroFiles(Map<String, String> structureDefinitionIntroMap) {
        File inputDir = new File(pageContentFolder);
        File[] inputFiles = inputDir.listFiles((dir, name) -> name.endsWith(".md"));

        Set<String> introFilesNotFound = new HashSet<>(structureDefinitionIntroMap.keySet());

        if (inputFiles != null) {
            for (File file : inputFiles) {
                introFilesNotFound.remove(file.getName());
            }
            for (File inputFile : inputFiles) {
                writeToFile(structureDefinitionIntroMap, inputFile.getName(), false);
            }
        }

        if (!introFilesNotFound.isEmpty()) {
            System.out.println("Some intro files were missing: " + String.join(", ", introFilesNotFound));
        }
    }

    private static JsonObject parseJsonFromFile(File file) throws Exception {
        try (Reader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void writeToFile(Map<String, String> structureDefinitionIntroMap, String introFileName, boolean createFile) {
        try {
            String content = structureDefinitionIntroMap.get(introFileName);
            if (content == null || content.isEmpty()) return;

            File introFile = new File("input/pagecontent/" + introFileName);
            if (createFile && introFile.createNewFile()) {
                System.out.println("File created: " + introFile.getName());
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(introFile));
            writer.write(content);
            writer.close();

            System.out.println("Injected intro body into: " + introFile.getName());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error creating file: " + introFileName);
        }
    }

    private static void outputMDMapToFile(Map<String, String> mdMap) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("musthave-qi-list.md"));
        for (Map.Entry<String, String> entry : mdMap.entrySet()) {
            writer.write("### [" + entry.getKey() + "](StructureDefinition-" + entry.getKey() + ".html)\n");
            writer.write(entry.getValue());
            writer.write("\n\n");
        }
        writer.close();
    }

    public abstract String buildStructureDefinitionIntro(String jsonString);
}
