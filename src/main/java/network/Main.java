package network;

import network.cli.Center;

/**
 * Application start point
 */
public class Main {
    public static void main(String[] args) {
        // Response to passed arguments
        new Center().parseOptions(args);
    }
}
