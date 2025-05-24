package org.example

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.concurrent.TimeUnit
import org.awaitility.Awaitility.await

@Testcontainers
class ChatClientE2ETest {
    private val outputStreamClient1 = ByteArrayOutputStream()
    private val outputStreamClient2 = ByteArrayOutputStream()
    private val outputStreamClient3 = ByteArrayOutputStream()

    private val printStreamClient1 = PrintStream(outputStreamClient1)
    private val printStreamClient2 = PrintStream(outputStreamClient2)
    private val printStreamClient3 = PrintStream(outputStreamClient3)

    private lateinit var client1: ChatClient
    private lateinit var client2: ChatClient
    private lateinit var client3: ChatClient

    @Container
    private val rabbitMQ = RabbitMQContainer("rabbitmq:3-management")
        .withExposedPorts(5672, 15672)

    @BeforeEach
    fun setup() {
        rabbitMQ.start()
        val port = rabbitMQ.amqpPort
        
        client1 = ChatClient(
            host = rabbitMQ.host,
            port = port,
            initialChannel = "test-channel",
            outputStream = printStreamClient1,
            username = "1"
        )
        client2 = ChatClient(
            host = rabbitMQ.host,
            port = port,
            initialChannel = "test-channel",
            outputStream = printStreamClient2,
            username = "2"
        )
        client3 = ChatClient(
            host = rabbitMQ.host,
            port = port,
            initialChannel = "test-channel",
            outputStream = printStreamClient3,
            username = "3"
        )
    }

    @AfterEach
    fun cleanup() {
        client1.close()
        client2.close()
        client3.close()
        rabbitMQ.stop()
    }

    @Test
    fun `test basic message exchange between two clients`() {
        client1.connect()
        client2.connect()

        val message = "Hello from client1!"
        client1.sendMessage(message)

        await().atMost(10, TimeUnit.SECONDS).until {
            outputStreamClient2.toString().contains(message)
        }

        val newChannel = "new-channel"
        client2.switchChannel(newChannel)

        val message2 = "This should not be received"
        client1.sendMessage(message2)

        await().atMost(5, TimeUnit.SECONDS).until {
            !outputStreamClient2.toString().contains(message2)
        }

        val message3 = "Hello from client2!"
        client2.sendMessage(message3)

        await().atMost(5, TimeUnit.SECONDS).until {
            !outputStreamClient1.toString().contains(message3)
        }
    }

    @Test
    fun `test multiple clients in same channel`() {
        client1.connect()
        client2.connect()
        client3.connect()

        val message1 = "Message from client1"
        val message2 = "Message from client2"
        val message3 = "Message from client3"

        client1.sendMessage(message1)
        client2.sendMessage(message2)
        client3.sendMessage(message3)

        await().atMost(10, TimeUnit.SECONDS).until {
            outputStreamClient1.toString().contains(message2) &&
            outputStreamClient1.toString().contains(message3) &&
            outputStreamClient2.toString().contains(message1) &&
            outputStreamClient2.toString().contains(message3) &&
            outputStreamClient3.toString().contains(message1) &&
            outputStreamClient3.toString().contains(message2)
        }
    }

    @Test
    fun `test channel switching and message isolation`() {
        client1.connect()
        client2.connect()
        client3.connect()

        val initialMessage = "Initial message"
        client1.sendMessage(initialMessage)
        await().atMost(5, TimeUnit.SECONDS).until {
            outputStreamClient2.toString().contains(initialMessage) &&
            outputStreamClient3.toString().contains(initialMessage)
        }

        val channel2 = "channel2"
        client2.switchChannel(channel2)
        val message2 = "Message in channel2"
        client2.sendMessage(message2)

        val channel3 = "channel3"
        client3.switchChannel(channel3)
        val message3 = "Message in channel3"
        client3.sendMessage(message3)

        await().atMost(5, TimeUnit.SECONDS).until {
            !outputStreamClient1.toString().contains(message2) &&
            !outputStreamClient1.toString().contains(message3) &&
            !outputStreamClient2.toString().contains(message3) &&
            !outputStreamClient3.toString().contains(message2)
        }
    }

    @Test
    fun `test reconnection after disconnection`() {
        client1.connect()
        client2.connect()

        val initialMessage = "Initial message"
        client1.sendMessage(initialMessage)
        await().atMost(5, TimeUnit.SECONDS).until {
            outputStreamClient2.toString().contains(initialMessage)
        }

        client1.close()
        client1 = ChatClient(
            host = rabbitMQ.host,
            port = rabbitMQ.amqpPort,
            initialChannel = "test-channel",
            outputStream = printStreamClient1,
            username = "1"
        )
        client1.connect()

        val reconnectedMessage = "Message after reconnection"
        client1.sendMessage(reconnectedMessage)
        await().atMost(5, TimeUnit.SECONDS).until {
            outputStreamClient2.toString().contains(reconnectedMessage)
        }
    }
} 