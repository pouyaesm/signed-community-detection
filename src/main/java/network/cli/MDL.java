package network.cli;

import network.Shared;
import network.core.Graph;
import network.core.GraphIO;
import network.core.SiGraph;
import network.core.Util;
import network.extendedmapequation.CPMap;
import network.optimization.CPM;
import network.optimization.CPMapParameters;
import org.apache.commons.cli.*;

import java.io.File;
import java.text.DecimalFormat;

/**
 * Detection and evaluation based on extended Map Equation to Signed networks
 * 2015 Community Detection in Signed Networks the Role of Negative ties in Different Scales
 */
public class MDL extends AbstractOperation{

    public static final String RESOLUTION_INTERVAL = "i";
    public static final String RESOLUTION_INTERVAL_START = "0.001";
    public static final String RESOLUTION_INTERVAL_END = "0.05";

    public static final String RESOLUTION_ACCURACY = "a";
    public static final String RESOLUTION_ACCURACY_DEFAULT = "0.002";

    public static final String TELEPORT = "tau";
    public static final String TELEPORT_DEFAULT = "0.15";

    public static final String THREAD_COUNT = "thread";
    public static final String THREAD_COUNT_DEFAULT = "4";

    private static final int PARTITION_NONE = 0;
    private static final int PARTITION_ONE = 1;
    private static final int PARTITION_MANY = 2;

