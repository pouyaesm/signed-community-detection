package network.cli;

import network.Shared;
import network.core.Graph;
import network.core.GraphIO;
import org.apache.commons.cli.*;

public class PreProcess extends AbstractOperation{

    public static final String FILTER = "filter"; // filter the weights
    public static final String CONNECTED_COMPONENTS = "c";
    public static final String LARGEST_CC = "largest"; // largest connected component
    public static final String OUTPUT_PARTITION = "p"; // output the partition of cc or lcc
    public static final String OUTPUT_GRAPH = "g"; // output the graph[s] of cc or lcc

    @Override
    public void parseOptions(String[] args) {
        // create the parser
        CommandLineParser parser = new DefaultParser();
        try {
            // isNumber the command line arguments
            CommandLine line = parser.parse( buildOptions(), args );
            if(line.hasOption(Center.HELP)){
                showHelp();
                return;
            }
            String input = line.getOptionValue(Center.INPUT, "");
            if(input.length() == 0){
                throw new ParseException(Center.ERR_INPUT);
            }
            String output = line.getOptionValue(Center.OUTPUT, "com_" + input);
            String[] filter = line.getOptionValues(FILTER);
            float lowerBound = filter[0] != null ? Float.parseFloat(filter[0]) : Float.NEGATIVE_INFINITY;
            float upperBound = filter[1] != null ? Float.parseFloat(filter[1]) : Float.POSITIVE_INFINITY;
            boolean outputCC = line.hasOption(CONNECTED_COMPONENTS);
            boolean largestCC = line.hasOption(LARGEST_CC);
            boolean outputPartition = line.hasOption(OUTPUT_PARTITION);
            boolean outputGraph = line.hasOption(OUTPUT_GRAPH);
            boolean isUndirected = line.hasOption(Center.UNDIRECTED);
            Shared.verbose = line.hasOption(Center.VERBOSE);
            System.out.println(lowerBound + " " + upperBound);
            Graph graph = GraphIO.readGraph(input, isUndirected);

        } catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            showHelp();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Options buildOptions() {
        // create Options object
        Option filter = Option.builder()
                .longOpt(FILTER).desc("only keep links inside (w1, w2) weight interval")
                .hasArgs().numberOfArgs(2).argName("w1 w2").type(Float.class).build();
        Option connectedComponents = Option.builder(CONNECTED_COMPONENTS)
                .longOpt("component").desc("partition the nodes based on connected components").build();
        Option largestCC = Option.builder()
                .longOpt(LARGEST_CC).desc("output the largest connected component").build();
        Option outputPartition = Option.builder(OUTPUT_PARTITION)
                .longOpt("output-partition").desc("output the partition of nodes").build();
        Option outputGraph = Option.builder(OUTPUT_GRAPH)
                .longOpt("output-graph").desc("output the graph[s] of components").build();
        Option help = Option.builder(Center.HELP)
                .longOpt("help")
                .desc("List of options for graph pre-processing").build();
        Options options = Center.getSharedOptions();
        options.addOption(filter).addOption(connectedComponents).addOption(largestCC)
                .addOption(outputPartition).addOption(outputGraph).addOption(help);
        return options;
    }

    @Override
    public void showHelp() {
        String header = "Options used for graph pre-processing:\n\n";
        String footer = "";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(Center.PRE_PROCESS, header, buildOptions(), footer, true);
    }

    @Override
    public void showIntroduction() {

    }
}
