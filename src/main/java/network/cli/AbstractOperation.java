package network.cli;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

abstract public class AbstractOperation {
    abstract public void parseOptions(String args[]) throws ParseException;
    abstract public Options buildOptions();
    abstract public void showHelp();
    abstract public void showIntroduction();
}
