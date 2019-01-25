package retrofit2;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.Url;

import static java.util.Collections.unmodifiableList;
import static retrofit2.Utils.checkNotNull;

/**
 * Retrofit源码解读
 */
public final class Retrofit {
    /**
     * Method是http的请求方法，ServiceMethod 代表网络请求接口中，对方法注解后，我们要通过解析，解析
     * 之后的对象就是这里的ServiceMethod，和注解中的请求方法（GET、POST）成对出现的，一一对应的
     * serviceMethodCache用于缓存，请求方法、配置、转换器、适配器等等
     */
    private final Map<Method, ServiceMethod<?, ?>> serviceMethodCache = new ConcurrentHashMap<>();

    final okhttp3.Call.Factory callFactory;  // 请求网络okhttp的工厂
    final HttpUrl baseUrl;   // 网络请求url的基地址
    final List<Converter.Factory> converterFactories; // 数据转换器工厂集合,就是把服务端返回的json转换成我们对应的JavaBean，这是个放置数据转换器的工厂
    final List<CallAdapter.Factory> callAdapterFactories; // 网络请求适配器工厂集合 ，就是把call对象转换成其他类型，比如转换成RxJava的被观擦者
    final @Nullable
    Executor callbackExecutor; // 用于执行回调的
    final boolean validateEagerly; // 表示标志位的，表示我们是否立即解析接口中的方法,动态代理解析接口中的方法的时候用到的

    Retrofit(okhttp3.Call.Factory callFactory, HttpUrl baseUrl,
             List<Converter.Factory> converterFactories, List<CallAdapter.Factory> callAdapterFactories,
             @Nullable Executor callbackExecutor, boolean validateEagerly) {
        this.callFactory = callFactory;
        this.baseUrl = baseUrl;
        this.converterFactories = converterFactories; // Copy+unmodifiable at call site.
        this.callAdapterFactories = callAdapterFactories; // Copy+unmodifiable at call site.
        this.callbackExecutor = callbackExecutor;
        this.validateEagerly = validateEagerly;
    }

