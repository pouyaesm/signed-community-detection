import Network.Core.GraphIO;
import Network.Core.Graph;
import Network.Core.Statistics;
import Network.Optimization.CPM;
import Network.Optimization.CPMParameters;
import Network.Optimization.CPMapParameters;
import Network.SignedMapEquation.SiMap;
import cern.colt.map.OpenIntIntHashMap;
import org.apache.commons.cli.*;

import java.util.Arrays;

public class Main {

    public static final String VERSION_ID = "1.0.0";

    private static final String DETECT = "detect";
    private static final String EVALUATE = "evaluate";
    private static final String ANALYSE = "analyse";

    private static final String GRAPH_INPUT = "f";
    private static final String PARTITION_INPUT = "p";
    private static final String OUTPUT = "o";
    private static final String RESOLUTION = "r";
    private static final String RESOLUTION_DEFAULT = "0.05";
    private static final String ALPHA = "a";
    private static final String ALPHA_DEFAULT = "0.5";
    private static final String ITERATION = "i";
    private static final String ITERATION_DEFAULT = "2";
    private static final String THREAD_COUNT = "t";
    private static final String THREAD_COUNT_DEFAULT = "2";
    private static final String HELP = "h";
    private static final String VERSION = "v";

    private static final String ERR_INPUT = "input address is not specified";
    public static void main(String[] args) {
//        for (String arg : args){
////            System.out.println(arg);
////        }
        OpenIntIntHashMap hashMap = new OpenIntIntHashMap();
        hashMap.put(1, 2);
        if(args.length == 0){
            System.out.println(getIntroduction());
            return;
        }else if (args.length == 1) {
            System.out.println(getIntroduction());
        }else{
            // remove the operation argument to fed to each operation parser
            String[] operationArgs = Arrays.copyOfRange(args, 1, args.length);
            String operation = args[0];
            switch (operation){
                case DETECT:
                    parseDetectionOptions(operationArgs);
                    return;
                case EVALUATE:
                case ANALYSE:
                default:
                    parseMainOptions(operationArgs);
            }
        }
    }

