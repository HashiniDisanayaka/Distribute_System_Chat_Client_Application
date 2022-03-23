# Distribute_System_Chat_Client_Application
**CS4262 Distributed Systems**

This repository is based on the group project for the module - Distributed Systems(CS4262). This is a distributed chat application with two main distributed components namely chat server and chat client. This is the repository for the client side of the application. These two distributed components can runs on seperate hosts. If you want to find the server side of the application you can find it on :  [GitHub Pages for ChatClient ]( https://github.com/GayashanNA/CS4262_ChatClient).
## Chat Client
Chat clients can join at most one, any available server. Chat clients are programs that can send request to perform following functions, 
- Creating chat room 
- Deleting chat room 
- Joining chat room
- Quitting a chat room
- See the list of available chat rooms in the system
- Find list of client identities currently connected to a given chat room
- **Send chat messages to other chat clients connected to the *same chat room***
 
## Chat Server
There are multiple servers working together to perform tasks for chat clients, where a server is capable of accepting multiple incoming TCP connections from chat clients. After the system is active the number of servers in the system is fixed. The server is reposible managing a subset of chat rooms. when a server gets are request of creating a chat rooms from a client and created a chat room, that chat room is managed by the server which created the chat room. When a client wants to move from one chat room to another, if the two rooms are connected to different servers then the client also should connet to the relevant server. Server is responsible for broadcasting messages to the members of the same chat room. And all of the servers maintain a list of chat rooms available in the system.  
## Executable Jar files
## Instructions to Build the executable Jar
## Instructions to Run the Jar
