package org.greenrobot.eventbus;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

/**
 * EventBus is a central publish/subscribe event system for Android. Events are posted ({@link #post(Object)}) to the
 * bus, which delivers it to subscribers that have a matching handler method for the event type. To receive events,
 * subscribers must register themselves to the bus using {@link #register(Object)}. Once registered, subscribers
 * receive events until {@link #unregister(Object)} is called. Event handling methods must be annotated by
 * {@link Subscribe}, must be public, return nothing (void), and have exactly one parameter
 * (the event).
 *
 * @author Markus Junginger, greenrobot
 *
 * https://blog.csdn.net/wbst5/article/details/81089710
 *
 * EventBus 不仅仅获取当前类的订阅方法，还会获取它所有父类的订阅方法
 *
 * 在 EventBus 中，一个订阅者包括这个订阅者的所有父类和子类，不会有多个方法相同的去接收同一个事件。
 *
 * 但是有可能出现这样一种情况，子类去订阅了该事件，父类也去订阅了该事件。
 * 当出现这种情况，EventBus 如何判断？通过调用 checkAddWithMethodSignature() 方法，根据方法签名来检查
 *
 */
public class EventBus {

    /**
     * Log tag, apps may override it.
     */
    public static String TAG = "EventBus";

    static volatile EventBus defaultInstance; // 单例采用 volatile 修饰符，会降低性能，但能保证EventBus每次取值都是从主内存中读取

    private static final EventBusBuilder DEFAULT_BUILDER = new EventBusBuilder();
    private static final Map<Class<?>, List<Class<?>>> eventTypesCache = new HashMap<>();

    private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType;
    private final Map<Object, List<Class<?>>> typesBySubscriber;
    private final Map<Class<?>, Object> stickyEvents;

    // currentPostingThreadState
    private final ThreadLocal<PostingThreadState> currentPostingThreadState = new ThreadLocal<PostingThreadState>() {
        @Override
        protected PostingThreadState initialValue() {
            return new PostingThreadState();
        }
    };

    // @Nullable
    private final MainThreadSupport mainThreadSupport;
    // @Nullable  主线程中的 poster
    private final Poster mainThreadPoster;

    // 后台线程中的 poster
    private final BackgroundPoster backgroundPoster;

    // 异步线程中的 poster
    private final AsyncPoster asyncPoster;

    private final SubscriberMethodFinder subscriberMethodFinder;

    private final ExecutorService executorService;

    private final boolean throwSubscriberException;
    private final boolean logSubscriberExceptions;
    private final boolean logNoSubscriberMessages;
    private final boolean sendSubscriberExceptionEvent;
    private final boolean sendNoSubscriberEvent;
    private final boolean eventInheritance;

    private final int indexCount;
    private final Logger logger;

    /**
     * Convenience singleton for apps using a process-wide EventBus instance.
     * 双重检查
     */
    public static EventBus getDefault() {
        if (defaultInstance == null) {
            synchronized (EventBus.class) {
                if (defaultInstance == null) {
                    defaultInstance = new EventBus();
                }
            }
        }
        return defaultInstance;
    }

    public static EventBusBuilder builder() {
        return new EventBusBuilder();
    }

    /**
     * For unit test primarily.
     */
    public static void clearCaches() {
        SubscriberMethodFinder.clearCaches();
        eventTypesCache.clear();
    }

    /**
     * Creates a new EventBus instance; each instance is a separate scope in which events are delivered. To use a
     * central bus, consider {@link #getDefault()}.
     * 创建一个新的 EventBus 实例，每个实例在 events 事件被发送的时候都是一个单独的领域，为了使用一个 事件总线，考虑用 getDefault() 构建。
     */
    public EventBus() {
        this(DEFAULT_BUILDER);
    }

