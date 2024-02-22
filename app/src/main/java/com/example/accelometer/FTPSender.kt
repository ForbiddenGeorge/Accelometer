
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
class FTPSender {

    private lateinit var host: String
    private lateinit var port: String
    private lateinit var username: String
    private lateinit var password: String
    private lateinit var defaultDirectory: String
    private lateinit var localFilePath: String
    private lateinit var remoteFileName: String
    private var hardwareSend: Boolean = false
    private lateinit var context: Context
    private var status: String = ""
    private val mainHandler = Handler(Looper.getMainLooper())
    fun init(
        context: Context,
        localFilePath: String,
        remoteFileName: String,
        hardwareSend: Boolean
    ) {
        this.context = context
        val sharedPreferences = context.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        host = sharedPreferences.getString("host", null).toString()
        username = sharedPreferences.getString("username", null).toString()
        password = sharedPreferences.getString("password", null).toString()
        defaultDirectory = sharedPreferences.getString("directory", null).toString()
        port = sharedPreferences.getString("port", null).toString()
        this.localFilePath = localFilePath
        this.remoteFileName = remoteFileName
        this.hardwareSend = hardwareSend
        Log.d("FTPSender", "Local File Path: $localFilePath")
    }

    fun uploadFileToFTPAsync(): String {
        val latch = CountDownLatch(1)
        var status = ""

        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            status = uploadFileToFTP()
            latch.countDown()
        }

        try {
            // Wait for the latch to count down to zero
            latch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        Log.d("Status in FTP main thread", status)
        return status
    }

    private fun uploadFileToFTP(): String {
        val ftpClient = FTPClient()
        status = ""
        try {
            // Connect to FTP server
            ftpClient.connect(host, port.toInt())
            ftpClient.login(username, password)
            Log.d("FTP","Přihlášeno a připojeno")
            // Set transfer mode and type
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
            ftpClient.enterLocalPassiveMode()
            // Change working directory if needed
            ftpClient.changeWorkingDirectory(defaultDirectory)
            Log.d("FTP","Nastaveno")
            // Upload file
            val incantate = localFilePath + "/" + remoteFileName
            println(incantate)
            println(defaultDirectory)
            val localFile = File(incantate)
            Log.d("FTP", "Připraveno pro poslání")

            FileInputStream(localFile).use { inputStream ->
                ftpClient.storeFile(remoteFileName, inputStream)
            }
            if (hardwareSend) {
                val hardwareFileFind = localFilePath + "/" + "DHI_" + Build.MODEL + ".txt"
                val hardwareFile = File(hardwareFileFind)
                FileInputStream(hardwareFile).use { inputStream ->
                    ftpClient.storeFile("DHI_" + Build.MODEL + ".txt", inputStream)
                }
                Log.d("FTP","Hardwarový soubor úspěšně nahrán")
            }

            // Logout and disconnect
            println("Soubor nahrán")
            ftpClient.logout()
            ftpClient.disconnect()
            println("Odpojeno")

        } catch (e: IOException) {
            println("Error during FTP upload: ${e.message}")
            ftpClient.disconnect()
            Log.d("FTP", "Odpojeno po erroru")
            status = e.message.toString()
        } finally {
            // Ensure the FTP client is disconnected in case of an exception
            if (ftpClient.isConnected) {
                try {
                    ftpClient.disconnect()
                } catch (e: IOException) {
                    println("Error disconnecting FTP client: ${e.message}")
                    status = e.message.toString()
                }
            }
        }
        Log.d("Status v FTP", status)
        return status
    }
}