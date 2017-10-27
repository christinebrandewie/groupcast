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
          Socket clientSocket = mServerSocket.accept();
          LOG.info("Client connection accepted: " + clientSocket.getRemoteSocketAddress().toString());
          new ClientConnection(this, clientSocket).start();
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

    for (Group group : groupSet) {
      try {
        quitGroup(group, client);
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
    Group group;

    synchronized (mGroups) {
      if (mGroups.containsKey(groupName)) {
        group = mGroups.get(groupName);

        if (maxMembers > 0 && maxMembers != group.mMaxMembers) {
          throw new MaxMembersMismatchException();
        }

        if (group.mMaxMembers == 0 || group.mMembers.size() < group.mMaxMembers) {
          group.addMember(client);
        }
        else {
          throw new GroupFullException();
        }
      }
      else {
        group = new Group();
        group.mName = groupName;
        group.mMaxMembers = maxMembers;
        group.addMember(client);
        mGroups.put(groupName, group);
      }
    }

    return group;
  }

  public void quitGroup(String groupName, ClientConnection client) throws NoSuchGroupException, NonMemberException {
    synchronized (mGroups) {
      if (mGroups.containsKey(groupName)) {
        Group group = mGroups.get(groupName);
        quitGroup(group, client);
      }
      else {
        throw new NoSuchGroupException();
      }
    }
  }

  public void quitGroup(Group group, ClientConnection client) throws NonMemberException {
    synchronized (mGroups) {
      group.removeMember(client);
      LOG.info("Group members: " + group.mMembers.size());
      if (group.mMembers.isEmpty()) {
        mGroups.remove(group.mName);
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
