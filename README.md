# DNS_Client
In my course of Computer networks at the University of Manitoba (COMP 4300), for an assignment I implemented a dns client that can make dns requests to dns servers.

COMP 4300, Section A01
Fall 2018
Assignment 2

To compile the dns client for question 2:
javac DNSClient.java

To run the dns client for question 2:
java DNSClient hostName dnsServer (It should resemble): java DNSClient www.google.ca 8.8.8.8

Important to note: I set a timeout of 5 seconds if there is a problem communicating with the dns server.

Sample output:
C:\Users\Stefan\Desktop\umanitoba\2018-2019\Fall 2018\COMP4300\Assignments\A2>java DNSClient www.google.ca 8.8.8.8
Client starting.
Information for the response:
Length of the reply in bytes: 47
Recursion available: Yes
Response code: 0 - No error condition
The number of answers in answers field: 1
The list of ip addresses: [216.58.217.35]
The number of ip addresses found: 1
Client finished.