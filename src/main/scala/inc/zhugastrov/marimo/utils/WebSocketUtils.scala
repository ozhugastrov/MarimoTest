package inc.zhugastrov.marimo.utils

import org.http4s.websocket.WebSocketFrame
import org.http4s.client.websocket.WSFrame
import scodec.bits.ByteVector

object WebSocketUtils {
  given wsDataFrameToWebSocketFrame: Conversion[WSFrame, WebSocketFrame] = {
    case WSFrame.Text(data, last) => WebSocketFrame.Text(data, last)
    case WSFrame.Binary(data, last) => WebSocketFrame.Binary(data, last)
    case WSFrame.Ping(data) => WebSocketFrame.Ping(data)
    case WSFrame.Pong(data) => WebSocketFrame.Pong(data)
    case WSFrame.Close(closeCode, reason) => WebSocketFrame.Close(ByteVector.apply(closeCode.toByte) ++ ByteVector.view(reason.getBytes))
  }

  given webSocketFrameToWSFrame: Conversion[WebSocketFrame, WSFrame] = {
    case WebSocketFrame.Text(data, last) => WSFrame.Text(data, last)
    case WebSocketFrame.Binary(data, last) => WSFrame.Binary(data, last)
    case WebSocketFrame.Ping(data) => WSFrame.Ping(data)
    case WebSocketFrame.Pong(data) => WSFrame.Pong(data)
    case ws@WebSocketFrame.Close(_) => WSFrame.Close(ws.closeCode, ws.reason)
  }

}
