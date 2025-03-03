To generate a runnable jar, run "mvn clean install" and navigate to structure-definition-intro-generator\target.

Copy StructureDefinitionIntroGeneratorTool-jar-with-dependencies.jar to the root directory of DEQM or QI-Core.

Run IG's _genonce script to generate all StructureDefinition files (which this tool loops through to build intros.)

In the root directory of IG, open command window and run:

  For QI-Core:
  
    java -jar StructureDefinitionIntroGeneratorTool-jar-with-dependencies.jar -qicore
    
  For DEQM:
  
    java -jart StructureDefinitionIntroGeneratorTool-jar-with-dependencies.jar -deqm
    
Once the process completes, rerun _genonce to view the new intro files absorbed into their html pages.
