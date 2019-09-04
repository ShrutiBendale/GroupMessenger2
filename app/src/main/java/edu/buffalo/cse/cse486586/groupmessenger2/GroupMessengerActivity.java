package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.telephony.TelephonyManager;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;

import static java.lang.Integer.parseInt;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {


    static final int SERVER_PORT = 10000;
    static int keynumber = 0;
    private static final String TAG = "GroupMessaging";
    static int proposed_priority=-1;
    static int unique_id = 0;

    public Comparator<Message> priorityComparator = new PriorityComparator();
    public PriorityQueue<Message> MsgQueue = new PriorityQueue(25, priorityComparator);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((parseInt(portStr) * 2));
        Log.i(TAG, "check 1");


        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));



        try {
            ServerSocket serverSocket = new ServerSocket(10000);
            Log.i(TAG,"check 2");
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText("");
                tv.append("\t" + msg+"\n");

                //Creating an object of the class 'Message' with the message and the senders port number
                Message msgObject = new Message();
                msgObject.msg = msg;
                msgObject.sender_port = parseInt(myPort);

                //Sending the Message object to the client task
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgObject);
            }
        });
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {


        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }
        Uri Uri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");



        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            Log.i("Server", "Initiated");

            ServerSocket serverSocket = sockets[0];
            DataOutputStream outmessage = null;
            DataInputStream inmessage = null;
            Random rand = new Random();

            try {
                while(true) {
                    Socket socket = serverSocket.accept();
                    inmessage = new DataInputStream(socket.getInputStream()); //This could be either the 'new message' for proposals or the final broadcast message with the max sequence number
                    Log.i("Server", "Server check");
                    String inmsg = inmessage.readUTF(); //https://stackoverflow.com/questions/19564643/datainputstream-and-outputstream-write-read-string-with-length?rq=1
                    Thread.sleep(rand.nextInt(51 + 50));
                    String[] inputstuff = inmsg.split("###"); //msg type, msd id, sender port, msg
                    Log.i("Server input msg type", inputstuff[0]);
                    Log.i("Server input port num", inputstuff[2]);
                    Log.i("Server input msg", inputstuff[3]);
                    Log.i("Server port num", inputstuff[4]);


                    //if the message is a new message, send proposals for sequence numbers
                    if(inputstuff[0].equals("newmessage"))
                    {
                        Log.i("Server",  "new msg");

                        proposed_priority = proposed_priority + rand.nextInt(50); //proposed priority is the priority of last message + some random integer
                        Log.i("Server",  Integer.toString(proposed_priority));
                        outmessage = new DataOutputStream(socket.getOutputStream());

                        outmessage.writeUTF(inputstuff[4]+"###"+ proposed_priority); //send the proposed priority
                        outmessage.flush();;
                    }

                    //if the message is the final multicast message, add to the priority queue wrt the sequence numbers
                    if(inputstuff[0].equals("broadcastmessage"))
                    {
                        Log.i("Server",  "broadcast msg");

                        if(Integer.parseInt(inputstuff[3]) >= proposed_priority){
                            proposed_priority = Integer.parseInt(inputstuff[3]) + 1;
                        }
                        Message msgObj2 = new Message();
                        msgObj2.msg = inputstuff[4];
                        msgObj2.msgID = Integer.valueOf(inputstuff[1]);
                        msgObj2.proposed_seq = Integer.valueOf(inputstuff[3]);
                        msgObj2.sender_port = Integer.valueOf(inputstuff[2]);
                        MsgQueue.add(msgObj2);
                    }

                    //publish the messages in the queue one by one
                    while(MsgQueue.peek()!=null){
                        Message msg = MsgQueue.poll();
                        Log.i("Queue messages", "Reading message : " + msg.msg );
                        Log.i("Queue messages","priority :" + msg.proposed_seq);
                        //Publish the results which passes the arguments to onProgressUpdate().
                        publishProgress(msg.msg);
                    }
                    socket.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (NullPointerException e){
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(String... strings) {

            String strReceived = strings[0].trim();
            TextView TextView = (TextView) findViewById(R.id.textView1);
            TextView.append(strReceived + "\t\n");

            //Pass the key-value pair to the insert() function using the content provider
            //https://developer.android.com/reference/android/content/ContentValues
            ContentValues keyValuePair = new ContentValues();
            keyValuePair.put("key", keynumber);
            keynumber++;
            keyValuePair.put("value",strReceived);
            getContentResolver().insert(Uri,keyValuePair);

            return;
        }
    }

    private class ClientTask extends AsyncTask<Message, Void, Void> {

        LinkedList<Integer> proposals = new LinkedList<Integer>(); //stores all the proposed sequence numbers from all the ports
        String failedPort = "null";
        Socket socket = null;
        String msgID = null;
        int remotePort;

        @Override
        protected Void doInBackground(Message... msgs) {

            String clientmsg = String.valueOf(msgs[0].msg);
            String clientport = String.valueOf(msgs[0].sender_port);

            Log.i("ClientTask", "initiated");

            //tISIS algorithm

            //sending the message to all servers and asking for proposals for sequence numbers
            for (int i = 11108; i <= 11124; i += 4) {

                try {
                        remotePort = i;
                        msgID = clientport + Integer.toString(unique_id);

                        //send the message for proposals
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), remotePort);
                        Log.i("ClientTask", msgID);
                        DataOutputStream clientmessage = new DataOutputStream(socket.getOutputStream());
                        clientmessage.writeUTF("newmessage" + "###" + msgID + "###" + clientport + "###" + clientmsg + "###" + remotePort); //Writes to string
                        clientmessage.flush();

                        //server will send the proposed sequence number
                        DataInputStream proposed_seq = new DataInputStream(socket.getInputStream());
                        String inputMessage = proposed_seq.readUTF();
                        String[] inputstuff = inputMessage.split("###"); //port, proposed priority
                        proposals.add(Integer.parseInt(inputstuff[1])); //adding all the proposals in a list
                        Log.i("Client proposals", inputstuff[1]);
                        socket.close();


                } catch (IOException e){
                    Log.e(TAG,"Failure at port "+ remotePort); //Catches the failed port and skips that port in the for loop

                }
                catch (NullPointerException e){
                    Log.e(TAG,"Null pointer exception  at port: "+String.valueOf(socket.getPort()));
                }
            }

            //After all the ports send proposals, calculate the maximum proposed sequence number
            int max = maxElement(proposals);
            Log.i("Client max priority:",String.valueOf(max));

            //multicasting the message with the highest proposed sequence number
            for (int i = 11108; i <= 11124; i += 4) {
                try {
                    int remotePort = i;
                    //send the message ID, message, port number, selected sequence number
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), remotePort);
                    DataOutputStream clientmessage2 = new DataOutputStream(socket.getOutputStream());
                    clientmessage2.writeUTF("broadcastmessage" + "###" + msgID + "###" + clientport + "###" + max + "###" + clientmsg); //Writes to string
                    clientmessage2.flush();
                    unique_id++;

                } catch (NullPointerException e) {
                    failedPort = String.valueOf(socket.getPort());
                    Log.e(TAG, "ClientTask NullPointerException");
                } catch (UnknownHostException e) {
                    failedPort = String.valueOf(socket.getPort());
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG,"Multicast Failure at port "+ remotePort); //Catches the failed port and skips that port in the for loop

                }
            }
            return null;
        }

        //function to calculate the maximum element in a linked list
        public int maxElement(LinkedList<Integer> x) {
            int max = 0;
            for (int i = 0; i < x.size(); i++) {
                if (x.get(i) > max)
                    max = x.get(i);
            }
            return max;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
