public class Driver {
  private static final int SERVER_PORT = 20000;

  public static void main(String[] args) {
    int port = SERVER_PORT;

    // make sure printstream is printing CRLF for newline
    System.setProperty("line.separator", "\r\n");

    if (args.length > 0) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (Exception e) {
        // LOG.warning("Invalid port specified: " + args[0]);
        // LOG.warning("Using default port " + port);
      }
    }

    Server server = new Server();
    server.start(port);
  }
}
