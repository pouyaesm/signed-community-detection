package network.cli;

import org.apache.commons.cli.*;

import java.util.Arrays;

public class OperationCenter extends AbstractOperation {

    public static final String VERSION_ID = "1.1.4";

    public static final String MDL = "mdl";
    public static final String PRE_PROCESS = "preprocess";

    public static final String INPUT_GRAPH = "g";
    public static final String INPUT_PARTITION = "p";
    public static final String OUTPUT = "o";
    public static final String DIRECTED = "directed";
    public static final String RESOLUTION = "r";
    public static final String ALPHA = "a";
    public static final String ALPHA_DEFAULT = "0.5";
    public static final String HELP = "h";
    public static final String VERSION = "v";
    public static final String VERBOSE = "verbose";

    public static final String ERR_INPUT_GRAPH_NOT_SPECIFIED = "graph address is not specified";
    public static final String ERR_INPUT_PARTITION_NOT_SPECIFIED = "partition address is not specified";
    public static final String ERR_OUTPUT_DIRECTORY = "output address must be a directory";
    public static final String ERR_OUTPUT_NOT_SPECIFIED = "output address is not specified";

    @Override
    public void parseOptions(String[] args) {
        if(args.length <= 1){
            showIntroduction();
            return;
        }
        // remove the operation argument to fed to each operation parser
        String[] operationArgs = Arrays.copyOfRange(args, 1, args.length);
        String operation = args[0];
        switch (operation){
            case MDL:
                new MDL().parseOptions(operationArgs);
                return;
            case PRE_PROCESS:
                new PreProcess().parseOptions(operationArgs);
                return;
            default:
        }
        try {
            // create the parser
            CommandLineParser parser = new DefaultParser();
            CommandLine line = parser.parse( buildOptions(), args );
            if(line.hasOption(VERSION) || line.hasOption(HELP)){
                showIntroduction();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Options buildOptions() {
        // create Options object
        Options options = new Options();
        Option version = Option.builder(VERSION)
                .longOpt("version").desc("Program version").build();
        Option help = Option.builder(HELP)
                .longOpt("help").desc("List of available commands").build();
        options.addOption(version).addOption(version).addOption(help);
        return options;
    }

    @Override
    public void showHelp() {

    }

    @Override
    public void showIntroduction() {
        String message =
                "\nCommunity Detection in Signed, Directed, and Weighted Networks @ 2024 version " + VERSION_ID +
                        "\nAvailable commands are:\n  " +
                        MDL + " -h  for community detection and evaluation\n  " +
                        PRE_PROCESS + " -h  for graph pre-processing\n";
        System.out.println(message);
    }

    /**
     * Shared options: input-output address, undirected, verbose
     * @return
     */
    public static Options getSharedOptions(){
        Option inputGraph = Option.builder(OperationCenter.INPUT_GRAPH)
                .longOpt("graph").desc("Input graph address")
                .hasArg().argName("address").type(String.class).build();
        Option inputPartition = Option.builder(OperationCenter.INPUT_PARTITION)
                .longOpt("partition").desc("Input partition address")
                .hasArg().argName("address").type(String.class).build();
        Option output = Option.builder(OperationCenter.OUTPUT).longOpt("output")
                .desc("Output address")
                .hasArg().hasArg().argName("address").type(String.class).build();
        Option unDirected = Option.builder()
                .longOpt(DIRECTED).desc("when link (a, b) differs from (b, a)").build();
        Option verbose = Option.builder()
                .longOpt(VERBOSE).desc("output the progress messages").build();
        Options options = new Options()
                .addOption(inputGraph).addOption(inputPartition).addOption(output)
                .addOption(unDirected).addOption(verbose);
        return options;
    }
}
