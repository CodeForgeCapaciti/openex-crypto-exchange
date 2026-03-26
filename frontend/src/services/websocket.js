// src/services/websocket.js
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class WebSocketService {
  constructor() {
    this.client = null;
    this.connected = false;
    this.messageHandlers = [];
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
  }

  connect() {
    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      debug: (str) => {
        console.log('STOMP:', str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.client.onConnect = () => {
      this.connected = true;
      this.reconnectAttempts = 0;
      console.log('WebSocket connected successfully');

      // Subscribe to market data topic
      this.client.subscribe('/topic/market', (message) => {
        try {
          const data = JSON.parse(message.body);
          console.log('Received WebSocket message:', data.type);
          this.messageHandlers.forEach(handler => handler(data));
        } catch (error) {
          console.error('Error parsing WebSocket message:', error);
        }
      });
    };

    this.client.onStompError = (frame) => {
      console.error('STOMP error:', frame);
    };

    this.client.onWebSocketError = (error) => {
      console.error('WebSocket error:', error);
      this.connected = false;
    };

    this.client.onDisconnect = () => {
      console.log('WebSocket disconnected');
      this.connected = false;

      // Attempt to reconnect
      if (this.reconnectAttempts < this.maxReconnectAttempts) {
        this.reconnectAttempts++;
        setTimeout(() => this.connect(), 5000);
      }
    };

    this.client.activate();
  }

  disconnect() {
    if (this.client && this.connected) {
      this.client.deactivate();
      this.connected = false;
      console.log('WebSocket disconnected manually');
    }
  }

  onMessage(handler) {
    this.messageHandlers.push(handler);
  }

  sendMessage(destination, message) {
    if (this.connected && this.client) {
      this.client.publish({
        destination: destination,
        body: JSON.stringify(message)
      });
    } else {
      console.warn('WebSocket not connected, message not sent');
    }
  }
}

export const websocketService = new WebSocketService();