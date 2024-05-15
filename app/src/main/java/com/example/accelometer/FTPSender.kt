
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.accelometer.Vysledek
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class FTPSender {

    private lateinit var server: String
    private lateinit var port: String
    private lateinit var username: String
    private lateinit var password: String
    private lateinit var defaultDirectory: String
    private lateinit var localFilePath: String
    private lateinit var remoteFileName: String
    private var hardwareSend: Boolean = false
    private lateinit var context: Context
    private val mainHandler = Handler(Looper.getMainLooper())

    fun init(
        context: Context,
        localFilePath: String,
        remoteFileName: String,
        hardwareSend: Boolean
    ) {
        this.context = context
        val sharedPreferences = context.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        server = sharedPreferences.getString("host", null).toString()
        username = sharedPreferences.getString("username", null).toString()
        password = sharedPreferences.getString("password", null).toString()
        defaultDirectory = sharedPreferences.getString("directory", null).toString()
        port = sharedPreferences.getString("port", null).toString()
        this.localFilePath = localFilePath
        this.remoteFileName = remoteFileName
        this.hardwareSend = hardwareSend
    }
    //Datová třída, která umožnňuje funkci vracet několik hodnot různých typů
    //data class Vysledek(val status: Boolean, val chyba: String, val kod: Int)

    fun uploadFileToFTPAsync(): Vysledek {
        val latch = CountDownLatch(1)
        var status = false
        var kod = 0
        var chyba = ""

        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            val pole = uploadFileToFTP()
            status = pole.status
            kod = pole.kod
            chyba = pole.chyba
            latch.countDown()
        }

        try {
            // Wait for the latch to count down to zero
            latch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //Log.d("Status in FTP main thread", status.toString())
        return Vysledek(status,chyba,kod)
    }


    private fun uploadFileToFTP(): Vysledek {
        val ftpClient = FTPClient()
        var status = false
        var kod = 0
        var chyba = ""
        if (isConnectedToInternet(context)) {
            try {
                // Connect to FTP server
                ftpClient.connect(server)
                ftpClient.login(username, password)
                kod = ftpClient.getReplyCode();
                // Set transfer mode and type
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
                ftpClient.enterLocalPassiveMode()
                // Change working directory if needed
                Log.d("Adresář", defaultDirectory)
                try {
                    ftpClient.changeWorkingDirectory(defaultDirectory)
                } catch (e: IOException) {
                    Log.e("Directory", "Nepovedlo se nastavit požadovaný adresář")
                }

                //I když se adresář úspěšně změní, boolean to hází false. WIERD?
                /*if (!ftpClient.changeWorkingDirectory(defaultDirectory)) {
                    throw IOException("Failed to access directory.")
                }*/
                // Upload file
                val cesta = localFilePath + "/" + remoteFileName
                val localFile = File(cesta)
                //Log.d("FTP", "Připraveno pro poslání")
                //kod = ftpClient.getReplyCode();
                //Tady seznam od chatGPT ohledně kódů
                if (FTPReply.isPositiveCompletion(kod)) {
                    FileInputStream(localFile).use { inputStream ->
                        ftpClient.storeFile(remoteFileName, inputStream)
                    }
                    status = true
                } else {
                    chyba = when (kod) {
                        530 -> "EN: Login incorrect: Please check your username and password.\nCZ: Neúspěšné přihlášení, zkontrolujte správnost jména a hesla"
                        500 -> "Syntax error: The FTP server encountered a syntax error in the command."
                        501 -> "Syntax error in parameters or arguments: Please verify your input."
                        502 -> "Command not implemented: The requested command is not supported by the server."
                        503 -> "Bad sequence of commands: The FTP client issued a command out of sequence."
                        504 -> "Command not implemented for that parameter: The provided command is not applicable in the current context."
                        550 -> "Action not taken: The requested action could not be completed."
                        551 -> "Requested action aborted: The FTP server aborted the requested action."
                        552 -> "Requested file action aborted: The FTP server aborted the requested file action."
                        553 -> "Requested action not taken: The FTP server could not complete the requested action."
                        421 -> "Service not available: The FTP server is not available."
                        425 -> "Can't open data connection: The FTP server encountered an issue while opening the data connection."
                        426 -> "Connection closed; transfer aborted: The FTP server closed the connection during the transfer."
                        451 -> "Requested action aborted: Local error in processing."
                        452 -> "Requested action not taken: Disk full or allocation exceeded."
                        //452 -> "Requested action not taken: The FTP server could not execute the requested action."
                        else -> "Unknown FTP server response code: $kod"
                    }
                }
                if (hardwareSend && status) {
                    val hardwareFileFind = localFilePath + "/" + "DHI_" + Build.MODEL + ".txt"
                    val hardwareFile = File(hardwareFileFind)
                    FileInputStream(hardwareFile).use { inputStream ->
                        ftpClient.storeFile("DHI_" + Build.MODEL + ".txt", inputStream)
                    }
                    Log.d("FTP", "Hardwarový soubor úspěšně nahrán")
                }
                // Logout and disconnect
                //println("Soubor nahrán")
                ftpClient.logout()
                ftpClient.disconnect()
                //println("Odpojeno")
            } catch (e: IOException) {
                ftpClient.disconnect()
                Log.d("FTP", "Odpojeno po erroru")
                chyba = e.message.toString()
            } finally {
                // Ensure the FTP client is disconnected in case of an exception
                if (ftpClient.isConnected) {
                    try {
                        ftpClient.disconnect()
                    } catch (e: IOException) {
                        //println("Error disconnecting FTP client: ${e.message}")
                        status = false
                        chyba = e.message.toString()
                        Log.w("Chyba", chyba)
                    }
                }
            }
            return Vysledek(status, chyba, kod)
        } else {
            kod = 2
            chyba = "CZ:Zařízení není připojeno k internetu, zkontrolujte internetové připojení"
            return Vysledek(status, chyba, kod)
        }
    }
    fun isConnectedToInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return (networkCapabilities != null) &&
                (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

}