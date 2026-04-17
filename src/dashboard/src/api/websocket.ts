import { Client, IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useAuthStore } from "@/store/authStore";

const wsURL = import.meta.env.VITE_WS_URL ?? "http://localhost:8080/ws";

let client: Client | null = null;

export function connect(): Client {
  if (client && client.active) {
    return client;
  }
  const token = useAuthStore.getState().accessToken;
  client = new Client({
    webSocketFactory: () => new SockJS(`${wsURL}?access_token=${token ?? ""}`),
    reconnectDelay: 3000,
  });
  client.activate();
  return client;
}

export function subscribe(topic: string, handler: (payload: unknown) => void): () => void {
  const c = connect();
  const sub = c.subscribe(topic, (msg: IMessage) => {
    try {
      handler(JSON.parse(msg.body));
    } catch {
      handler(msg.body);
    }
  });
  return () => sub.unsubscribe();
}

export function disconnect(): void {
  client?.deactivate();
  client = null;
}