    public static void parseEvaluationOptions(String args[]){
        // create the parser
        CommandLineParser parser = new DefaultParser();
        try {
            // isNumber the command line arguments
            CommandLine line = parser.parse( getDetectionOptions(), args );
            if(line.hasOption(HELP)){
                showDetectionHelp();
                return;
            }
            String graphInput = line.getOptionValue(GRAPH_INPUT, "");
            if(graphInput.length() == 0){
                throw new ParseException(ERR_INPUT);
            }
            String output = line.getOptionValue(OUTPUT, "com_" + graphInput);
            float resolution = Float.parseFloat(line.getOptionValue(RESOLUTION, RESOLUTION_DEFAULT));
            float alpha = Float.parseFloat(line.getOptionValue(ALPHA, ALPHA_DEFAULT));
            int iteration = Integer.parseInt(line.getOptionValue(ITERATION, ITERATION_DEFAULT));
            CPM detector = new CPM();
            Graph graph = GraphIO.readGraph(graphInput, true);
            int[] partition = detector.detect(graph, 0.05f, 0.5f, 0);
            GraphIO.writePartition(graph, partition, output);
        } catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            showDetectionHelp();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void parseDetectionOptions(String args[]){
        // create the parser
        CommandLineParser parser = new DefaultParser();
        try {
            // isNumber the command line arguments
            CommandLine line = parser.parse( getDetectionOptions(), args );
            if(line.hasOption(HELP)){
                showDetectionHelp();
                return;
            }
            String input = line.getOptionValue(GRAPH_INPUT, "");
            if(input.length() == 0){
                throw new ParseException(ERR_INPUT);
            }
            String output = line.getOptionValue(OUTPUT, "com_" + input);
            float resolution = Float.parseFloat(line.getOptionValue(RESOLUTION, RESOLUTION_DEFAULT));
            float alpha = Float.parseFloat(line.getOptionValue(ALPHA, ALPHA_DEFAULT));
            int iteration = Integer.parseInt(line.getOptionValue(ITERATION, ITERATION_DEFAULT));
            int threadCount = Integer.parseInt(line.getOptionValue(THREAD_COUNT, THREAD_COUNT_DEFAULT));

            System.out.println("Resolution scale: " + resolution);

            CPM cpmDetector = (CPM) new CPM().setThreadCount(threadCount);
            double time = System.currentTimeMillis();
            Graph graph = GraphIO.readGraph(input, true);
            int[] partition = cpmDetector.detect(graph, resolution, alpha, iteration);
            GraphIO.writePartition(graph, partition, output);
            double duration = (System.currentTimeMillis() - time) / 1000;
            System.out.println("Finished in " + duration + " seconds");
            System.out.println("Number of communities: " + Statistics.array(partition).uniqueCount);

            CPMapParameters cpMapParameters = new CPMapParameters();
            cpMapParameters.USE_RECORDED = false;
            cpMapParameters.TELEPORT_TO_NODE = false;
            cpMapParameters.TAU = 0.01f;
            System.out.println("Minimum Description Length: " + SiMap.evaluate(graph, partition, cpMapParameters));

            CPMParameters cpmParameters = new CPMParameters();
            cpmParameters.resolution = resolution;
            cpMapParameters.alpha = alpha;
            System.out.println("Hamiltonian: " + cpmDetector.evaluate(graph, partition, cpMapParameters));

        } catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            showDetectionHelp();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showDetectionHelp(){
        String header = "Options used for community detection:\n\n";
        String footer = "";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(DETECT, header, getDetectionOptions(), footer, true);
    }

    /**
     * Return the list of available argument objects to be passed to the program
     * @return
     */
    public static Options getDetectionOptions(){
        // create Options object
        Option input = Option.builder(GRAPH_INPUT).longOpt("input").desc("Input edge list file")
                .hasArg().argName("address").type(String.class).build();
        Option output = Option.builder(OUTPUT).longOpt("output")
                .desc("Output file")
                .hasArg().hasArg().argName("address").type(String.class).build();
        Option resolution = Option.builder(RESOLUTION)
                .longOpt("resolution").desc("Resolution for fast community detection at a specific scale;" +
                        " larger values result in smaller and denser communities. Default value is "
                        + RESOLUTION_DEFAULT)
                .hasArg().argName("resolution").type(Float.class).build();
        Option alpha = Option.builder(ALPHA)
                .longOpt("alpha")
                .desc("Relative importance of positive links compared to negative links. Default is "
                        + ALPHA_DEFAULT)
                .hasArg().argName("weight").type(Float.class).build();
        Option iteration = Option.builder(ITERATION)
                .longOpt("iteration")
                .desc("Number of times partition is further refined. Default value is "
                        + ITERATION_DEFAULT)
                .hasArg().argName("groupCount").type(Integer.class).build();
        Option threadCount = Option.builder(THREAD_COUNT)
                .longOpt("thread")
                .desc("Number of threads used for parallel computations. Default value is "
                        + THREAD_COUNT_DEFAULT)
                .hasArg().argName("thread").type(Integer.class).build();
        Option help = Option.builder(HELP)
                .longOpt("help")
                .desc("List of options for community detection").build();
        Options options = new Options();
        options.addOption(input).addOption(output).addOption(resolution)
                .addOption(alpha).addOption(iteration).addOption(threadCount).addOption(help);
        return options;
    }

    public static void parseMainOptions(String[] args){
        // create the parser
        CommandLineParser parser = new DefaultParser();
        try {
            // isNumber the command line arguments
            CommandLine line = parser.parse( getMainOptions(), args );
            if(line.hasOption(VERSION) || line.hasOption(HELP)){
                System.out.println(getIntroduction());
            }
        }
        catch( ParseException exp ) {
            return;
        }
    }
    /**
     * Return the list of available main options
     * @return
     */
    public static Options getMainOptions(){
        // create Options object
        Options options = new Options();
        Option input = Option.builder(VERSION)
                .longOpt("version").desc("Program version").build();
        Option output = Option.builder(HELP)
                .longOpt("help").desc("List of available commands").build();
        options.addOption(input).addOption(output).addOption(output);
        return options;
    }

    /**
     * Program introduction
     * @return
     */
    public static String getIntroduction(){
        return "\nCommunity Detection in Signed, Directed, and Weighted Networks @ 2018 version " + VERSION_ID +
                "\nAvailable commands are:\n  " +
                DETECT + " -h\n  " +
                EVALUATE + " -h\n  " +
                ANALYSE + " -h";
    }
}
