package main

import auth.Login
import commands.BasicHandlers
import commands.Commands
import commands.ContextHandlers
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import model.Model
import model.RegularContext
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage

class SpotifyApi : MqttCallback {
    override fun messageArrived(topic: String, message: MqttMessage) {
        async(CommonPool) {
            Main.processCommand(String(message.payload))
        }
    }

    override fun connectionLost(cause: Throwable?) {
    }

    override fun deliveryComplete(token: IMqttDeliveryToken?) {
    }
}

object Main {
    val spotifyApi = Login().createAuthenticatedApi()
    val handlers = listOf(
            BasicHandlers(spotifyApi),
            ContextHandlers(spotifyApi)
    )
    var model = Model(mutableListOf(), RegularContext(
            spotifyApi.wrapped.informationAboutUsersCurrentPlayback.build().execute()))

    fun processCommand(commandString: String) {
        val commands = Commands.commandsSplitOnAmpersand(commandString)
        model = commands.fold(model) { model, command ->
            handlers.fold(model) { currentModel, handler ->
                handler.handle(command, currentModel)
            }
        }
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Please supply the host as: <hostname>:<hostip>")
    }
    val host = args[0]
    val client = MqttClient("tcp://$host", MqttClient.generateClientId())
    client.connect()
    client.setCallback(SpotifyApi())
    client.subscribe("/home/spotify")
}