    @Override
    public void parseOptions(String args[]){
        // create the parser
        CommandLineParser parser = new DefaultParser();
        try {
            double startTime = System.currentTimeMillis();

            CommandLine line = parser.parse( buildOptions(), args );
            if(line.hasOption(OperationCenter.HELP)){
                showHelp();
                return;
            }
            Shared.setVerbose(line.hasOption(OperationCenter.VERBOSE)); // verbose log or not

            Shared.log("Program started");

            String inputGraph = line.getOptionValue(OperationCenter.INPUT_GRAPH, "");
            if(inputGraph.length() == 0){
                throw new ParseException(OperationCenter.ERR_INPUT_GRAPH_NOT_SPECIFIED);
            }

            String output = line.getOptionValue(OperationCenter.OUTPUT, "");
            if(output.length() == 0){
                Shared.log(OperationCenter.ERR_OUTPUT_NOT_SPECIFIED);
            }

            String inputPartition = line.getOptionValue(OperationCenter.INPUT_PARTITION, "");
            File[] partitionFiles = null;
            int partitionMode;
            if(inputPartition.contains("*")){ // batch files
                String partitionDirectory = Util.getDirectory(inputPartition);
                String partitionFileName = Util.getFileName(inputPartition);
                partitionMode = PARTITION_MANY;
                partitionFiles = new File(partitionDirectory).listFiles(
                        (dir, name) -> name.matches(network.core.Util.wildcardToRegex(partitionFileName)));
            }else if(inputPartition.length() > 0){ // one file
                partitionMode = PARTITION_ONE;
                partitionFiles = new File[]{new File(inputPartition)};
            } else{ // no evaluation
                partitionMode = PARTITION_NONE;
            }

            int threadCount = Integer.parseInt(line.getOptionValue(THREAD_COUNT, THREAD_COUNT_DEFAULT));
            boolean isUndirected = line.hasOption(OperationCenter.UNDIRECTED);

            float teleport = Float.parseFloat(line.getOptionValue(TELEPORT, TELEPORT_DEFAULT));
            float specificResolution = Float.parseFloat(line.getOptionValue(
                    OperationCenter.RESOLUTION, "-1"));
            float resolutionStart = Float.parseFloat(RESOLUTION_INTERVAL_START);
            float resolutionEnd = Float.parseFloat(RESOLUTION_INTERVAL_END);
            if(line.hasOption(RESOLUTION_INTERVAL)){
                String[] resolutionInterval = line.getOptionValues(RESOLUTION_INTERVAL);
                resolutionStart = Float.parseFloat(resolutionInterval[0]);
                resolutionEnd = Float.parseFloat(resolutionInterval[1]);
            }
            float resolutionAccuracy = Float.parseFloat(
                    line.getOptionValue(RESOLUTION_ACCURACY, RESOLUTION_ACCURACY_DEFAULT));

            // Read the graph and construct the signed multi-graph
            Graph graph = GraphIO.readGraph(inputGraph, isUndirected);
            SiGraph siGraph = new SiGraph(graph);

            // Prepare the detector/evaluator and the given parameters
            CPMap cpmap = new CPMap();
            CPMapParameters parameters = new CPMapParameters(
                    teleport, false, false, threadCount, resolutionStart, resolutionEnd, resolutionAccuracy);

            // Respond to user requested mode either evaluation or detection accordingly
            int[] detectedPartition = null;
            if(partitionMode != PARTITION_NONE){ // partition evaluation (no detection)
                String[][] evaluations = new String[partitionFiles.length][2];
                DecimalFormat decimalFormat = new DecimalFormat("#.00000");
                for(int p = 0 ; p < evaluations.length ; p++){
                    int[] partition = GraphIO.readPartition(
                            partitionFiles[p].getAbsolutePath(), graph.getToNormal()[0]);
                    double mdl = cpmap.evaluate(graph, partition, parameters);
                    evaluations[p][0] = partitionFiles[p].getName();
                    evaluations[p][1] = decimalFormat.format(mdl);
                    Shared.log(evaluations[p][0] + "\t" + evaluations[p][1]);
                }
                GraphIO.writeEvaluation(evaluations, output);
            } else if (specificResolution >= 0) { // community detection at a specific resolution
                Shared.log("Resolution: " + specificResolution);
                CPM detector = (CPM) new CPM(specificResolution).setThreadCount(threadCount);
                detectedPartition = detector.detect(siGraph);
                GraphIO.writePartition(siGraph, detectedPartition, output);
                // Evaluate the detected partition too, if verbose is on
                if(Shared.isVerbose()){
                    Shared.log("Calculating MDL (this is skipped when verbose is off)");
                    Shared.log("MDL: " + cpmap.evaluate(graph, detectedPartition, parameters));
                }
            }else{ // community detection on a range of resolutions
                detectedPartition = cpmap.detect(graph, parameters);
                GraphIO.writePartition(siGraph, detectedPartition, output);
            }
            double duration = (System.currentTimeMillis() - startTime) / 1000;
            Shared.log("Finished in " + duration + " seconds");
        } catch( ParseException exp ) {
            // oops, something went wrong
            System.out.println( "Parsing failed.  Reason: " + exp.getMessage() );
            showHelp();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Return the list of available argument objects to be passed to the program
     * @return
     */
    @Override
    public Options buildOptions(){
        // create Options object
        Option resolution = Option.builder(OperationCenter.RESOLUTION)
                .longOpt("resolution").desc("Fast community detection at this specific scale;" +
                        " larger values result in smaller and denser communities")
                .hasArg().argName("resolution").type(Float.class).build();
        Option interval = Option.builder(RESOLUTION_INTERVAL)
                .longOpt("interval").desc("Resolution interval for community detection. Default interval is ["
                        + RESOLUTION_INTERVAL_START + ", " + RESOLUTION_INTERVAL_END + "]")
                .hasArgs().numberOfArgs(2).argName("start end").type(Float.class).build();
        Option accuracy = Option.builder(RESOLUTION_ACCURACY)
                .longOpt("accuracy").desc("Search algorithm stops when reaches this interval length." +
                        " Default value is " + RESOLUTION_ACCURACY_DEFAULT)
                .hasArg().argName("resolution").type(Float.class).build();
        Option teleport = Option.builder()
                .longOpt(TELEPORT).desc("Probability of teleportation in map equation" +
                        ", you should use the same value for comparing different partitions"
                        + ". Default value is " + TELEPORT_DEFAULT)
                .hasArg().argName("value").type(Float.class).build();
        Option threadCount = Option.builder()
                .longOpt(THREAD_COUNT)
                .desc("Number of threads used for parallel computations. Default value is "
                        + THREAD_COUNT_DEFAULT)
                .hasArg().argName("thread").type(Integer.class).build();
        Option help = Option.builder(OperationCenter.HELP)
                .longOpt("help")
                .desc("List of options for community detection and evaluation").build();
        Options options = OperationCenter.getSharedOptions();
        options.addOption(resolution).addOption(interval)
                .addOption(accuracy).addOption(threadCount)
                .addOption(help).addOption(teleport);
        return options;
    }

    @Override
    public void showHelp() {
        String header = "Options used for community detection and evaluation:\n\n";
        String footer = "";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(OperationCenter.MDL, header, buildOptions(), footer, true);
    }

    @Override
    public void showIntroduction() {
        showHelp();
    }
}
