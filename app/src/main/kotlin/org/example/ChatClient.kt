package org.example

import com.rabbitmq.client.*
import kotlinx.coroutines.*
import java.io.PrintStream

class ChatClient(
    private val host: String = "127.0.0.1",
    initialChannel: String,
    private val username: String,
    private val port: Int? = null,
    private val outputStream: PrintStream = System.out,
    private val onMessageReceived: ((String) -> Unit)? = null
) {
    private var connection: Connection? = null
    private var channel: Channel? = null
    private var currentChannel: String = initialChannel
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentQueue: String? = null

    fun connect() {
        try {
            val factory = ConnectionFactory()
            factory.host = host
            if (port != null) {
                factory.port = port
            }
            connection = factory.newConnection()
            channel = connection?.createChannel()

            channel?.exchangeDeclare(currentChannel, BuiltinExchangeType.FANOUT)

            currentQueue = channel?.queueDeclare()?.queue
            channel?.queueBind(currentQueue, currentChannel, "")

            startMessageConsumer(currentQueue)

            outputStream.println("Connected to RabbitMQ server at $host")
            outputStream.println("Joined channel: $currentChannel")
        } catch (e: Exception) {
            outputStream.println("Error connecting to RabbitMQ: ${e.message}")
            close()
        }
    }

    private fun startMessageConsumer(queueName: String?) {
        scope.launch {
            try {
                channel?.basicConsume(queueName, true, object : DefaultConsumer(channel) {
                    override fun handleDelivery(
                        consumerTag: String,
                        envelope: Envelope,
                        properties: AMQP.BasicProperties,
                        body: ByteArray
                    ) {
                        val message = String(body, Charsets.UTF_8)
                        outputStream.println("[$currentChannel] $message")
                        onMessageReceived?.invoke(message)
                    }
                })
            } catch (e: Exception) {
                outputStream.println("Error consuming messages: ${e.message}")
            }
        }
    }

    fun switchChannel(newChannel: String) {
        try {
            currentQueue?.let { queue ->
                channel?.queueUnbind(queue, currentChannel, "")
                channel?.queueDelete(queue)
            }

            channel?.exchangeDeclare(newChannel, BuiltinExchangeType.FANOUT)
            currentQueue = channel?.queueDeclare()?.queue
            channel?.queueBind(currentQueue, newChannel, "")

            currentChannel = newChannel
            outputStream.println("Switched to channel: $currentChannel")

            startMessageConsumer(currentQueue)
        } catch (e: Exception) {
            outputStream.println("Error switching channel: ${e.message}")
        }
    }

    fun sendMessage(message: String) {
        try {
            val messageWithUsername = "[$username] $message"
            channel?.basicPublish(
                currentChannel,
                "",
                null,
                messageWithUsername.toByteArray(Charsets.UTF_8)
            )
        } catch (e: Exception) {
            outputStream.println("Error sending message: ${e.message}")
        }
    }

    fun close() {
        try {
            currentQueue?.let { queue ->
                channel?.queueUnbind(queue, currentChannel, "")
                channel?.queueDelete(queue)
            }
            channel?.close()
            connection?.close()
            scope.cancel()
        } catch (e: Exception) {
            outputStream.println("Error closing connection: ${e.message}")
        }
    }
}

fun main(args: Array<String>) {
    val host = args.getOrNull(0) ?: System.getenv("RABBITMQ_HOST")
    val initialChannel = args.getOrNull(1) ?: "general"

    print("Enter your username: ")
    val username = readLine()?.takeIf { it.isNotBlank() } ?: "Anonymous"

    val client = ChatClient(host, initialChannel, username)
    client.connect()

    println("Chat client started as $username. Commands:")
    println("!switch <channel> - Switch to a different channel")
    println("Type your message and press Enter to send")
    println("Type 'exit' to quit")

    while (true) {
        val input = readlnOrNull() ?: continue

        when {
            input.startsWith("!switch ") -> {
                val newChannel = input.substringAfter("!switch ")
                client.switchChannel(newChannel)
            }

            input == "exit" -> {
                client.close()
                break
            }

            else -> {
                client.sendMessage(input)
            }
        }
    }
} 