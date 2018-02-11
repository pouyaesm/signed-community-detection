package network;

/**
 * Package.Shared variables such as verbose logging
 */
public class Shared {
    private static boolean verbose; // if true progress messages are logged in console

    public static void log(String message){
        if(verbose){
            System.out.println(message);
        }
    }

    public static void setVerbose(boolean verbose) {
        Shared.verbose = verbose;
    }

    public static boolean isVerbose() {
        return verbose;
    }
}
