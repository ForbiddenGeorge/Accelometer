import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.accelometer.R

class ForegroundService: Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action){
            Actions.START.toString() -> start()
            Actions.STOP.toString() -> stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start(){
        val notification = NotificationCompat.Builder(this,"Running_service")
            .setSmallIcon(R.drawable.combined_logo)
            .setContentTitle("Akcelerometr")
            .setContentText("Probíhá měření")
            .build()
        startForeground(1, notification )
    }

    enum class Actions{
        START, STOP
    }
}