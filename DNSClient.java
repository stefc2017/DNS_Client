import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;

/*
 * Fall 2018, Section A01
 * DNSClient.java
 */

public class DNSClient {
  public static int nextIndex; //keeps track of the next index in the dns response/request and is used to determine when the 
                               //dns header/question section is terminated
  
  public static void main(String[] args) throws IOException {
    
    if (args.length != 2) {
      System.out.println("Usage: java DNSClient hostName ipAddressOfDNSServer");
      return;
    }
    
    DatagramSocket socket = null;       // client's datagram socket
    InetAddress address = null;         // addr of server (local host for now)
    DatagramPacket packet = null;       // packet to be sent and received from the dns server (via UDP)
    String hostname = null;        //hostname we want to search for using DNS
    final int portToSendRequestTo = 53; //the port that is used by convention for TCP/UDP requests
    
    System.out.println("Client starting.");
    
    // create datagram socket
    try {
      address = InetAddress.getByName(args[1]);
      hostname = args[0].toLowerCase();
      socket = new DatagramSocket();
    } catch (Exception e) {
      System.out.println("Creation of client's Socket failed.");
      System.exit(1);
    }
    socket.setSoTimeout(5000); //set socket timeout to 5000ms = 5 seconds
    // send request
    byte[] buf = new byte[256];
    buf = constructRequest(hostname);
    
    packet = new DatagramPacket(buf, buf.length, address, portToSendRequestTo);
    socket.send(packet);
    
    // get response
    packet = new DatagramPacket(buf, buf.length); 
    socket.receive(packet);
    
    // display response information
    displayResponseInformation(packet);
    
    // close the streams and  socket
    try {
      socket.close();
    } catch (Exception e) {
      System.out.println("Client couldn't close socket.");
      System.exit(1);
    }
    
    System.out.println("Client finished.");
  }//end main
  
  /*
   * This method takes as an input the hostname that we want to find more information about in the dns request.
   * This method will build the dns request message to send using UDP to a dns server specified in command line.
   */ 
  public static byte[] constructRequest(String hostname){
    byte [] request = new byte[256];
    String [] hostNameParts = hostname.split("\\."); //since the hostname will be in the form of www.google.ca
    int length = -1; //length of the hostname
    
    /* header */
    request[0] = (byte)0xC1; request[1] = (byte)0x08; request[2] = (byte)0x01; request[3] = (byte)0x00; 
    request[4] = (byte)0x00; request[5] = (byte)0x01; request[6] = (byte)0x00; request[7] = (byte)0x00; 
    request[8] = (byte)0x00; request[9] = (byte)0x00; request[10] = (byte)0x00; request[11] = (byte)0x00;
    
    nextIndex = 12;
    
    /* question */
    for(int i = 0; i < hostNameParts.length; i++){
      /* determine the length of the domain name */
      length = hostNameParts[i].length();
      request[nextIndex] = (byte)length;
      nextIndex++;
      
      /* Convert each domain name character to its hex value */
      for(int j = 0; j < hostNameParts[i].length(); j++){
        String hex = Integer.toHexString(hostNameParts[i].charAt(j));
        int decimal = Integer.parseInt(hex, 16);
        request[nextIndex] = (byte)decimal;
        nextIndex++;
      }//end inner for
    }//end outer for
    
    request[nextIndex] = (byte)0x00; nextIndex++; request[nextIndex] = (byte)0x00; nextIndex++; 
    request[nextIndex] = (byte)0x01; nextIndex++; request[nextIndex] = (byte)0x00; nextIndex++;
    request[nextIndex] = (byte)0x01; nextIndex++;
    
    return request;
  }//end constructRequest
  
  /*
   * This method will accept a DatagramPacket which is a dns response sent back from the dns server. This method will
   * parse the dns response and display the relevant information asked for in the assignment.
   */ 
  public static void displayResponseInformation(DatagramPacket packet){
    byte [] packetResponse = packet.getData(); //convert the packet to a byte array
    int index = 0; //current index in the packetResponse array
    int newNextIndex = 0; //will be used to keep track of which index we are in the packetResponse array while retrieving the information
    int num, answerCount, rdataType = 0;
    String ra_rcode = ""; //the 8 bit binary string for ra and rcode
    String ancount = ""; //the 16 bit binary string for ancount
    String recursionAvailable, responseCode, ipAddress = "";
    ArrayList<String> ipAddresses = new ArrayList<String>(); //keep track of all the ipAddresses we found
    
    /* HashMaps */
    HashMap<Character, String> recursionAvailableHashMap = new HashMap<Character, String>();
    HashMap<String, String> responseCodeHashMap = new HashMap<String, String>();
    
    /* Set-up HashMaps */
    recursionAvailableHashMap.put('0', "No"); recursionAvailableHashMap.put('1', "Yes");
    
    responseCodeHashMap.put("0000", "0 - No error condition"); responseCodeHashMap.put("0001", "1 - Format error");
    responseCodeHashMap.put("0010", "2 - Server Failure"); responseCodeHashMap.put("0011", "3 - Name error");
    responseCodeHashMap.put("0100", "4 - Not Implemented"); responseCodeHashMap.put("0101", "5 - Refused");
    
    ByteArrayInputStream byteStrm = new ByteArrayInputStream(packetResponse);
    newNextIndex = nextIndex;
    
    //go through all the data until we have reached the end
    while( (num = byteStrm.read()) != -1 && index < packet.getLength()) {      
      if(index == 3){ //index 3 will contain the recursion available and response code
        ra_rcode = String.format("%8s", Integer.toBinaryString(num)).replace(' ', '0');
      }
      else if(index == 6 || index == 7){ //index 6 and 7 will contain the numbers of answers in the dns message
        ancount += String.format("%8s", Integer.toBinaryString(num)).replace(' ', '0');
      }
      else if(num == 192){ //If the number is 192, it means we are looking at a new answer section
        newNextIndex = index;
        rdataType = 0;
        ipAddress = "";
      }
      else if(index == newNextIndex + 2 || index == newNextIndex + 3){ //If the type field in the answer is 1, then RDATA contains IP address
        rdataType += num;
      }
      else if(rdataType == 1 && (index == newNextIndex+12 || index == newNextIndex+13 || index == newNextIndex+14 || index == newNextIndex+15)){
        //we retrieve the ip address
        if(index == newNextIndex+15){ //if last digit of the ip address, don't put a '.' after the number
          ipAddress += num;
          ipAddresses.add(ipAddress);
        }
        else{ //not the last digit of the ip address
          ipAddress += num + ".";
        }
      }
      else if(index == newNextIndex && num != 192){ //increase the new index to go over the domain name in the rdata of the answer section
        newNextIndex += num + 1;
      }
      else if(rdataType != 1 && index == newNextIndex + 12){ //rdata does not contain ip address, get length of domain name and increase the index
        newNextIndex += 12 + num + 1;
      }
      index++;
    }//end while
    
    recursionAvailable = recursionAvailableHashMap.get(ra_rcode.charAt(0));
    responseCode = responseCodeHashMap.get(ra_rcode.substring(4));
    answerCount = Integer.parseInt(ancount, 2);
    
    System.out.println("Information for the response:");
    System.out.println("Length of the reply in bytes: " + packet.getLength());
    System.out.println("Recursion available: " + recursionAvailable);
    System.out.println("Response code: " + responseCode);
    System.out.println("The number of answers in answers field: " + answerCount);
    System.out.println("The list of ip addresses: " + ipAddresses.toString());
    System.out.println("The number of ip addresses found: " + ipAddresses.size());
  }//end displayResponseInformation
}//end class DNSClient