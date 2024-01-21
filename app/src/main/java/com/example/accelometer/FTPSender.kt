import android.content.Context
import android.os.Looper
import android.util.Log
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.Executors
import android.os.Handler
import android.widget.Toast

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
    private val mainHandler = Handler(Looper.getMainLooper())
    fun init(
        context: Context,
        localFilePath: String,
        remoteFileName: String,
        hardwareSend: Boolean,
        defaultPath: String
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
        this.defaultDirectory = defaultPath
        Log.d("FTPSender", "Local File Path: $localFilePath")

    }

    fun uploadFileToFTPAsync() {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            uploadFileToFTP()
        }
    }

    private fun uploadFileToFTP() {
        val ftpClient = FTPClient()
        try {
            // Connect to FTP server
            ftpClient.connect(host, port.toInt())
            ftpClient.login(username, password)
            Log.d("FTP","Přihlášeno a připojeno")
            // Set transfer mode and type
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
            ftpClient.enterLocalPassiveMode()
            Log.d("FTP","Nastaveno")
            // Change working directory if needed
            //ftpClient.changeWorkingDirectory(defaultDirectory)

            // Upload file
            val incantate = localFilePath +"/" + remoteFileName
            val localFile = File(incantate)
            Log.d("FTP", "Připraveno pro poslání")

            FileInputStream(localFile).use { inputStream ->
                ftpClient.storeFile(remoteFileName, inputStream)

                if (hardwareSend) {
                    ftpClient.storeFile("Device_Hardware_Information.txt", inputStream)
                    Log.d("FTP","Hardwarový soubor úspěšně nahrán")
                }
            }

            // Logout and disconnect
            ftpClient.logout()
            ftpClient.disconnect()
            println("Soubor nahrán")

        } catch (e: IOException) {
            println("Error during FTP upload: ${e.message}")
            ftpClient.disconnect()
            Log.d("FTP", "Odpojeno po erroru")
        } finally {
            // Ensure the FTP client is disconnected in case of an exception
            if (ftpClient.isConnected) {
                try {
                    ftpClient.disconnect()
                } catch (e: IOException) {
                    println("Error disconnecting FTP client: ${e.message}")
                }
            }
        }
    }
}