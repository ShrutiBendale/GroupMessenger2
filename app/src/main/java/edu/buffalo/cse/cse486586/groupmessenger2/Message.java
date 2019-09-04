package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

public class Message implements Serializable {
    String msg;
    int msgID;
    int sender_port;
    int proposed_seq;
}
