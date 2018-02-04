import org.apache.commons.cli.*;

public class Main {

    private static final String INPUT = "i";
    private static final String OUTPUT = "o";
    private static final String RESOLUTION = "r";
    private static final String ALPHA = "a";
    private static final String ITERATION = "i";
    private static final String EVALUATE = "e";
    private static final String DETECT = "d";
    private static final String HELP = "h";

    public static void main(String[] args) {

        // create the parser
        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse( getOptions(), args );
            if(line.hasOption(INPUT)){
                System.out.println(line.getOptionValue(INPUT));
            }
            if(line.hasOption(OUTPUT)){
                System.out.println(line.getOptionValue(OUTPUT));
            }
            if(line.hasOption(RESOLUTION)){
                System.out.println(line.getOptionValue(RESOLUTION));
            }
            if(line.hasOption(HELP)){
                showHelp();
            }
            System.out.println(line.getOptionValue(ALPHA, "0.5"));
            System.out.println(line.getOptionValue(ITERATION, "2"));
        }
        catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            showHelp();
        }
    }

    public static void showHelp(){
        String header = "You can do community detection and partition evaluation\n\n";
        String footer = "\nPlease report issues at http://example.com/issues";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Main", header, getOptions(), footer, true);
    }

    /**
     * Return the list of available argument objects to be passed to the program
     * @return
     */
    public static Options getOptions(){
        // create Options object
        Options options = new Options();
        Option input = Option.builder(INPUT)
                .required().longOpt("input").desc("Input file or directory.").hasArg().build();
        Option output = Option.builder(OUTPUT)
                .required().longOpt("output")
                .desc("Output location. For multiple input files, the closest directory is used.")
                .hasArg().build();
        Option resolution = Option.builder(RESOLUTION)
                .longOpt("resolution").desc("Resolution for fast community detection at a specific scale;" +
                        " by default, the best resolution is searched by minimizing the Minimum Description Length")
                .hasArg().build();
        Option alpha = Option.builder(ALPHA)
                .longOpt("alpha")
                .desc("Relative importance of positive links compared to negative links; default is 0.5")
                .hasArg().build();
        Option refineCount = Option.builder(ITERATION)
                .longOpt("iteration")
                .desc("Number of times partition is further refined for improvement; default is 2")
                .hasArg().build();
        Option evaluate = Option.builder(ITERATION)
                .longOpt("evaluate")
                .desc("Output the partition quality based on input partition")
                .hasArg().build();
        Option detect = Option.builder(ITERATION)
                .longOpt("iteration")
                .desc("Number of times partition is further refined for improvement; default is 2")
                .hasArg().build();
        Option help = Option.builder(ITERATION)
                .longOpt("help")
                .desc("See how to use the program").build();
        options.addOption(input).addOption(output).addOption(resolution)
                .addOption(alpha).addOption(refineCount).addOption(help);
        return options;
    }
}
