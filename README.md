# Auctioning_System
Advanced Auctioning System

Compiles using: javac -cp jgroups-3.6.14.Final.jar;. *.java
Runs Using: java -cp jgroups-3.6.14.Final.jar;. "FileName"

AuctionServer needs to be run first as this is the front end

ServerReplica are the replicas that run in the back end behind the front end server, 
atleast one needs to be running before a client is started. Can be shut down and booted up at runtime

Client is used by the client to connect to the front end server

Steps:
Start Auction Server (java -cp jgroups-3.6.14.Final.jar;. AuctionServer)
Start Replica's (java -cp jgroups-3.6.14.Final.jar;. ServerReplica)
Start Client's (java -cp jgroups-3.6.14.Final.jar;. Client)
