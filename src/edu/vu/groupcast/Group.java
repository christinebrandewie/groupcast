package edu.vu.groupcast;

import java.util.HashSet;

public class Group {
  String mName;
  HashSet<ClientConnection> mMembers = new HashSet<ClientConnection>();
  int mMaxMembers;

  public void addMember(ClientConnection client) {
    synchronized(mMembers) {
      mMembers.add(client);
    }
  }

  public void removeMember(ClientConnection client) throws NonMemberException {
    synchronized(mMembers) {
      if (mMembers.contains(client)) {
        mMembers.remove(client);
      }
      else {
        throw new NonMemberException();
      }
    }
  }

  public int memberCount() {
    synchronized(mMembers) {
      return mMembers.size();
    }
  }

  public String toString() {
    return mName + "(" + memberCount() + "/" + mMaxMembers + ")";
  }
}