    /**
     * @param service 构建API的生产接口
     * @return 返回 构建API的生产接口
     */
    @SuppressWarnings("unchecked") // Single-interface proxy creation guarded by parameter safety.
    public <T> T create(final Class<T> service) {
        Utils.validateServiceInterface(service); // 验证该接口的合法性
        if (validateEagerly) {
            eagerlyValidateMethods(service); // 立即（验证)）解析接口中的方法
        }
        // 通过动态代理将构建API的生产接口的注解翻译成一个个http请求，再由线程池来执行这一个个的网络请求
        return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service},
                new InvocationHandler() {
                    private final Platform platform = Platform.get();

                    @Override
                    public Object invoke(Object proxy, Method method, @Nullable Object[] args)
                            throws Throwable {
                        // If the method is a method from Object then defer to normal invocation.
                        if (method.getDeclaringClass() == Object.class) {
                            return method.invoke(this, args); // 如果该方法来自对象，由该实例正常调用
                        }
                        if (platform.isDefaultMethod(method)) { //如果该方法是声明为default，则正常调用该方法
                            return platform.invokeDefaultMethod(method, service, proxy, args);
                        }
                        // 从缓存ServiceMethod的方法的集合中根据Method获取该ServiceMethod实例对象
                        ServiceMethod<Object, Object> serviceMethod =
                                (ServiceMethod<Object, Object>) loadServiceMethod(method);
                        OkHttpCall<Object> okHttpCall = new OkHttpCall<>(serviceMethod, args); // 创建一个请求执行对象，就是okHttp中的Call
                        return serviceMethod.adapt(okHttpCall);  // 网络请求适配器的使用以及异步执行器的回调
                    }
                });
    }

    /**
     * 会在Retrofit第一次cretae的时候调用
     *
     * 立即（验证)）解析接口中的方法
     * @param service  构建API的生产接口
     */
    private void eagerlyValidateMethods(Class<?> service) {
        Platform platform = Platform.get();
        for (Method method : service.getDeclaredMethods()) { // 利用反射获取生产接口中的所有方法（私有，默认、公有)）
            if (!platform.isDefaultMethod(method)) { //判断是不是default方法，jdk7以后接口中可以编写方法的实现，但是必须在方法前面设置一个关键字default
                loadServiceMethod(method);
            }
        }
    }

    /**
     * 加载接口中的非default方法，构建一个ServiceMethod对象，以Method为Key，该对象作为value添加到serviceMethodCache集合缓存起来
     */
    ServiceMethod<?, ?> loadServiceMethod(Method method) {
        ServiceMethod<?, ?> result = serviceMethodCache.get(method);
        if (result != null) return result;

        synchronized (serviceMethodCache) {
            result = serviceMethodCache.get(method);
            if (result == null) {
                // 传入Retrofit实例和Method到MethodBuild对象构建一个ServiceMethod，该对象将会包含Retrofit所有数据，以后就是用做对象完成数据请求以及封装
                result = new ServiceMethod.Builder<>(this, method).build();
                serviceMethodCache.put(method, result);
            }
        }
        return result;
    }

    /**
     * 返回网络请求的工厂
     */
    public okhttp3.Call.Factory callFactory() {
        return callFactory;
    }

    /**
     * 返回baseUrl所在的HttpUrl
     */
    public HttpUrl baseUrl() {
        return baseUrl;
    }

    /**
     * 获取网络请求适配器的工厂集合
     */
    public List<CallAdapter.Factory> callAdapterFactories() {
        return callAdapterFactories;
    }

    /**
     * 网络请求适配器
     */
    public CallAdapter<?, ?> callAdapter(Type returnType, Annotation[] annotations) {
        return nextCallAdapter(null, returnType, annotations);
    }

    /**
     * 网络请求适配器 为上面的方法服务的
     */
    public CallAdapter<?, ?> nextCallAdapter(@Nullable CallAdapter.Factory skipPast, Type returnType,
                                             Annotation[] annotations) {
        checkNotNull(returnType, "returnType == null");
        checkNotNull(annotations, "annotations == null");

        int start = callAdapterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = callAdapterFactories.size(); i < count; i++) {
            CallAdapter<?, ?> adapter = callAdapterFactories.get(i).get(returnType, annotations, this);
            if (adapter != null) {
                return adapter;
            }
        }

        StringBuilder builder = new StringBuilder("Could not locate call adapter for ")
                .append(returnType)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(callAdapterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = callAdapterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(callAdapterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * 获取数据解析器的工厂集合
     */
    public List<Converter.Factory> converterFactories() {
        return converterFactories;
    }

    /**
     * RequestBody 转换器
     */
    public <T> Converter<T, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations) {
        return nextRequestBodyConverter(null, type, parameterAnnotations, methodAnnotations);
    }

    /**
     * RequestBody 转换器 为上面的方法服务
     */
    public <T> Converter<T, RequestBody> nextRequestBodyConverter(@Nullable Converter.Factory skipPast, Type type, Annotation[] parameterAnnotations,
                                                                  Annotation[] methodAnnotations) {
        checkNotNull(type, "type == null");
        checkNotNull(parameterAnnotations, "parameterAnnotations == null");
        checkNotNull(methodAnnotations, "methodAnnotations == null");

        int start = converterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            Converter.Factory factory = converterFactories.get(i);
            Converter<?, RequestBody> converter =
                    factory.requestBodyConverter(type, parameterAnnotations, methodAnnotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<T, RequestBody>) converter;
            }
        }

        StringBuilder builder = new StringBuilder("Could not locate RequestBody converter for ")
                .append(type)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * ResponseBody的转换器
     */
    public <T> Converter<ResponseBody, T> responseBodyConverter(Type type, Annotation[] annotations) {
        return nextResponseBodyConverter(null, type, annotations);
    }

    /**
     * ResponseBody的转换器，为上面的方法服务
     */
    public <T> Converter<ResponseBody, T> nextResponseBodyConverter(@Nullable Converter.Factory skipPast, Type type, Annotation[] annotations) {
        checkNotNull(type, "type == null");
        checkNotNull(annotations, "annotations == null");

        int start = converterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            Converter<ResponseBody, ?> converter =
                    converterFactories.get(i).responseBodyConverter(type, annotations, this);
            if (converter != null) {
                //noinspection unchecked 不检查
                return (Converter<ResponseBody, T>) converter;
            }
        }

        StringBuilder builder = new StringBuilder("Could not locate ResponseBody converter for ")
                .append(type)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * String类型的转换器
     */
    public <T> Converter<T, String> stringConverter(Type type, Annotation[] annotations) {
        checkNotNull(type, "type == null");
        checkNotNull(annotations, "annotations == null");

        for (int i = 0, count = converterFactories.size(); i < count; i++) {
            Converter<?, String> converter =
                    converterFactories.get(i).stringConverter(type, annotations, this);
            if (converter != null) {
                //不检查 （免检）
                return (Converter<T, String>) converter;
            }
        }

        // 没有匹配的，默认使用 ToStringConverter 默认转换器
        return (Converter<T, String>) BuiltInConverters.ToStringConverter.INSTANCE;
    }

    /**
     * 异步回调的执行方法. 如果为空，在这种情况下，应该在后台线程上同步进行回调
     */
    public @Nullable
    Executor callbackExecutor() {
        return callbackExecutor;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    /**
     * 内部类 Builder
     */
    public static final class Builder {
        private final Platform platform;  // 平台  android  java8，默认用的都是android平台
        private @Nullable
        okhttp3.Call.Factory callFactory;  // 请求网络的工厂，默认是okHttp的
        private HttpUrl baseUrl; // 网络请求的基地址，传进来的是String，这里要把String转换成HttpUrl
        private final List<Converter.Factory> converterFactories = new ArrayList<>(); // 数据转换器工厂
        private final List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>(); // 网络请求适配器工厂
        private @Nullable
        Executor callbackExecutor; // 执行异步回调
        private boolean validateEagerly; // 标志，是否立即解析接口中的方法，动态代理解析接口中的方法的时候用到的

        Builder(Platform platform) {
            this.platform = platform;
        }

        public Builder() {
            this(Platform.get());  // 默认安卓平台，里面有默认网络请求适配器call方法
        }

        Builder(Retrofit retrofit) {
            platform = Platform.get();
            callFactory = retrofit.callFactory;
            baseUrl = retrofit.baseUrl;

            converterFactories.addAll(retrofit.converterFactories); // 添加设置的数据转换器
            // Remove the default BuiltInConverters instance added by build().
            converterFactories.remove(0); // 移除默认的数据转换器

            callAdapterFactories.addAll(retrofit.callAdapterFactories); // 添加适配器工厂
            // Remove the default, platform-aware call adapter added by build().
            callAdapterFactories.remove(callAdapterFactories.size() - 1); // 移除默认的适配器

            callbackExecutor = retrofit.callbackExecutor;
            validateEagerly = retrofit.validateEagerly;
        }

        /**
         * 定制客户端的请求，可定制OkHttpClient 比如添加相应的拦截器等
         */
        public Builder client(OkHttpClient client) {
            return callFactory(checkNotNull(client, "client == null"));
        }

        /**
         * 自定义网络请求执行工厂，默认的是 okHttp
         */
        public Builder callFactory(okhttp3.Call.Factory factory) {
            this.callFactory = checkNotNull(factory, "factory == null");
            return this;
        }

        /**
         * 设置http的主机名(域名) + 端口号 *以/结尾
         */
        public Builder baseUrl(String baseUrl) {
            checkNotNull(baseUrl, "baseUrl == null"); // 先判断 baseUrl是否为空
            HttpUrl httpUrl = HttpUrl.parse(baseUrl); // 把baseUrl转换成HttpUrl
            if (httpUrl == null) {
                throw new IllegalArgumentException("Illegal URL: " + baseUrl);
            }
            return baseUrl(httpUrl);
        }

        /**
         * 承接上面的方法，解析处理 baseUrl
         */
        public Builder baseUrl(HttpUrl baseUrl) {
            checkNotNull(baseUrl, "baseUrl == null");
            List<String> pathSegments = baseUrl.pathSegments(); // 拆成集合碎片
            if (!"".equals(pathSegments.get(pathSegments.size() - 1))) {
                throw new IllegalArgumentException("baseUrl must end in /: " + baseUrl); // baseUrl必须用“/”结尾
            }
            this.baseUrl = baseUrl; // 赋值给内部类的成员变量
            return this;
        }

        /**
         * 为对象的序列化和反序列化添加数据转换器工厂.
         */
        public Builder addConverterFactory(Converter.Factory factory) {
            converterFactories.add(checkNotNull(factory, "factory == null"));
            return this;
        }

        /**
         * 添加调用适配器工厂 RxJava 的观察者 observe 默认是okHttp的call
         */
        public Builder addCallAdapterFactory(CallAdapter.Factory factory) {
            callAdapterFactories.add(checkNotNull(factory, "factory == null"));
            return this;
        }

        /**
         * 执行异步回调
         */
        public Builder callbackExecutor(Executor executor) {
            this.callbackExecutor = checkNotNull(executor, "executor == null");
            return this;
        }

        /**
         * 返回可修改的调用适配器工厂列表.
         */
        public List<CallAdapter.Factory> callAdapterFactories() {
            return this.callAdapterFactories;
        }

        /**
         * 返回可修改的（数据）转换器工厂列表.
         */
        public List<Converter.Factory> converterFactories() {
            return this.converterFactories;
        }

        /**
         * 设置标志，是否立即解析接口中的方法，动态代理解析接口中的方法的时候用到的
         */
        public Builder validateEagerly(boolean validateEagerly) {
            this.validateEagerly = validateEagerly;
            return this;
        }

        /**
         * 根据上面所配置的值创建实例，如果没有自定义配置网络请求，默认创建并使用okHttp请求
         */
        public Retrofit build() {
            if (baseUrl == null) {
                throw new IllegalStateException("Base URL required.");
            }

            okhttp3.Call.Factory callFactory = this.callFactory;
            if (callFactory == null) {
                callFactory = new OkHttpClient();
            }

            Executor callbackExecutor = this.callbackExecutor;
            if (callbackExecutor == null) {
                callbackExecutor = platform.defaultCallbackExecutor();
            }

            // 创建并复制网络请求适配器工厂，并添加默认的网络请求适配器
            List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>(this.callAdapterFactories);
            callAdapterFactories.add(platform.defaultCallAdapterFactory(callbackExecutor));

            // 创建并复制数据解析器工厂
            List<Converter.Factory> converterFactories = new ArrayList<>(1 + this.converterFactories.size());

            // 首先添加内置转换器工厂。这可以防止覆盖它，确保在使用是是正确的
            converterFactories.add(new BuiltInConverters()); // 添加默认的数据转换器工厂
            converterFactories.addAll(this.converterFactories); // 添加用户自定义设置进来的数据转换工厂

            // https://blog.csdn.net/cilen/article/details/7744969 题外说一下unmodifiableList用法
            return new Retrofit(callFactory, baseUrl, unmodifiableList(converterFactories),
                    unmodifiableList(callAdapterFactories), callbackExecutor, validateEagerly);
        }
    }
}