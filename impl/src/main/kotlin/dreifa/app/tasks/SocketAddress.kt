package dreifa.app.tasks

// todo - should be in commons
data class SocketAddress(val address: String, val port: Int) {
    constructor(socketAddress: String) : this(socketAddress.split(":")[0],
        socketAddress.split(":")[1].toInt())

}