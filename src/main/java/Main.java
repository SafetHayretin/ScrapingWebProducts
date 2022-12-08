import org.apache.commons.cli.*;

public class Main {
    public static CommandLine cmd;
    // link | directory to save photos
    public static void main(String[] args) {
        ConnectionHtml con = new ConnectionHtml(args[0], args[1]);
        con.run();
    }

}
