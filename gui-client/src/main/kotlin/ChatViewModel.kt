import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.example.ChatClient

class ChatViewModel(
    private val username: String,
    private val host: String = "127.0.0.1",
    initialChannel: String = "general"
) {
    private val chatClient = ChatClient(
        host = host,
        initialChannel = initialChannel,
        username = username,
        onMessageReceived = { message ->
            messages = messages + message
        }
    )
    
    var messages by mutableStateOf<List<String>>(emptyList())
        private set
    
    var currentChannel by mutableStateOf(initialChannel)
        private set
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    init {
        chatClient.connect()
    }
    
    fun sendMessage(message: String) {
        if (message.isBlank()) return
        
        scope.launch {
            chatClient.sendMessage(message)
        }
    }
    
    fun changeChannel(channel: String) {
        if (channel.isBlank()) return
        currentChannel = channel
        messages = emptyList()
        chatClient.switchChannel(channel)
    }
    
    fun close() {
        chatClient.close()
    }
} 