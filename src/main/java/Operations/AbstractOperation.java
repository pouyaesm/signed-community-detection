package Operations;

import org.apache.commons.cli.Options;

abstract public class AbstractOperation {
    abstract public void parseOptions(String args[]);
    abstract public Options buildOptions();
    abstract public void showHelp();
    abstract public void showIntroduction();
}
