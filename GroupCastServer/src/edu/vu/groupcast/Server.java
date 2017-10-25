package edu.vu.groupcast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

public class Server {
  private static final Logger LOG =
    Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

  ServerSocket ss = null;

  HashMap<String, ClientConnection> clients = new HashMap<String, ClientConnection>();
  HashMap<String, Group> groups = new HashMap<String, Group>();

  public void start(int port) {

    try {
      ss = new ServerSocket(port);
      LOG.info(this.toString() + " started");

      try {
        while (true) {
          Socket cs = ss.accept();
          LOG.info("Client connection accepted: " + cs.getRemoteSocketAddress().toString());

          new ClientConnection(this, cs).start();
        }
      } catch (IOException e) {
        LOG.severe("Error while accepting connections");
        e.printStackTrace();
      }
    } catch (IOException e) {
      LOG.severe("Cannot create server socket on port " + port);
      e.printStackTrace();
    } finally {
      if (ss != null && !ss.isClosed()) {
        try {
          LOG.info("Attempting to close server socket");
          ss.close();
        } catch (IOException e) {
          LOG.severe("Error closing server socket");
          e.printStackTrace();
        }
      }
    }

  }

  @Override
  public String toString() {
    try {
      return "GroupCast server 1.0 " + InetAddress.getLocalHost().toString() + ":" + ss.getLocalPort();
    } catch (UnknownHostException e) {
      return "GroupCast server 1.0 " + "localhost:" + ss.getLocalPort();
    }
  }

  public void addClient(String name, ClientConnection client) throws ClientNameException {
    synchronized (clients) {
      if (clients.containsKey(name)) {
        throw new ClientNameException();
      }
      clients.put(name, client);
    }
  }

  public void removeClient(ClientConnection client) {
    synchronized (clients) {
      if (clients.containsKey(client.name)) {
        clients.remove(client.name);
      }
    }


    HashSet<Group> groupSet;
    synchronized (groups) {
      groupSet = new HashSet<Group>(groups.values());
    }

    for (Group g : groupSet) {
      try {
        quitGroup(g, client);
      } catch (NonMemberException e) {}
    }

  }

  public Group getGroupByName(String groupName) {
    synchronized (groups) {
      if (groups.containsKey(groupName)) {
        return groups.get(groupName);
      }
      return null;
    }
  }


  public Group joinGroup(String groupName, ClientConnection client, int maxMembers) throws GroupFullException, MaxMembersMismatchException {
    Group g;

    synchronized (groups) {
      if (groups.containsKey(groupName)) {
        // group already exists
        g = groups.get(groupName);

        if (maxMembers > 0 && maxMembers != g.maxMembers) {
          throw new MaxMembersMismatchException();
        }

        if (g.maxMembers == 0 || g.members.size() < g.maxMembers) {
          g.addMember(client);
        }
        else {
          throw new GroupFullException();
        }
      }
      else {
        // new group needed
        g = new Group();
        g.name = groupName;
        g.maxMembers = maxMembers;
        g.addMember(client);
        groups.put(groupName, g);
      }
    }

    return g;
  }

  public void quitGroup(String groupName, ClientConnection client) throws NoSuchGroupException, NonMemberException {
    synchronized (groups) {
      if (groups.containsKey(groupName)) {
        Group g = groups.get(groupName);
        quitGroup(g, client);
      }
      else {
        throw new NoSuchGroupException();
      }
    }
  }

  public void quitGroup(Group g, ClientConnection client) throws NonMemberException {
    synchronized (groups) {
      g.removeMember(client);
      LOG.info("Group members: " + g.members.size());
      if (g.members.isEmpty()) {
        groups.remove(g.name);
      }
    }
  }

  public ClientConnection getClientByName(String clientName) {
    synchronized (clients) {
      if (clients.containsKey(clientName)) {
        return clients.get(clientName);
      }
      return null;
    }
  }

  public static void main(String[] args) {
    int port = 20000;

    // make sure printstream is printing CRLF for newline
    System.setProperty("line.separator", "\r\n");

    if (args.length > 0) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (Exception e) {
        LOG.warning("Invalid port specified: " + args[0]);
        LOG.warning("Using default port " + port);
      }
    }

    Server server = new Server();
    server.start(port);
  }
}
