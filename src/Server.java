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

  ServerSocket mServerSocket = null;
  HashMap<String, ClientConnection> mClients = new HashMap<String, ClientConnection>();
  HashMap<String, Group> mGroups = new HashMap<String, Group>();

  public void start(int port) {
    try {
      mServerSocket = new ServerSocket(port);
      LOG.info(this.toString() + " started");

      try {
        while (true) {
          Socket cs = mServerSocket.accept();
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
      if (mServerSocket != null && !mServerSocket.isClosed()) {
        try {
          LOG.info("Attempting to close server socket");
          mServerSocket.close();
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
      return "GroupCast server 1.0 " + InetAddress.getLocalHost().toString() + ":" + mServerSocket.getLocalPort();
    } catch (UnknownHostException e) {
      return "GroupCast server 1.0 " + "localhost:" + mServerSocket.getLocalPort();
    }
  }

  public void addClient(String name, ClientConnection client) throws ClientNameException {
    synchronized (mClients) {
      if (mClients.containsKey(name)) {
        throw new ClientNameException();
      }
      mClients.put(name, client);
    }
  }

  public void removeClient(ClientConnection client) {
    synchronized (mClients) {
      if (mClients.containsKey(client.mName)) {
        mClients.remove(client.mName);
      }
    }

    HashSet<Group> groupSet;
    synchronized (mGroups) {
      groupSet = new HashSet<Group>(mGroups.values());
    }

    for (Group g : groupSet) {
      try {
        quitGroup(g, client);
      } catch (NonMemberException e) {}
    }

  }

  public Group getGroupByName(String groupName) {
    synchronized (mGroups) {
      if (mGroups.containsKey(groupName)) {
        return mGroups.get(groupName);
      }
      return null;
    }
  }


  public Group joinGroup(String groupName, ClientConnection client, int maxMembers) throws GroupFullException, MaxMembersMismatchException {
    Group g;

    synchronized (mGroups) {
      if (mGroups.containsKey(groupName)) {
        // group already exists
        g = mGroups.get(groupName);

        if (maxMembers > 0 && maxMembers != g.mMaxMembers) {
          throw new MaxMembersMismatchException();
        }

        if (g.mMaxMembers == 0 || g.mMembers.size() < g.mMaxMembers) {
          g.addMember(client);
        }
        else {
          throw new GroupFullException();
        }
      }
      else {
        // new group needed
        g = new Group();
        g.mName = groupName;
        g.mMaxMembers = maxMembers;
        g.addMember(client);
        mGroups.put(groupName, g);
      }
    }

    return g;
  }

  public void quitGroup(String groupName, ClientConnection client) throws NoSuchGroupException, NonMemberException {
    synchronized (mGroups) {
      if (mGroups.containsKey(groupName)) {
        Group g = mGroups.get(groupName);
        quitGroup(g, client);
      }
      else {
        throw new NoSuchGroupException();
      }
    }
  }

  public void quitGroup(Group g, ClientConnection client) throws NonMemberException {
    synchronized (mGroups) {
      g.removeMember(client);
      LOG.info("Group members: " + g.mMembers.size());
      if (g.mMembers.isEmpty()) {
        mGroups.remove(g.mName);
      }
    }
  }

  public ClientConnection getClientByName(String clientName) {
    synchronized (mClients) {
      if (mClients.containsKey(clientName)) {
        return mClients.get(clientName);
      }
      return null;
    }
  }
}
