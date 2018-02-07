package Operations;

import org.apache.commons.cli.*;

import java.util.Arrays;

public class Main extends AbstractOperation {

    public static final String VERSION_ID = "1.0.0";

    public static final String DETECT = "detect";
    public static final String EVALUATE = "evaluate";
    public static final String ANALYSE = "analyse";

    public static final String GRAPH_INPUT = "f";
    public static final String PARTITION_INPUT = "p";
    public static final String OUTPUT = "o";
    public static final String RESOLUTION = "r";
    public static final String RESOLUTION_DEFAULT = "0.05";
    public static final String ALPHA = "a";
    public static final String ALPHA_DEFAULT = "0.5";
    public static final String HELP = "h";
    public static final String VERSION = "v";

    public static final String ERR_INPUT = "input address is not specified";

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
            case EVALUATE:
            case ANALYSE:
            default:
        }
        try {
            // create the parser
            CommandLineParser parser = new DefaultParser();
            CommandLine line = null;
            line = parser.parse( buildOptions(), args );
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
        Option input = Option.builder(VERSION)
                .longOpt("version").desc("Program version").build();
        Option output = Option.builder(HELP)
                .longOpt("help").desc("List of available commands").build();
        options.addOption(input).addOption(output).addOption(output);
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
}
