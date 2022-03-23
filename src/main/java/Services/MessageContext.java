package services;

import java.util.List;

public class MessageContext {

    public enum MESSAGE_TYPE {
        NEW_ID,
        LIST,//roomlist
        WHO,//roomcontents
        MESSAGE,
        CREATE_ROOM,
        JOIN_ROOM,//roomchange
        BROADCAST_JOIN_ROOM,//roomchangeall
        ROUTE,
        SERVER_CHANGE,
        DELETE_ROOM,
        QUIT
    }

    public MESSAGE_TYPE messageType;

    public String clientId;
    public String roomId;
    public String formerRoomId;
    public String currentServerId;
    public String availableServerId;
    public String body;
    public String isNewClientIdAvailable;
    public String isNewRoomIdAvailable;
    public String isDeleteRoomAvailable;
    public String isServerChangeAvailable;
    public String targetHost;
    public String targetPort;

    public List<String> memberList;
    public List<String> roomList;

    public MessageContext setMessageType(MESSAGE_TYPE messageType) {
        this.messageType = messageType;
        return this;
    }

    public MessageContext setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public MessageContext setRoomId(String roomId) {
        this.roomId = roomId;
        return this;
    }

    public MessageContext setFormerRoomId(String formerRoomId) {
        this.formerRoomId = formerRoomId;
        return this;
    }

    public MessageContext setCurrentServerId(String currentServerId) {
        this.currentServerId = currentServerId;
        return this;
    }

    public MessageContext setAvailableServerId(String availableServerId) {
        this.availableServerId = availableServerId;
        return this;
    }

    public MessageContext setBody(String body) {
        this.body = body;
        return this;
    }

    public MessageContext setIsNewClientIdAvailable(String isNewClientIdAvailable) {
        this.isNewClientIdAvailable = isNewClientIdAvailable;
        return this;
    }

    public MessageContext setIsNewRoomIdAvailable(String isNewRoomIdAvailable) {
        this.isNewRoomIdAvailable = isNewRoomIdAvailable;
        return this;
    }

    public MessageContext setIsDeleteRoomAvailable(String isDeleteRoomAvailable) {
        this.isDeleteRoomAvailable = isDeleteRoomAvailable;
        return this;
    }

    public MessageContext setIsServerChangeAvailable(String isServerChangeAvailable) {
        this.isServerChangeAvailable = isServerChangeAvailable;
        return this;
    }

    public MessageContext setTargetHost(String targetHost) {
        this.targetHost = targetHost;
        return this;
    }

    public MessageContext setTargetPort(String targetPort) {
        this.targetPort = targetPort;
        return this;
    }

    public MessageContext setMemberList(List<String> memberList) {
        this.memberList = memberList;
        return this;
    }

    public MessageContext setRoomList(List<String> roomList) {
        this.roomList = roomList;
        return this;
    }
}

