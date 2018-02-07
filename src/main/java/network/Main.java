package network;

import network.cli.OperationCenter;

/**
 * Application start point
 */
public class Main {
    public static void main(String[] args) {
        // Response to passed arguments
        new OperationCenter().parseOptions(args);
    }
}
