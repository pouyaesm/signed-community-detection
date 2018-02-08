package network.cli;

import network.Shared;
import network.core.ConnectedComponents;
import network.core.Graph;
import network.core.GraphIO;
import network.core.ListMatrix;
import org.apache.commons.cli.*;

import java.io.File;

public class PreProcess extends AbstractOperation{

    public static final String FILTER = "filter"; // filter the weights
    public static final String CONNECTED_COMPONENTS = "c";
    public static final String LARGEST_CC = "largest"; // largest connected component
    public static final String OUTPUT_PARTITION = "p"; // output the partition of cc or lcc
    public static final String OUTPUT_GRAPH = "g"; // output the graph[s] of cc or lcc
    public static final String OUTPUT_PREFIX = "output-prefix"; // output files prefix

    @Override
    public void parseOptions(String[] args) {
        // create the parser
        CommandLineParser parser = new DefaultParser();
        try {
            // isNumber the command line arguments
            CommandLine line = parser.parse( buildOptions(), args );
            if(line.hasOption(OperationCenter.HELP)){
                showHelp();
                return;
            }
            String input = line.getOptionValue(OperationCenter.INPUT, "");
            if(input.length() == 0){
                throw new ParseException(OperationCenter.ERR_INPUT);
            }
            String output = line.getOptionValue(OperationCenter.OUTPUT, "") + "/";
            if(!new File(output).isDirectory()){
                throw new ParseException(OperationCenter.ERR_DIRECTORY);
            }
            String[] filter = line.getOptionValues(FILTER);
            float lowerBound = filter[0] != null ? Float.parseFloat(filter[0]) : Float.NEGATIVE_INFINITY;
            float upperBound = filter[1] != null ? Float.parseFloat(filter[1]) : Float.POSITIVE_INFINITY;
            boolean outputCCs = line.hasOption(CONNECTED_COMPONENTS);
            boolean outputLargestCC = line.hasOption(LARGEST_CC);
            boolean outputPartition = line.hasOption(OUTPUT_PARTITION);
            boolean outputGraph = line.hasOption(OUTPUT_GRAPH);
            boolean isUndirected = line.hasOption(OperationCenter.UNDIRECTED);
            String outputPrefix = line.getOptionValue(OUTPUT_PREFIX, "");
            Shared.verbose = line.hasOption(OperationCenter.VERBOSE);
            System.out.println(lowerBound + " " + upperBound);
            // Read the graph
            Graph graph;
            try {
                graph = GraphIO.readGraph(input, isUndirected);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            // Filter the graph links
            if(lowerBound > Float.NEGATIVE_INFINITY || upperBound < Float.POSITIVE_INFINITY){
                graph = graph.filter(lowerBound, upperBound);
            }
            ConnectedComponents connectedComponents = new ConnectedComponents(graph).find();
            // Extract the connected components
            if(outputCCs){
                int[] components = connectedComponents.getComponents();
                if(outputPartition){
                    GraphIO.writePartition(graph, components,
                            output + outputPrefix + "ccs.txt");
                }
                if(outputGraph){
                    // Write each component to a separate edge list
                    Graph[] graphs = graph.decompose(components);
                    ListMatrix[] listMatrices = new ListMatrix[graphs.length];
                    String[] addresses = new String[graphs.length];
                    for(int graphId = 0 ; graphId < graphs.length ; graphId++){
                        if(graphs[graphId] == null) continue;
                        listMatrices[graphId] = graphs[graphId];
                        addresses[graphId] = output + outputPrefix
                                + "component-" + graphId + ".txt";
                    }
                    GraphIO.writeListMatrix(listMatrices, addresses);
                }
            }
            // Extract the largest connected component
            if(outputLargestCC) {
                int[] largestComponent = connectedComponents.getLargestComponent();
                if(outputPartition){
                    GraphIO.writePartition(graph, largestComponent,
                            output + outputPrefix + "lcc.txt");
                }
                if(outputGraph){
                    GraphIO.writeListMatrix(graph,
                            output + outputPrefix + "largest-component.txt");
                }
            }
        } catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            showHelp();
        }
    }

    @Override
    public Options buildOptions() {
        // create Options object
        Option outputPrefix = Option.builder()
                .longOpt(OUTPUT_PREFIX).desc("this prefix is appended to file names")
                .hasArg().argName("prefix").type(Float.class).build();
        Option filter = Option.builder()
                .longOpt(FILTER).desc("only keep links inside (w1, w2) weight interval")
                .hasArgs().numberOfArgs(2).argName("w1 w2").type(Float.class).build();
        Option connectedComponents = Option.builder(CONNECTED_COMPONENTS)
                .longOpt("component").desc("partition the nodes based on connected components").build();
        Option largestCC = Option.builder()
                .longOpt(LARGEST_CC).desc("output the largest connected component").build();
        Option outputPartition = Option.builder(OUTPUT_PARTITION)
                .longOpt("output-partition").desc("output the partitions").build();
        Option outputGraph = Option.builder(OUTPUT_GRAPH)
                .longOpt("output-graph").desc("output the graph[s] of components").build();
        Option help = Option.builder(OperationCenter.HELP)
                .longOpt("help")
                .desc("List of options for graph pre-processing").build();
        Options options = OperationCenter.getSharedOptions();
        options.addOption(outputPrefix).addOption(filter).addOption(connectedComponents)
                .addOption(largestCC).addOption(outputPartition).addOption(outputGraph)
                .addOption(help);
        return options;
    }

    @Override
    public void showHelp() {
        String header = "Options used for graph pre-processing:\n\n";
        String footer = "";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(OperationCenter.PRE_PROCESS, header, buildOptions(), footer, true);
    }

    @Override
    public void showIntroduction() {
        showHelp();
    }
}
