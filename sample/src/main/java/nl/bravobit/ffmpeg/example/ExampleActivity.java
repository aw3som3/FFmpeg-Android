package nl.bravobit.ffmpeg.example;

import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;
import nl.bravobit.ffmpeg.FFprobe;
import nl.bravobit.ffmpeg.FFtask;
import timber.log.Timber;

/**
 * Created by Brian on 11-12-17.
 */
public class ExampleActivity extends AppCompatActivity {
    private static final int MOVIE_OPEN_REQUEST = 1;
    private Handler handler = new Handler();
    private String mediaFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FFmpeg.getInstance(this).isSupported()) {
            // ffmpeg is supported
            versionFFmpeg();
            //ffmpegTestTaskQuit();
        } else {
            // ffmpeg is not supported
            Timber.e("ffmpeg not supported!");
        }

        if (FFprobe.getInstance(this).isSupported()) {
            // ffprobe is supported
            versionFFprobe();
        } else {
            // ffprobe is not supported
            Timber.e("ffprobe not supported!");
        }

    }

    private void versionFFmpeg() {
        FFmpeg.getInstance(this).execute(new String[]{"-version"}, new ExecuteBinaryResponseHandler() {
            @Override
            public void onSuccess(String message) {
                Timber.d(message);
            }

            @Override
            public void onProgress(String message) {
                Timber.d(message);
            }
        });

    }

    private void versionFFprobe() {
        Timber.d("version ffprobe");
        FFprobe.getInstance(this).execute(new String[]{"-version"}, new ExecuteBinaryResponseHandler() {
            @Override
            public void onSuccess(String message) {
                Timber.d(message);
            }

            @Override
            public void onProgress(String message) {
                Timber.d(message);
            }
        });
    }

    private void ffmpegTestTaskQuit() {
        String[] command = {"-i", "input.mp4", "output.mov"};

        final FFtask task = FFmpeg.getInstance(this).execute(command, new ExecuteBinaryResponseHandler() {
            @Override
            public void onStart() {
                Timber.d( "on start");
            }

            @Override
            public void onFinish() {
                Timber.d("on finish");
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Timber.d("RESTART RENDERING");
                        ffmpegTestTaskQuit();
                    }
                }, 5000);
            }

            @Override
            public void onSuccess(String message) {
                Timber.d(message);
            }

            @Override
            public void onProgress(String message) {
                Timber.d(message);
            }

            @Override
            public void onFailure(String message) {
                Timber.d(message);
            }
        });

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Timber.d( "STOPPING THE RENDERING!");
                task.sendQuitSignal();
            }
        }, 8000);
    }
    
    @Override
    public void onResume() {
        super.onResume();

        if (mediaFile == null) {
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .setType("video/*")
                    .putExtra("android.content.extra.SHOW_ADVANCED", true) // thanks Commonsware, https://issuetracker.google.com/issues/72053350
                    .putExtra("android.content.extra.FANCY", true)
                    .putExtra("android.content.extra.SHOW_FILESIZE", true)
                    .addCategory(Intent.CATEGORY_OPENABLE), DOC_OPEN_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == MOVIE_OPEN_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
                int fd = parcelFileDescriptor.getFd();
                int pid = Process.myPid();
                mediaFile = "/proc/" + pid + "/fd/" + fd;
                Timber.i("opened " + uri + " as " + mediaFile);
                runFFmpeg();
            } catch (Throwable e) {
                Timber.e(e, "cannot work with " + uri);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    private void runFFmpeg() {
        String outPath = getExternalFilesDir(null) + "/out.mp4"
        new File(outPath).delete();
        FFmpeg.getInstance(this).execute(new String[]{"-i", mediaFile, "-frames:v", "500", outPath}, new ExecuteBinaryResponseHandler() {
            @Override
            public void onSuccess(String message) {
                Timber.e(message.substring(0, 100));
            }

            @Override
            public void onProgress(String message) {
                Timber.i(message);
            }

            @Override
            public void onFailure(String message) {
                Timber.e(message);
            }
        });
    }

}
