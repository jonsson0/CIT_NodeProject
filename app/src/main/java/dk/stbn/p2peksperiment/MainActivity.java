package dk.stbn.p2peksperiment;



import android.annotation.SuppressLint;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // UI-elements
    private Button startClient, submitIP;
    private TextView serverInfoTv, clientInfoTv;
    private EditText ipInputField;

    // Logging/status messages
    private String serverinfo = "SERVER LOG:";
    private String clientinfo = "CLIENT LOG: ";

    // Global data
    private final int PORT = 4444;
    private String THIS_IP_ADDRESS = "";
    private String REMOTE_IP_ADDRESS = "";
    private Thread serverThread = new Thread(new MyServerThread());
    private Thread clientThread = new Thread(new MyClientThread());
    private String command = "getId";

    // Some state
    private boolean ip_submitted = false;
    private boolean serverCarryOn = true;

    private boolean clientCarryOn = true;
    private boolean clientStarted = false;

    private boolean hasConnaction = false;

    private Node node;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //UI boilerplate
        startClient = findViewById(R.id.button);
        serverInfoTv = findViewById(R.id.serveroutput);
        clientInfoTv = findViewById(R.id.clientoutput);
        submitIP = findViewById(R.id.sendclient);
        ipInputField = findViewById(R.id.clientmessagefield);

        //Setting click-listeners on buttons
        startClient.setOnClickListener(this);
        submitIP.setOnClickListener(this);

        //Setting some UI state
        ipInputField.setHint("Submit IP-address");
        startClient.setEnabled(false); //deactivates the button

        //Getting the IP address of the device
        THIS_IP_ADDRESS = getLocalIpAddress();
        sUpdate("This IP is " + THIS_IP_ADDRESS);

        //setting up a node
        List<String> nodesLeft = new ArrayList<>();
        nodesLeft.add(THIS_IP_ADDRESS);
        nodesLeft.add(THIS_IP_ADDRESS);
        nodesLeft.add(THIS_IP_ADDRESS);
        List<String> nodesRight = new ArrayList<>();
        nodesRight.add(THIS_IP_ADDRESS);
        nodesRight.add(THIS_IP_ADDRESS);
        nodesRight.add(THIS_IP_ADDRESS);
        node = new Node(THIS_IP_ADDRESS, nodesLeft, nodesRight);


        //Starting the server thread
        serverThread.start();
        serverinfo += "- - - SERVER STARTED - - -\n";

    }

    @Override
    public void onClick(View view) {

        if (view == startClient) {
            if (!clientStarted) {
                //her should bed logic for making a new node in chain
                clientStarted = true;
                clientThread.start();
                clientinfo += "- - - CLIENT STARTED - - - \n";
                startClient.setText("Resend");
            }else{
                if(!ipInputField.getText().toString().equals(REMOTE_IP_ADDRESS)) {
                    command = ipInputField.getText().toString();
                }else{
                    command = "getId";
                }
                Thread clientThread = new Thread(new MyClientThread());
                clientThread.start();
            }
        } else if (view == submitIP) {
            if (!ip_submitted) {
                ip_submitted = true;
                REMOTE_IP_ADDRESS = ipInputField.getText().toString();
                startClient.setEnabled(true);
                submitIP.setEnabled(false);
            }
        }

    }//onclick

    class MyServerThread implements Runnable {
        @SuppressLint("SuspiciousIndentation")
        @Override
        public void run() {
            //Always be ready for next client
            boolean running = true;
            while(true){
                try {
            while (running) {
                    try {
                        ServerSocket serverSocket = new ServerSocket(4444);
                        sUpdate("SERVER: start listening..");
                        Socket nodeSocket = serverSocket.accept();
                        sUpdate("SERVER connection accepted");

                        DataInputStream inNodeStream = new DataInputStream(nodeSocket.getInputStream());
                        DataOutputStream outNodeStream = new DataOutputStream(nodeSocket.getOutputStream());
                        String str;
                        String response;
                        serverCarryOn = true;
                        //Start conversation
                        while (serverCarryOn) {
                            try {
                                str = (String) inNodeStream.readUTF();
                                sUpdate("Client says: " + str);
                                System.out.println("client to server " + str);
                                //logic to handle things
                                if (str.equals("getId")) {
                                    //run with getId
                                    response = node.getId();
                                } else if (str.startsWith("newNeighbor")) {
                                    //run with newNeighbor;id1,id2,id3;left
                                    List<String> commandList = Arrays.asList(str.split(";"));
                                    response = node.newNeighbor(Arrays.asList(commandList.get(1).split(",")), commandList.get(2)).toString();
                                } else if (str.startsWith("GetPhonebookLeft")) {
                                    //run with GetPhonebookLeft
                                    response = node.GetPhonebookLeft().toString();
                                } else if (str.startsWith("GetPhonebookRight")) {
                                    //run with GetPhonebookRight
                                    response = node.GetPhonebookRight().toString();
                                } else if (str.startsWith("GetData")) {
                                    //run with GetData;dataId
                                    List<String> commandList = Arrays.asList(str.split(";"));
                                    response = node.GetData(commandList.get(1));
                                } else if (str.startsWith("RemoveData")) {
                                    //run with RemoveData;dataId
                                    List<String> commandList = Arrays.asList(str.split(";"));
                                    node.RemoveData(commandList.get(1));
                                    response = "Data Removed";
                                } else if (str.startsWith("AddData")) {
                                    //run like AddData;{some json data can not have ;}
                                    List<String> commandList = Arrays.asList(str.split(";"));
                                    response = node.AddData(commandList.get(1));
                                } else {
                                    response = "Fail";
                                }

                                sUpdate(response);
                                outNodeStream.writeUTF(response);
                                outNodeStream.flush();
                                waitABit();
                                serverCarryOn = false;
                            } catch (Exception e) {
                                e.printStackTrace();
                                throw new RuntimeException(e);
                            }
                        }//serverCarryOn loop

                        //Closing everything down
                        nodeSocket.shutdownInput();
                        sUpdate("SERVER: inputstream closed");
                        nodeSocket.shutdownOutput();
                        sUpdate("SERVER: outputstream closed");
                        nodeSocket.close();
                        sUpdate("SERVER: Client socket closed");
                        serverSocket.close();
                        sUpdate("SERVER: Server socket closed");
                    } catch (IOException e) {
                        sUpdate("oops!!");
                        throw new RuntimeException(e);
                    }
                }//running loop
            }catch (Exception e){
                    running = false;
                }
            }//While loop
        }//run
    }//runnable

    // !!! Returns 0.0.0.0 on emulator
    //Modified from https://www.tutorialspoint.com/sending-and-receiving-data-with-sockets-in-android
    private String getLocalIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        String address = null;
        try {
            address = InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return address;
    }

    class MyClientThread implements Runnable {
        @Override
        public void run() {

            try {
                cUpdate("CLIENT: starting client socket ");
                Socket connectionToServer = new Socket(REMOTE_IP_ADDRESS, 4444);
                cUpdate("CLIENT: client connected ");

                DataInputStream inClientStream = new DataInputStream(connectionToServer.getInputStream());
                DataOutputStream outClientStream = new DataOutputStream(connectionToServer.getOutputStream());
                String messageFromServer;

                outClientStream.writeUTF(command);
                outClientStream.flush();
                cUpdate("I said:      " + command);
                messageFromServer = inClientStream.readUTF();
                cUpdate("Server says: " + messageFromServer);
                waitABit();
                connectionToServer.shutdownInput();
                cUpdate("CLIENT: closed inputstream");
                connectionToServer.shutdownOutput();
                cUpdate("CLIENT: closed outputstream");
                connectionToServer.close();
                cUpdate("CLIENT: closed socket");

            }catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

        }//run()
    } //class MyClientThread

    //Wait by setting the thread to sleep for 1,5 seconds
    private void waitABit() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    //The below two methods are for updating UI-elements on the main thread

    //Server update TexView
    private void sUpdate(String message) {
        //Run this code on UI-thread
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                serverinfo = message + "\n" + serverinfo;
                serverInfoTv.setText(serverinfo);
            }
        });

    }

    //Client update TextView
    private void cUpdate(String message) {
        System.out.println(message);

        //Run this code on UI-thread
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                clientinfo = message + "\n" + clientinfo;
                clientInfoTv.setText(clientinfo);
            }
        });
    }
}