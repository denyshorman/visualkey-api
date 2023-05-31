package visualkey.service.signer

import visualkey.util.sign
import org.web3j.crypto.Credentials

class AppSigner(privateKey: String) {
    private val credentials = Credentials.create(privateKey)

    fun sign(message: String): String {
        return message.sign(credentials)
    }
}
