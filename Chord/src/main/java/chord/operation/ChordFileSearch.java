/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chord.operation;

import chord.ChordThread;
import chord.Finger;
import chord.Message;
import chord.Node;
import chord.SHA1Hasher;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static chord.Sender.data;

public class ChordFileSearch {
    
    private Node node;
    
    public ChordFileSearch(Node node){
        this.node = node;
    } 
    
    public List<Finger> searchFile(String fullFileName){
        long fileKey = new SHA1Hasher(fullFileName).getLong();
        if (fileKey >= ChordOperation.RING_SIZE) {
            fileKey -= ChordOperation.RING_SIZE;
        }
        String searchResponse = findKeyUsingFinger(this.node.getFirstPredecessor(), String.valueOf(fileKey));
        return decodeServerResponse(searchResponse);
    } 
    
    private String findKeyUsingFinger(Finger searchFinger, String key){
        String response = "Not found.";
        try {
            // Open socket to chord node
            DatagramSocket socket = new DatagramSocket();

            // Send query to chord
            String message = ChordOperation.FIND_VALUE + " " + key;
            message = Message.customFormat("0000", message.length()) + " " + message;

            byte[] toSend  = message.getBytes();
            InetAddress IPAddress; 
                try {
                    IPAddress = InetAddress.getByName(searchFinger.getAddress());
                    DatagramPacket packet =new DatagramPacket(toSend, toSend.length, IPAddress, searchFinger.getPort());
                    try {
                        socket.send(packet);
                    } catch (IOException ex) {
                        Logger.getLogger(ChordThread.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } catch (UnknownHostException ex) {
                    Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
                }

            System.out.println("Sent: " + message);

            byte[] receive = new byte[65535]; 
            DatagramPacket DpReceive = new DatagramPacket(receive, receive.length); 
            try {
                socket.receive(DpReceive);
            } catch (IOException ex) {
                Logger.getLogger(ChordThread.class.getName()).log(Level.SEVERE, null, ex);
            }

            // Read response from chord
            String serverResponse = data(receive).toString();

            System.out.println("Response from node " + searchFinger.getAddress() + ", port " + searchFinger.getPort() + ", position " + " (" + searchFinger.getId() + "):");

            response = serverResponse;

            // Close connections
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }
    
    private List<Finger> decodeServerResponse(String serverResponse){
         String[] queryContents = serverResponse.split(" ");
         String command = queryContents[1];
         
         List<Finger> fingetList = new ArrayList<>();
         
         if(command.equals(ChordOperation.VALUE_FOUND)){
             int noOfNodes = Integer.valueOf(queryContents[2]);
             
             for(int i=0; i<noOfNodes; i++){
                  Finger fileOwner = new Finger(queryContents[3+(2*i)], Integer.valueOf(queryContents[4+(2*i)]));
                  fingetList.add(fileOwner);
             }
         }
         return fingetList;
    }
}
