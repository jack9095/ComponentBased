package eventbus;

import android.os.Looper;

/**
 * Interface to the "main" thread, which can be whatever you like. Typically on Android, Android's main thread is used.
 * 接口到“主”线程，可以是任何你喜欢的。通常在Android上使用Android的主线程。
 */
public interface MainThreadSupport {

    boolean isMainThread();

    Poster createPoster(EventBus eventBus);

    class AndroidHandlerMainThreadSupport implements MainThreadSupport {

        private final Looper looper;

        public AndroidHandlerMainThreadSupport(Looper looper) {
            this.looper = looper;
        }

        @Override
        public boolean isMainThread() {
            return looper == Looper.myLooper();
        }

        @Override
        public Poster createPoster(EventBus eventBus) {
            return new HandlerPoster(eventBus, looper, 10);
        }
    }

}
