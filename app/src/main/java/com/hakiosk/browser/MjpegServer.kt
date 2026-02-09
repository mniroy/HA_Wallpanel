package com.hakiosk.browser

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class MjpegServer(private val port: Int) {
    
    companion object {
        private const val TAG = "MjpegServer"
        private const val BOUNDARY = "frame"
    }

    @Volatile
    private var isRunning = false
    private var serverSocket: ServerSocket? = null
    private val clients = ConcurrentHashMap<Socket, DataOutputStream>()
    private var lastFrame: ByteArray? = null

    fun start() {
        if (isRunning) return
        isRunning = true

        Thread {
            try {
                serverSocket = ServerSocket(port)
                Log.d(TAG, "MJPEG Server started on port $port")
                
                while (isRunning) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        Log.d(TAG, "New client connected: ${clientSocket.inetAddress}")
                        handleClient(clientSocket)
                    } catch (e: IOException) {
                        if (isRunning) Log.e(TAG, "Error accepting connection", e)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error starting server", e)
            }
        }.start()
    }

    private fun handleClient(socket: Socket) {
        Thread {
            try {
                val outputStream = DataOutputStream(socket.getOutputStream())
                
                // Send MJPEG header
                outputStream.writeBytes(
                    "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: multipart/x-mixed-replace; boundary=$BOUNDARY\r\n" +
                    "Connection: close\r\n" +
                    "\r\n"
                )
                outputStream.flush()
                
                clients[socket] = outputStream
                
                // Send the last frame immediately if available
                lastFrame?.let { sendFrameToClient(socket, outputStream, it) }

            } catch (e: IOException) {
                Log.e(TAG, "Error initializing client stream", e)
                closeClient(socket)
            }
        }.start()
    }

    fun updateFrame(jpegData: ByteArray) {
        lastFrame = jpegData
        if (clients.isEmpty()) return

        val iterator = clients.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val socket = entry.key
            val outputStream = entry.value
            
            try {
                sendFrameToClient(socket, outputStream, jpegData)
            } catch (e: IOException) {
                Log.e(TAG, "Error sending frame to client, removing", e)
                iterator.remove()
                closeClient(socket)
            }
        }
    }

    private fun sendFrameToClient(socket: Socket, outputStream: DataOutputStream, jpegData: ByteArray) {
        synchronized(socket) {
            outputStream.writeBytes("--$BOUNDARY\r\n")
            outputStream.writeBytes("Content-Type: image/jpeg\r\n")
            outputStream.writeBytes("Content-Length: ${jpegData.size}\r\n")
            outputStream.writeBytes("\r\n")
            outputStream.write(jpegData)
            outputStream.writeBytes("\r\n")
            outputStream.flush()
        }
    }

    private fun closeClient(socket: Socket) {
        try {
            socket.close()
        } catch (e: IOException) {
            // Ignore
        }
        clients.remove(socket)
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing server socket", e)
        }
        
        clients.keys.forEach { closeClient(it) }
        clients.clear()
        Log.d(TAG, "MJPEG Server stopped")
    }
}
