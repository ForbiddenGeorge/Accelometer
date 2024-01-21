import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter

class Writer(private val context: Context) {
    private var writer: FileWriter? = null

    //Získání složky pro uložení
    private fun getFolder(): File {
        return context.getExternalFilesDir(null)!!
    }

    //Získání subsložky pro uložení
    fun getAppSubdirectory(): File {
        val folder = getFolder()
        return File(folder, "Mereni").apply { mkdirs() }
    }

    //Vytvoření souboru
    fun createFile(fileName: String) {
        //val folder = context.filesDir

        Log.d("File destination:", getAppSubdirectory().toString())
        val file = File(getAppSubdirectory(), fileName)
        if (file.exists()) {
            Log.d("File already exists", "File with name $fileName already exists.")
            writer?.close()
            return
        }
        writer = FileWriter(file)
    }

    //Psaní dat z měření do souboru
    fun writeData(data: Array<String>) {
        writer?.write(data.joinToString(","))
        writer?.write("\n")
    }

    //Psaní dat o senzorech do souboru
    fun writeHardwareData(data: Array<String>) {
        writer?.write(data.joinToString(" ") )
        writer?.write("\n")
    }

    //Uzavření souboru
    fun closeFile() {
        writer?.close()
        Log.d("File closed", "Soubor closed and saved")
    }
}
