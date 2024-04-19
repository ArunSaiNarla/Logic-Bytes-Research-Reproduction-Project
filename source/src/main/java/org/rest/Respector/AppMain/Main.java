package org.rest.Respector.AppMain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.rest.Respector.EndPointRecog.PreprocessFramework;

import soot.*;
import soot.Scene;
import soot.options.Options;

public class Main {
  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Error: Insufficient arguments provided. Usage: java Main <process-dir>... <output-file>");
      return;
    }
    
    List<String> argsList = Arrays.asList(args);
    List<String> processDir = argsList.subList(0, args.length - 1);
    
    String outputFile = argsList.get(args.length - 1);
    String sourceDirectory = System.getProperty("user.dir");

    G.reset();
    Options.v().set_prepend_classpath(true);
    Options.v().set_allow_phantom_refs(true);
    Options.v().set_keep_line_number(true);
    Options.v().set_exclude(List.of("jdk.*"));

    Options.v().set_process_dir(processDir);
    Options.v().set_soot_classpath(sourceDirectory);
    Options.v().set_omit_excepting_unit_edges(true);

    Options.v().setPhaseOption("jb", "use-original-names:true");
    Options.v().setPhaseOption("jb", "preserve-source-annotations:true");
    
    Options.v().set_write_local_annotations(true);
    Options.v().set_whole_program(true);
    
    // Call-graph options
    Options.v().setPhaseOption("cg", "library:any-subtype");
    Options.v().setPhaseOption("cg", "all-reachable");

    // Control CHA and SPARK call-graph construction via command-line or config
    boolean enableCHA = false; // This could be set based on external configuration
    boolean enableSPARK = true; // This could be set based on external configuration

    Options.v().setPhaseOption("cg.cha", "enabled:" + enableCHA);
    Options.v().setPhaseOption("cg.spark", "enabled:" + enableSPARK);

    Options.v().set_no_bodies_for_excluded(true);
    Scene.v().loadNecessaryClasses();

    PreprocessFramework endPointInfoWithData = PreprocessFramework.getEndPointInfo(Scene.v());
    
    Options.v().set_output_format(Options.output_format_jimple);

    Transform myAppTransform = new Transform("wjtp.myApp", new MainTransform(endPointInfoWithData, outputFile));
    PackManager.v().getPack("wjtp").add(myAppTransform);

    PackManager.v().runPacks();
  }
}