package Operations;

import Network.Core.Graph;
import Network.Core.GraphIO;
import Network.Core.Statistics;
import Network.Optimization.CPM;
import Network.Optimization.CPMParameters;
import Network.Optimization.CPMapParameters;
import Network.SignedMapEquation.SiMap;
import org.apache.commons.cli.*;

import static Operations.Main.HELP;

public class Detection extends AbstractOperation {

    public static final String ITERATION = "i";
    public static final String ITERATION_DEFAULT = "2";
    public static final String THREAD_COUNT = "t";
    public static final String THREAD_COUNT_DEFAULT = "2";

    @Override
    public void parseOptions(String args[]){
        // create the parser
        CommandLineParser parser = new DefaultParser();
        try {
            // isNumber the command line arguments
            CommandLine line = parser.parse( buildOptions(), args );
            if(line.hasOption(HELP)){
                showHelp();
                return;
            }
            String input = line.getOptionValue(Main.GRAPH_INPUT, "");
            if(input.length() == 0){
                throw new ParseException(Main.ERR_INPUT);
            }
            String output = line.getOptionValue(Main.OUTPUT, "com_" + input);
            float resolution = Float.parseFloat(line.getOptionValue(Main.RESOLUTION, Main.RESOLUTION_DEFAULT));
            float alpha = Float.parseFloat(line.getOptionValue(Main.ALPHA, Main.ALPHA_DEFAULT));
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
        Option input = Option.builder(Main.GRAPH_INPUT).longOpt("input").desc("Input edge list file")
                .hasArg().argName("address").type(String.class).build();
        Option output = Option.builder(Main.OUTPUT).longOpt("output")
                .desc("Output file")
                .hasArg().hasArg().argName("address").type(String.class).build();
        Option resolution = Option.builder(Main.RESOLUTION)
                .longOpt("resolution").desc("Resolution for fast community detection at a specific scale;" +
                        " larger values result in smaller and denser communities. Default value is "
                        + Main.RESOLUTION_DEFAULT)
                .hasArg().argName("resolution").type(Float.class).build();
        Option alpha = Option.builder(Main.ALPHA)
                .longOpt("alpha")
                .desc("Relative importance of positive links compared to negative links. Default is "
                        + Main.ALPHA_DEFAULT)
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

    @Override
    public void showHelp() {
        String header = "Options used for community detection:\n\n";
        String footer = "";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(Main.DETECT, header, buildOptions(), footer, true);
    }

    @Override
    public void showIntroduction() {

    }
}
