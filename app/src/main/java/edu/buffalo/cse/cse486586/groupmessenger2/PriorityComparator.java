package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;

public class PriorityComparator implements Comparator<Message> {
    @Override
    public int compare(Message x, Message y) {
        if (x.proposed_seq < y.proposed_seq) {
            return -1;
        }
        else if (x.proposed_seq > y.proposed_seq) {
            return 1;
        }
        else //if two sequence numbers are same
        {
            if((x.sender_port)< (y.sender_port))
            {
                return -1;
            }
            else if((x.sender_port)> (y.sender_port))
            {
                return 1;
            }
        }
        return 0;

    }
}
