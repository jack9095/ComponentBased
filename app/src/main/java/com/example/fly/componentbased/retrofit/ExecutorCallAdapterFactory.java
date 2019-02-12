//package retrofit2;
//
//import java.io.IOException;
//import java.lang.annotation.Annotation;
//import java.lang.reflect.Type;
//import java.util.concurrent.Executor;
//import okhttp3.Request;
//
//import static retrofit2.Utils.checkNotNull;
//
///**
// * 默认创建的 CallAdapterFactory，当然也可以自定义RxJava的Observer<Object>，这里只说默认的
// */
//final class ExecutorCallAdapterFactory extends CallAdapter.Factory {
//    final Executor callbackExecutor;
//
//    ExecutorCallAdapterFactory(Executor callbackExecutor) {
//        this.callbackExecutor = callbackExecutor;
//    }
//
//    @Override
//    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
//        if (getRawType(returnType) != Call.class) {
//            return null;
//        }
//        final Type responseType = Utils.getCallResponseType(returnType);
//        return new CallAdapter<Object, Call<?>>() {
//            @Override public Type responseType() {
//                return responseType;
//            }
//
//            @Override public Call<Object> adapt(Call<Object> call) {
//                return new ExecutorCallbackCall<>(callbackExecutor, call);
//            }
//        };
//    }
//
//    static final class ExecutorCallbackCall<T> implements Call<T> {
//        final Executor callbackExecutor; // 回调的Executor (其实就是Android那个Platform中的MainThreadExecutor)
//        final Call<T> delegate;   // 代理Call 真正的执行Call
//
//        ExecutorCallbackCall(Executor callbackExecutor, Call<T> delegate) {
//            this.callbackExecutor = callbackExecutor;
//            this.delegate = delegate;
//        }
//
//        // 发送异步请求
//        @Override public void enqueue(final Callback<T> callback) {
//            checkNotNull(callback, "callback == null");
//
//            delegate.enqueue(new Callback<T>() {
//                @Override public void onResponse(Call<T> call, final Response<T> response) {
//                    // 发送响应到主线程
//                    callbackExecutor.execute(new Runnable() {
//                        @Override public void run() {
//                            if (delegate.isCanceled()) {
//                                // Emulate OkHttp's behavior of throwing/delivering an IOException on cancellation.
//                                callback.onFailure(ExecutorCallbackCall.this, new IOException("Canceled"));
//                            } else {
//                                callback.onResponse(ExecutorCallbackCall.this, response);
//                            }
//                        }
//                    });
//                }
//
//                @Override public void onFailure(Call<T> call, final Throwable t) {
//                    callbackExecutor.execute(new Runnable() {
//                        @Override public void run() {
//                            callback.onFailure(ExecutorCallbackCall.this, t);
//                        }
//                    });
//                }
//            });
//        }
//
//        // 判断该Call是否已经执行
//        @Override public boolean isExecuted() {
//            return delegate.isExecuted();
//        }
//
//        // 发送同步请求
//        @Override public Response<T> execute() throws IOException {
//            return delegate.execute();
//        }
//
//        // 取消该call 就是在okHttp的call池中移除
//        @Override public void cancel() {
//            delegate.cancel();
//        }
//
//        // 判断该Call是否已经取消掉了
//        @Override public boolean isCanceled() {
//            return delegate.isCanceled();
//        }
//
//        @SuppressWarnings("CloneDoesntCallSuperClone") // Performing deep clone.
//        @Override public Call<T> clone() {
//            return new ExecutorCallbackCall<>(callbackExecutor, delegate.clone());
//        }
//
//        @Override public Request request() {
//            return delegate.request();
//        }
//    }
//}
