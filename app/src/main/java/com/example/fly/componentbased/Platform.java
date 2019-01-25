package retrofit2;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

class Platform {
    private static final Platform PLATFORM = findPlatform();

    static Platform get() {
        return PLATFORM;
    }

    private static Platform findPlatform() {
        try {
            Class.forName("android.os.Build");
            if (Build.VERSION.SDK_INT != 0) {
                return new Android();  // 安卓平台
            }
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("java.util.Optional");
            return new Java8();  // Java8平台
        } catch (ClassNotFoundException ignored) {
        }
        return new Platform();
    }

    @Nullable Executor defaultCallbackExecutor() {
        return null;
    }

    CallAdapter.Factory defaultCallAdapterFactory(@Nullable Executor callbackExecutor) {
        if (callbackExecutor != null) {
            return new ExecutorCallAdapterFactory(callbackExecutor); // 创建默认的CallAdapterFactory  ---> ExecutorCallAdapterFactory
        }
        return DefaultCallAdapterFactory.INSTANCE;
    }

    boolean isDefaultMethod(Method method) {
        return false;
    }

    @Nullable Object invokeDefaultMethod(Method method, Class<?> declaringClass, Object object,
                                         @Nullable Object... args) throws Throwable {
        throw new UnsupportedOperationException();
    }

    @IgnoreJRERequirement // Only classloaded and used on Java 8.
    static class Java8 extends Platform {
        @Override boolean isDefaultMethod(Method method) {
            return method.isDefault();
        }

        @Override Object invokeDefaultMethod(Method method, Class<?> declaringClass, Object object,
                                             @Nullable Object... args) throws Throwable {
            // Because the service interface might not be public, we need to use a MethodHandle lookup
            // that ignores the visibility of the declaringClass.
            Constructor<Lookup> constructor = Lookup.class.getDeclaredConstructor(Class.class, int.class);
            constructor.setAccessible(true);
            return constructor.newInstance(declaringClass, -1 /* trusted */)
                    .unreflectSpecial(method, declaringClass)
                    .bindTo(object)
                    .invokeWithArguments(args);
        }
    }

    static class Android extends Platform {
        @Override public Executor defaultCallbackExecutor() {
            // 持有有个主线程的Handler，发送响应是事件到主线程
            return new MainThreadExecutor(); // 构建一个用来将响应事件发送到Main线程
        }

        @Override CallAdapter.Factory defaultCallAdapterFactory(@Nullable Executor callbackExecutor) {
            if (callbackExecutor == null) throw new AssertionError();
            // 返回一个默认的Call（执行器）的适配器工厂，就是用来将执行结果转换成你想要的返回对象，比如，你想要Observer<Object> 等
            return new ExecutorCallAdapterFactory(callbackExecutor);
        }

        static class MainThreadExecutor implements Executor {
            private final Handler handler = new Handler(Looper.getMainLooper());

            @Override public void execute(Runnable r) {
                handler.post(r);
            }
        }
    }
}
