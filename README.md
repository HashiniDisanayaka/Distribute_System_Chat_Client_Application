# Distribute_System_Chat_Client_Application
**CS4262 Distributed Systems**
## Introduction 
This repository is based on the group project for the module - Distributed Systems(CS4262). This is a distributed chat application with two main distributed components namely chat server and chat client. This is the repository for the client side of the application. These two distributed components can run on separate hosts. If you want to find the server side of the application you can find it on :  [GitHub Pages for ChatClient ]( https://github.com/GayashanNA/CS4262_ChatClient).
### Chat Client
Chat clients can join at most one, any available server. Chat clients are programs that can send request to perform following functions, 
- Creating chat room 
- Deleting chat room 
- Joining chat room
- Quitting a chat room
- See the list of available chat rooms in the system
- Find list of client identities currently connected to a given chat room
- **Send chat messages to other chat clients connected to the *same chat room***
 
### Chat Server
There are multiple servers working together to perform tasks for chat clients, where a server is capable of accepting multiple incoming TCP connections from chat clients. After the system is active the number of servers in the system is fixed. The server is responsible for managing a subset of chat rooms. When a server receives a request to create a chat room from a client and creates a chat room, that chat room is managed by the server which created the chat room. When a client wants to move from one chat room to another, if the two rooms are connected to different servers then the client also should connect to the relevant server. The server is responsible for broadcasting messages to the members of the same chat room. And all of the servers maintain a list of chat rooms available in the system. 

One of the servers is selected as the leader after a leader election process, at the initialization. And the leader is responsible for managing the global consistency of the system, the non-leader servers seek the approval of the leader in order to perform certain tasks. Heartbeat is implemented to identify a failure of a non-leader server, and the leader deletes the state of the disconnected server after identifying. The heartbeat is implemented using gossiping and consensus. 

## Executable Jar files
The "executables" folder contains the executable jar files for the client and server.

Command for execute the chat client 

> `java -jar client.jar -h server_address [-p server_port] -i identity [-d]`

eg: java -jar client.jar -h localhost -p 4444 -i Adel

Command for execute the chat server 

> `java -jar server.jar [server_name] "[location of server configuration file]"`

eg: java -jar server.jar s1 "myPathToProject\src\main\java\config\config.txt"
## Instructions to Build the executable Jar
Development Environment - `IntelliJ IDEA`

> install java (version `17.0.2`)
> 
> install Maven (version `4.0.0`)

steps to build the executable jar file:

- Add an artifact using the project structure : File -> Project Structure.. -> Artifacts -> Add -> JAR -> From modules with dependencies..
- Add main class -> Apply -> OK
- Then Build -> Build Artifacts.. -> Build

The output jar will be created inside the `'out'` folder named `Distributed_System_Chat_Client_Application.jar`
## Instructions to Run the Jar
run the following command in a terminal

> `java -jar Distributed_System_Chat_Client_Application.jar s1 "C:code\src\main\java\config\config.txt"`
>
note `s1` should be changed according to the server instance.
note the path to the `config.txt` should be given according to the configuration file location.