    EventBus(EventBusBuilder builder) {
        logger = builder.getLogger();
        subscriptionsByEventType = new HashMap<>();
        typesBySubscriber = new HashMap<>();
        stickyEvents = new ConcurrentHashMap<>();

        // 用于线程间调度
        mainThreadSupport = builder.getMainThreadSupport();
        mainThreadPoster = mainThreadSupport != null ? mainThreadSupport.createPoster(this) : null;
        backgroundPoster = new BackgroundPoster(this);
        asyncPoster = new AsyncPoster(this);

        // 用于记录event生成索引
        indexCount = builder.subscriberInfoIndexes != null ? builder.subscriberInfoIndexes.size() : 0;

        // 对已经注解过的Method的查找器，会对所设定过 @Subscriber 注解的的方法查找相应的Event
        subscriberMethodFinder = new SubscriberMethodFinder(builder.subscriberInfoIndexes,
                builder.strictMethodVerification, builder.ignoreGeneratedIndex);

        // 当调用事件处理函数发生异常是否需要打印Log
        logSubscriberExceptions = builder.logSubscriberExceptions;

        // 当没有订阅者订阅这个消息的时候是否打印Log
        logNoSubscriberMessages = builder.logNoSubscriberMessages;

        // 当调用事件处理函数，如果异常，是否需要发送Subscriber这个事件
        sendSubscriberExceptionEvent = builder.sendSubscriberExceptionEvent;

        // 当没有事件处理函数时，对事件处理是否需要发送sendNoSubscriberEvent这个标志
        sendNoSubscriberEvent = builder.sendNoSubscriberEvent;

        // 是否需要抛出SubscriberException
        throwSubscriberException = builder.throwSubscriberException;

        // 与Event有继承关系的类是否都需要发送
        eventInheritance = builder.eventInheritance;

        // 线程池 Executors.newCachedThreadPool()
        executorService = builder.executorService;
    }

    /**
     * Registers the given subscriber to receive events. Subscribers must call {@link #unregister(Object)} once they
     * are no longer interested in receiving events.
     * <p/>
     * Subscribers have event handling methods that must be annotated by {@link Subscribe}.
     * The {@link Subscribe} annotation also allows configuration like {@link
     * ThreadMode} and priority.
     *
     * 传进来的是订阅者 subscriber
     *
     */
    public void register(Object subscriber) {
        // 通过反射获取到订阅者的对象
        Class<?> subscriberClass = subscriber.getClass();

        // 通过Class对象找到对应的订阅者方法集合
        List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);

