package network;

/**
 * Package.Shared variables such as verbose logging
 */
public class Shared {
    public static boolean verbose; // if true progress messages are logged in console

    public static void log(String message){
        if(verbose){
            System.out.println(message);
        }
    }
}
