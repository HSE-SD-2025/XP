import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

@Composable
@Preview
fun UsernameInputWindow(
    onUsernameSubmitted: (String) -> Unit,
    onClose: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    
    Window(
        onCloseRequest = onClose,
        title = "Enter Username",
        state = rememberWindowState(width = 300.dp, height = 150.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { 
                    if (username.isNotBlank()) {
                        onUsernameSubmitted(username)
                    }
                },
                enabled = username.isNotBlank()
            ) {
                Text("Start Chat")
            }
        }
    }
}

@Composable
@Preview
fun ChatWindow(
    username: String,
    onClose: () -> Unit
) {
    val viewModel = remember { ChatViewModel(username) }
    var message by remember { mutableStateOf("") }
    var channelInput by remember { mutableStateOf(viewModel.currentChannel) }
    
    Window(
        onCloseRequest = {
            viewModel.close()
            onClose()
        },
        title = "Chat - $username",
        state = rememberWindowState(width = 600.dp, height = 400.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Header with channel and username info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Channel: ${viewModel.currentChannel}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "User: $username",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Channel selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Switch to:", modifier = Modifier.align(Alignment.CenterVertically))
                OutlinedTextField(
                    value = channelInput,
                    onValueChange = { channelInput = it },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.changeChannel(channelInput) }
                ) {
                    Text("Switch")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Messages area
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    items(viewModel.messages) { message ->
                        Text(
                            text = message,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Message input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") }
                )
                
                Button(
                    onClick = {
                        if (message.isNotBlank()) {
                            viewModel.sendMessage(message)
                            message = ""
                        }
                    },
                    enabled = message.isNotBlank()
                ) {
                    Text("Send")
                }
            }
        }
    }
}

fun main() = application {
    var username by remember { mutableStateOf<String?>(null) }
    
    if (username == null) {
        UsernameInputWindow(
            onUsernameSubmitted = { newUsername ->
                username = newUsername
            },
            onClose = ::exitApplication
        )
    } else {
        ChatWindow(
            username = username!!,
            onClose = ::exitApplication
        )
    }
} 