        // 遍历订阅者方法集合，将订阅者和订阅者放方法想成订阅关系
        synchronized (this) {
            // 迭代每个 Subscribe 方法，调用 subscribe() 传入 subscriber(订阅者) 和 subscriberMethod(订阅方法) 完成订阅，
            for (SubscriberMethod subscriberMethod : subscriberMethods) {
                subscribe(subscriber, subscriberMethod);
            }
        }
    }

    // Must be called in synchronized block  必须组同步块中调用 eventType 就是我们平时定义的 code
    private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
        Class<?> eventType = subscriberMethod.eventType;

        // 创建 Subscription 封装订阅者和订阅方法信息
        Subscription newSubscription = new Subscription(subscriber, subscriberMethod);

        // 可并发读写的 ArrayList（CopyOnWriteArrayList)），key为 EventType，value为 Subscriptions
        // 根据事件类型从 subscriptionsByEventType 这个 Map 中获取 Subscription 集合
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);

        // 如果为 null，表示还没有订阅过，创建并 put 进 Map
        if (subscriptions == null) {
            subscriptions = new CopyOnWriteArrayList<>();
            subscriptionsByEventType.put(eventType, subscriptions);
        } else {
            // 若subscriptions中已经包含newSubscription，表示该newSubscription已经被订阅过，抛出异常
            if (subscriptions.contains(newSubscription)) {
                throw new EventBusException("Subscriber " + subscriber.getClass() + " already registered to event "
                        + eventType);
            }
        }

        // 按照优先级插入subscriptions
        int size = subscriptions.size();
        for (int i = 0; i <= size; i++) {
            if (i == size || subscriberMethod.priority > subscriptions.get(i).subscriberMethod.priority) {
                subscriptions.add(i, newSubscription);
                break;
            }
        }

        // key为订阅者，value为eventType，用来存放订阅者中的事件类型
        List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
        if (subscribedEvents == null) {
            subscribedEvents = new ArrayList<>();
            typesBySubscriber.put(subscriber, subscribedEvents);
        }

        // 将EventType放入subscribedEvents的集合中
        subscribedEvents.add(eventType);

        //判断是否为Sticky事件
        if (subscriberMethod.sticky) {

            //判断是否设置了事件继承
            if (eventInheritance) {
                // Existing sticky events of all subclasses of eventType have to be considered.
                // Note: Iterating over all events may be inefficient with lots of sticky events,
                // thus data structure should be changed to allow a more efficient lookup
                // (e.g. an additional map storing sub classes of super classes: Class -> List<Class>).
                // 获取到所有Sticky事件的Set集合
                Set<Map.Entry<Class<?>, Object>> entries = stickyEvents.entrySet();

                //遍历所有Sticky事件
                for (Map.Entry<Class<?>, Object> entry : entries) {
                    Class<?> candidateEventType = entry.getKey();

                    //判断当前事件类型是否为黏性事件或者其子类
                    if (eventType.isAssignableFrom(candidateEventType)) {
                        Object stickyEvent = entry.getValue();

                        // 执行设置了 sticky 模式的订阅方法
                        checkPostStickyEventToSubscription(newSubscription, stickyEvent);
                    }
                }
            } else {
                Object stickyEvent = stickyEvents.get(eventType);
                checkPostStickyEventToSubscription(newSubscription, stickyEvent);
            }
        }
    }

    private void checkPostStickyEventToSubscription(Subscription newSubscription, Object stickyEvent) {
        if (stickyEvent != null) {
            // If the subscriber is trying to abort the event, it will fail (event is not tracked in posting state)
            // --> Strange corner case, which we don't take care of here.
            postToSubscription(newSubscription, stickyEvent, isMainThread());
        }
    }

    /**
     * Checks if the current thread is running in the main thread.
     * If there is no main thread support (e.g. non-Android), "true" is always returned. In that case MAIN thread
     * subscribers are always called in posting thread, and BACKGROUND subscribers are always called from a background
     * poster.
     */
    private boolean isMainThread() {
        return mainThreadSupport != null ? mainThreadSupport.isMainThread() : true;
    }

    public synchronized boolean isRegistered(Object subscriber) {
        return typesBySubscriber.containsKey(subscriber);
    }

    /**
     * Only updates subscriptionsByEventType, not typesBySubscriber! Caller must update typesBySubscriber.
     */
    private void unsubscribeByEventType(Object subscriber, Class<?> eventType) {
        List<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions != null) {
            int size = subscriptions.size();
            for (int i = 0; i < size; i++) {
                Subscription subscription = subscriptions.get(i);
                if (subscription.subscriber == subscriber) {
                    subscription.active = false;
                    subscriptions.remove(i);
                    i--;
                    size--;
                }
            }
        }
    }

    /**
     * Unregisters the given subscriber from all event classes.
     */
    public synchronized void unregister(Object subscriber) {
        List<Class<?>> subscribedTypes = typesBySubscriber.get(subscriber);
        if (subscribedTypes != null) {
            for (Class<?> eventType : subscribedTypes) {
                unsubscribeByEventType(subscriber, eventType);
            }
            typesBySubscriber.remove(subscriber);
        } else {
            logger.log(Level.WARNING, "Subscriber to unregister was not registered before: " + subscriber.getClass());
        }
    }

    /**
     * Posts the given event to the event bus.
     *
     * 将给定事件发布到事件总线
     */
    public void post(Object event) {

        // 获取当前线程的 posting 状态
        PostingThreadState postingState = currentPostingThreadState.get();

        // 获取当前事件队列
        List<Object> eventQueue = postingState.eventQueue;

        // 将事件添加进当前线程的事件队列
        eventQueue.add(event);

        // 判断是否正在posting
        if (!postingState.isPosting) {
            postingState.isMainThread = isMainThread();
            postingState.isPosting = true;

            // 如果已经取消，则抛出异常
            if (postingState.canceled) {
                throw new EventBusException("Internal error. Abort state was not reset");
            }
            try {
                while (!eventQueue.isEmpty()) {
                    // 发送事件
                    postSingleEvent(eventQueue.remove(0), postingState);
                }
            } finally {
                // 状态复原
                postingState.isPosting = false;
                postingState.isMainThread = false;
            }
        }
    }

    /**
     * Called from a subscriber's event handling method, further event delivery will be canceled. Subsequent
     * subscribers
     * won't receive the event. Events are usually canceled by higher priority subscribers (see
     * {@link Subscribe#priority()}). Canceling is restricted to event handling methods running in posting thread
     * {@link ThreadMode#POSTING}.
     */
    public void cancelEventDelivery(Object event) {
        PostingThreadState postingState = currentPostingThreadState.get();
        if (!postingState.isPosting) {
            throw new EventBusException(
                    "This method may only be called from inside event handling methods on the posting thread");
        } else if (event == null) {
            throw new EventBusException("Event may not be null");
        } else if (postingState.event != event) {
            throw new EventBusException("Only the currently handled event may be aborted");
        } else if (postingState.subscription.subscriberMethod.threadMode != ThreadMode.POSTING) {
            throw new EventBusException(" event handlers may only abort the incoming event");
        }

        postingState.canceled = true;
    }

    /**
     * Posts the given event to the event bus and holds on to the event (because it is sticky). The most recent sticky
     * event of an event's type is kept in memory for future access by subscribers using {@link Subscribe#sticky()}.
     */
    public void postSticky(Object event) {
        synchronized (stickyEvents) {
            stickyEvents.put(event.getClass(), event);
        }
        // Should be posted after it is putted, in case the subscriber wants to remove immediately
        post(event);
    }

    /**
     * Gets the most recent sticky event for the given type.
     *
     * @see #postSticky(Object)
     */
    public <T> T getStickyEvent(Class<T> eventType) {
        synchronized (stickyEvents) {
            return eventType.cast(stickyEvents.get(eventType));
        }
    }

    /**
     * Remove and gets the recent sticky event for the given event type.
     *
     * @see #postSticky(Object)
     */
    public <T> T removeStickyEvent(Class<T> eventType) {
        synchronized (stickyEvents) {
            return eventType.cast(stickyEvents.remove(eventType));
        }
    }

    /**
     * Removes the sticky event if it equals to the given event.
     *
     * @return true if the events matched and the sticky event was removed.
     */
    public boolean removeStickyEvent(Object event) {
        synchronized (stickyEvents) {
            Class<?> eventType = event.getClass();
            Object existingEvent = stickyEvents.get(eventType);
            if (event.equals(existingEvent)) {
                stickyEvents.remove(eventType);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Removes all sticky events.
     */
    public void removeAllStickyEvents() {
        synchronized (stickyEvents) {
            stickyEvents.clear();
        }
    }

    public boolean hasSubscriberForEvent(Class<?> eventClass) {
        List<Class<?>> eventTypes = lookupAllEventTypes(eventClass);
        if (eventTypes != null) {
            int countTypes = eventTypes.size();
            for (int h = 0; h < countTypes; h++) {
                Class<?> clazz = eventTypes.get(h);
                CopyOnWriteArrayList<Subscription> subscriptions;
                synchronized (this) {
                    subscriptions = subscriptionsByEventType.get(clazz);
                }
                if (subscriptions != null && !subscriptions.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 发送事件
     *
     * EventBus 用 ThreadLocal 存储每个线程的 PostingThreadState，一个存储了事件发布状态的类，
     * 当 post 一个事件时，添加到事件队列末尾，等待前面的事件发布完毕后再拿出来发布，
     * 这里看事件发布的关键代码postSingleEvent()
     *
     * @param event
     * @param postingState
     * @throws Error
     */
    private void postSingleEvent(Object event, PostingThreadState postingState) throws Error {
        Class<?> eventClass = event.getClass();
        boolean subscriptionFound = false;
        if (eventInheritance) {
            List<Class<?>> eventTypes = lookupAllEventTypes(eventClass);
            int countTypes = eventTypes.size();
            for (int h = 0; h < countTypes; h++) {
                Class<?> clazz = eventTypes.get(h);
                subscriptionFound |= postSingleEventForEventType(event, postingState, clazz);
            }
        } else {
            subscriptionFound = postSingleEventForEventType(event, postingState, eventClass);
        }
        if (!subscriptionFound) {
            if (logNoSubscriberMessages) {
                logger.log(Level.FINE, "No subscribers registered for event " + eventClass);
            }
            if (sendNoSubscriberEvent && eventClass != NoSubscriberEvent.class &&
                    eventClass != SubscriberExceptionEvent.class) {
                post(new NoSubscriberEvent(this, event));
            }
        }
    }

    private boolean postSingleEventForEventType(Object event, PostingThreadState postingState, Class<?> eventClass) {
        CopyOnWriteArrayList<Subscription> subscriptions;
        synchronized (this) {
            subscriptions = subscriptionsByEventType.get(eventClass);
        }
        if (subscriptions != null && !subscriptions.isEmpty()) {
            for (Subscription subscription : subscriptions) {
                postingState.event = event;
                postingState.subscription = subscription;
                boolean aborted = false;
                try {
                    postToSubscription(subscription, event, postingState.isMainThread);
                    aborted = postingState.canceled;
                } finally {
                    postingState.event = null;
                    postingState.subscription = null;
                    postingState.canceled = false;
                }
                if (aborted) {
                    break;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 订阅者五种线程模式的特点对应的就是以上代码，简单来讲就是订阅者指定了在哪个线程订阅事件，无论发布者在哪个线程，它都会将事件发布到订阅者指定的线程
     * @param subscription
     * @param event
     * @param isMainThread
     */
    private void postToSubscription(Subscription subscription, Object event, boolean isMainThread) {
        switch (subscription.subscriberMethod.threadMode) {

            // 订阅线程跟随发布线程，EventBus 默认的订阅方式
            case POSTING:
                invokeSubscriber(subscription, event);  // 订阅线程和发布线程相同，直接订阅
                break;

            // 订阅线程为主线程
            case MAIN:
                if (isMainThread) { // 如果 post是在 UI 线程，直接调用 invokeSubscriber
                    invokeSubscriber(subscription, event);
                } else {

                    // 如果不在 UI 线程，用 mainThreadPoster 进行调度，即上文讲述的 HandlerPoster 的 Handler 异步处理，将订阅线程切换到主线程订阅
                    mainThreadPoster.enqueue(subscription, event);
                }
                break;

            // 订阅线程为主线程
            case MAIN_ORDERED:
                if (mainThreadPoster != null) {
                    mainThreadPoster.enqueue(subscription, event);
                } else {
                    // temporary: technically not correct as poster not decoupled from subscriber
                    invokeSubscriber(subscription, event);
                }
                break;

            // 订阅线程为后台线程
            case BACKGROUND:

                // 如果在 UI 线程，则将 subscription 添加到后台线程的线程池
                if (isMainThread) {
                    backgroundPoster.enqueue(subscription, event);
                } else {
                    // 不在UI线程，直接分发
                    invokeSubscriber(subscription, event);
                }
                break;

            // 订阅线程为异步线程
            case ASYNC:
                // 使用线程池线程订阅
                asyncPoster.enqueue(subscription, event);
                break;
            default:
                throw new IllegalStateException("Unknown thread mode: " + subscription.subscriberMethod.threadMode);
        }
    }

    /**
     * Looks up all Class objects including super classes and interfaces. Should also work for interfaces.
     */
    private static List<Class<?>> lookupAllEventTypes(Class<?> eventClass) {
        synchronized (eventTypesCache) {
            List<Class<?>> eventTypes = eventTypesCache.get(eventClass);
            if (eventTypes == null) {
                eventTypes = new ArrayList<>();
                Class<?> clazz = eventClass;
                while (clazz != null) {
                    eventTypes.add(clazz);
                    addInterfaces(eventTypes, clazz.getInterfaces());
                    clazz = clazz.getSuperclass();
                }
                eventTypesCache.put(eventClass, eventTypes);
            }
            return eventTypes;
        }
    }

    /**
     * Recurses through super interfaces.
     */
    static void addInterfaces(List<Class<?>> eventTypes, Class<?>[] interfaces) {
        for (Class<?> interfaceClass : interfaces) {
            if (!eventTypes.contains(interfaceClass)) {
                eventTypes.add(interfaceClass);
                addInterfaces(eventTypes, interfaceClass.getInterfaces());
            }
        }
    }

    /**
     * Invokes the subscriber if the subscriptions is still active. Skipping subscriptions prevents race conditions
     * between {@link #unregister(Object)} and event delivery. Otherwise the event might be delivered after the
     * subscriber unregistered. This is particularly important for main thread delivery and registrations bound to the
     * live cycle of an Activity or Fragment.
     */
    void invokeSubscriber(PendingPost pendingPost) {
        Object event = pendingPost.event;
        Subscription subscription = pendingPost.subscription;

        // 进行释放 pendingPost
        PendingPost.releasePendingPost(pendingPost);
        if (subscription.active) {
            invokeSubscriber(subscription, event);
        }
    }

    void invokeSubscriber(Subscription subscription, Object event) {
        try {
            subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
        } catch (InvocationTargetException e) {
            handleSubscriberException(subscription, event, e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unexpected exception", e);
        }
    }

    private void handleSubscriberException(Subscription subscription, Object event, Throwable cause) {
        if (event instanceof SubscriberExceptionEvent) {
            if (logSubscriberExceptions) {
                // Don't send another SubscriberExceptionEvent to avoid infinite event recursion, just log
                logger.log(Level.SEVERE, "SubscriberExceptionEvent subscriber " + subscription.subscriber.getClass()
                        + " threw an exception", cause);
                SubscriberExceptionEvent exEvent = (SubscriberExceptionEvent) event;
                logger.log(Level.SEVERE, "Initial event " + exEvent.causingEvent + " caused exception in "
                        + exEvent.causingSubscriber, exEvent.throwable);
            }
        } else {
            if (throwSubscriberException) {
                throw new EventBusException("Invoking subscriber failed", cause);
            }
            if (logSubscriberExceptions) {
                logger.log(Level.SEVERE, "Could not dispatch event: " + event.getClass() + " to subscribing class "
                        + subscription.subscriber.getClass(), cause);
            }
            if (sendSubscriberExceptionEvent) {
                SubscriberExceptionEvent exEvent = new SubscriberExceptionEvent(this, cause, event,
                        subscription.subscriber);
                post(exEvent);
            }
        }
    }

    /**
     * For ThreadLocal, much faster to set (and get multiple values).
     *
     * 发送事件的线程封装类
     */
    final static class PostingThreadState {
        final List<Object> eventQueue = new ArrayList<>();  // 事件队列
        boolean isPosting; // 是否正在 posting
        boolean isMainThread;  // 是否为主线程
        Subscription subscription;
        Object event;
        boolean canceled; // 是否已经取消
    }

    ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * For internal use only.
     */
    public Logger getLogger() {
        return logger;
    }

    // Just an idea: we could provide a callback to post() to be notified, an alternative would be events, of course...
    /* public */interface PostCallback {
        void onPostCompleted(List<SubscriberExceptionEvent> exceptionEvents);
    }

    @Override
    public String toString() {
        return "EventBus[indexCount=" + indexCount + ", eventInheritance=" + eventInheritance + "]";
    }
}