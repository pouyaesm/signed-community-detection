package network.cli;

import org.apache.commons.cli.*;

import java.util.Arrays;

public class OperationCenter extends AbstractOperation {

    public static final String VERSION_ID = "1.0.0";

    public static final String DETECT = "detect";
    public static final String EVALUATE = "evaluate";
    public static final String ANALYSE = "analyse";
    public static final String PRE_PROCESS = "preprocess";

    public static final String INPUT = "i";
    public static final String OUTPUT = "o";
    public static final String UNDIRECTED = "undirected";
    public static final String RESOLUTION = "r";
    public static final String RESOLUTION_DEFAULT = "0.05";
    public static final String ALPHA = "a";
    public static final String ALPHA_DEFAULT = "0.5";
    public static final String HELP = "h";
    public static final String VERSION = "v";
    public static final String VERBOSE = "verbose";

    public static final String ERR_INPUT = "input address is not specified";
    public static final String ERR_DIRECTORY = "output address must be a directory";

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
            case DETECT:
                new Detection().parseOptions(operationArgs);
                return;
            case PRE_PROCESS:
                new PreProcess().parseOptions(operationArgs);
                return;
            case EVALUATE:
            case ANALYSE:
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
                "\nCommunity Detection in Signed, Directed, and Weighted Networks @ 2018 version " + VERSION_ID +
                        "\nAvailable commands are:\n  " +
                        DETECT + " -h\n  " + EVALUATE + " -h\n  " + ANALYSE + " -h";
        System.out.println(message);
    }

    /**
     * Shared options: input-output address, undirected, verbose
     * @return
     */
    public static Options getSharedOptions(){
        Option input = Option.builder(OperationCenter.INPUT).longOpt("input").desc("Input address")
                .hasArg().argName("address").type(String.class).build();
        Option output = Option.builder(OperationCenter.OUTPUT).longOpt("output")
                .desc("Output address")
                .hasArg().hasArg().argName("address").type(String.class).build();
        Option unDirected = Option.builder()
                .longOpt(UNDIRECTED).desc("when link (a, b) exists, (b, a) is also considered").build();
        Option verbose = Option.builder()
                .longOpt(VERBOSE).desc("output the progress messages").build();
        Options options = new Options()
                .addOption(input).addOption(output)
                .addOption(unDirected).addOption(verbose);
        return options;
    }
}
