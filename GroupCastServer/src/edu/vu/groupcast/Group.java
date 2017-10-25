package edu.vu.groupcast;

import java.util.HashSet;

public class Group {
  String name;
  HashSet<ClientConnection> members = new HashSet<ClientConnection>();
  int maxMembers;

  public void addMember(ClientConnection client) {
    synchronized(members) {
      members.add(client);
    }
  }

  public void removeMember(ClientConnection client) throws NonMemberException {
    synchronized(members) {
      if (members.contains(client)) {
        members.remove(client);
      }
      else {
        throw new NonMemberException();
      }

    }
  }

  public int memberCount() {
    synchronized(members) {
      return members.size();
    }
  }

  public String toString() {
    return name + "(" + memberCount() + "/" + maxMembers + ")";
  }
}
