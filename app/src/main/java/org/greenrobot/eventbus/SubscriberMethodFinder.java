package org.greenrobot.eventbus;

import org.greenrobot.eventbus.meta.SubscriberInfo;
import org.greenrobot.eventbus.meta.SubscriberInfoIndex;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class SubscriberMethodFinder {
    /*
     * In newer class files, compilers may add methods. Those are called bridge or synthetic methods.
     * EventBus must ignore both. There modifiers are not public but defined in the Java class file format:
     * http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.6-200-A.1
     */
    private static final int BRIDGE = 0x40;
    private static final int SYNTHETIC = 0x1000;

    private static final int MODIFIERS_IGNORE = Modifier.ABSTRACT | Modifier.STATIC | BRIDGE | SYNTHETIC;

    // 订阅方法的缓存，key为订阅者对象，value为订阅者对象所有的订阅方法是一个List集合
    private static final Map<Class<?>, List<SubscriberMethod>> METHOD_CACHE = new ConcurrentHashMap<>();

    private List<SubscriberInfoIndex> subscriberInfoIndexes;
    private final boolean strictMethodVerification;
    private final boolean ignoreGeneratedIndex;

    private static final int POOL_SIZE = 4;
    private static final FindState[] FIND_STATE_POOL = new FindState[POOL_SIZE];

    SubscriberMethodFinder(List<SubscriberInfoIndex> subscriberInfoIndexes, boolean strictMethodVerification,
                           boolean ignoreGeneratedIndex) {
        this.subscriberInfoIndexes = subscriberInfoIndexes;
        this.strictMethodVerification = strictMethodVerification;
        this.ignoreGeneratedIndex = ignoreGeneratedIndex;
    }

    /**
     * 订阅方法查找
     * @param subscriberClass 订阅者对象
     * @return 返回订阅者 所有的订阅方法 是一个List集合
     */
    List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
        // 首先在 METHOD_CACHE 中查找该 Event 对应的订阅者集合是否已经存在，如果有直接返回
        List<SubscriberMethod> subscriberMethods = METHOD_CACHE.get(subscriberClass);
        if (subscriberMethods != null) {
            return subscriberMethods;
        }

        // 根据订阅者类 subscriberClass 查找相应的订阅方法
        if (ignoreGeneratedIndex) { // 是否忽略生成 index 默认为 false，当为 true 时，表示以反射的方式获取订阅者中的订阅方法
            subscriberMethods = findUsingReflection(subscriberClass); // 通过反射获取
        } else {
            // 通过 SubscriberIndex 方式获取
            subscriberMethods = findUsingInfo(subscriberClass);
        }

        // 若订阅者中没有订阅方法，则抛异常
        if (subscriberMethods.isEmpty()) {
            throw new EventBusException("Subscriber " + subscriberClass
                    + " and its super classes have no public methods with the @Subscribe annotation");
        } else {
            METHOD_CACHE.put(subscriberClass, subscriberMethods); // 把订阅方法集合 List 存到缓存中
            return subscriberMethods;
        }
    }

    private List<SubscriberMethod> findUsingInfo(Class<?> subscriberClass) {
        // 通过 prepareFindState 获取到 FindState(保存找到的注解过的方法的状态)
        FindState findState = prepareFindState();

        // findState 与 subscriberClass 关联
        findState.initForSubscriber(subscriberClass);
        while (findState.clazz != null) {
            // 获取订阅者信息
            // 通过 SubscriberIndex 获取 findState.clazz 对应的 SubscriberInfo
            findState.subscriberInfo = getSubscriberInfo(findState);
            if (findState.subscriberInfo != null) {
                SubscriberMethod[] array = findState.subscriberInfo.getSubscriberMethods();
                for (SubscriberMethod subscriberMethod : array) {
                    if (findState.checkAdd(subscriberMethod.method, subscriberMethod.eventType)) {
                        // 逐个添加进 findState.subscriberMethods
                        findState.subscriberMethods.add(subscriberMethod);
                    }
                }
            } else {
                // 使用反射的方式获取单个类的订阅方法
                findUsingReflectionInSingleClass(findState);
            }
            findState.moveToSuperclass();
        }
        return getMethodsAndRelease(findState);
    }

    private List<SubscriberMethod> getMethodsAndRelease(FindState findState) {
        List<SubscriberMethod> subscriberMethods = new ArrayList<>(findState.subscriberMethods);
        findState.recycle();
        synchronized (FIND_STATE_POOL) {
            for (int i = 0; i < POOL_SIZE; i++) {
                if (FIND_STATE_POOL[i] == null) {
                    FIND_STATE_POOL[i] = findState;
                    break;
                }
            }
        }
        return subscriberMethods;
    }

    // 通过 prepareFindState 获取到 FindState 对象
    private FindState prepareFindState() {
        // 找到 FIND_STATE_POOL 对象池
        synchronized (FIND_STATE_POOL) {
            for (int i = 0; i < POOL_SIZE; i++) {
                // 当找到了对应的FindState
                FindState state = FIND_STATE_POOL[i];
                if (state != null) { // FindState 非空表示已经找到
                    FIND_STATE_POOL[i] = null; // 清空找到的这个FindState，为了下一次能接着复用这个FIND_STATE_POOL池
                    return state; // 返回该 FindState
                }
            }
        }

        // 如果依然没找到，则创建一个新的 FindState
        return new FindState();
    }

    private SubscriberInfo getSubscriberInfo(FindState findState) {
        if (findState.subscriberInfo != null && findState.subscriberInfo.getSuperSubscriberInfo() != null) {
            SubscriberInfo superclassInfo = findState.subscriberInfo.getSuperSubscriberInfo();
            if (findState.clazz == superclassInfo.getSubscriberClass()) {
                return superclassInfo;
            }
        }
        if (subscriberInfoIndexes != null) {
            for (SubscriberInfoIndex index : subscriberInfoIndexes) {
                SubscriberInfo info = index.getSubscriberInfo(findState.clazz);
                if (info != null) {
                    return info;
                }
            }
        }
        return null;
    }

    private List<SubscriberMethod> findUsingReflection(Class<?> subscriberClass) {
        // 创建并初始化 FindState 对象
        FindState findState = prepareFindState();

        // findState 与 subscriberClass 关联
        findState.initForSubscriber(subscriberClass);
        while (findState.clazz != null) {
            // 使用反射的方式获取单个类的订阅方法
            findUsingReflectionInSingleClass(findState);

            // 使 findState.clazz 指向父类的 Class，继续获取
            findState.moveToSuperclass();
        }

        // 返回订阅者及其父类的订阅方法 List，并释放资源
        return getMethodsAndRelease(findState);
    }

    private void findUsingReflectionInSingleClass(FindState findState) {
        Method[] methods;
        try {
            // This is faster than getMethods, especially when subscribers are fat classes like Activities
            methods = findState.clazz.getDeclaredMethods(); // 通过反射获取到所有方法
        } catch (Throwable th) {
            // Workaround for java.lang.NoClassDefFoundError, see https://github.com/greenrobot/EventBus/issues/149
            methods = findState.clazz.getMethods();
            findState.skipSuperClasses = true;
        }
        for (Method method : methods) {
            int modifiers = method.getModifiers();

            // 忽略非 public 和 static 的方法
            if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIERS_IGNORE) == 0) {

                // 获取订阅方法的所有参数
                Class<?>[] parameterTypes = method.getParameterTypes();

                // 订阅方法只能有一个参数，否则忽略
                if (parameterTypes.length == 1) {
                    // 获取有 Subscribe 的注解
                    Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
                    if (subscribeAnnotation != null) {
                        // 获取第一个参数
                        Class<?> eventType = parameterTypes[0];

                        // 检查 eventType 决定是否订阅，通常订阅者不能有多个 eventType 相同的订阅方法
                        if (findState.checkAdd(method, eventType)) {
                            // 获取线程模式
                            ThreadMode threadMode = subscribeAnnotation.threadMode();

                            // 添加订阅方法进 List
                            findState.subscriberMethods.add(new SubscriberMethod(method, eventType, threadMode,
                                    subscribeAnnotation.priority(), subscribeAnnotation.sticky()));
                        }
                    }
                } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
                    String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                    throw new EventBusException("@Subscribe method " + methodName +
                            "must have exactly 1 parameter but has " + parameterTypes.length);
                }
            } else if (strictMethodVerification && method.isAnnotationPresent(Subscribe.class)) {
                String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                throw new EventBusException(methodName +
                        " is a illegal @Subscribe method: must be public, non-static, and non-abstract");
            }
        }
    }

    static void clearCaches() {
        METHOD_CACHE.clear();
    }

    // FindState 封装了所有的订阅者和订阅方法的集合
    static class FindState {
        // 保存所有订阅方法
        final List<SubscriberMethod> subscriberMethods = new ArrayList<>();

        // 事件类型为Key，订阅方法为Value
        final Map<Class, Object> anyMethodByEventType = new HashMap<>();

        // 订阅方法为Key，订阅者的Class对象为Value
        final Map<String, Class> subscriberClassByMethodKey = new HashMap<>();

        final StringBuilder methodKeyBuilder = new StringBuilder(128);

        Class<?> subscriberClass;
        Class<?> clazz;
        boolean skipSuperClasses;
        SubscriberInfo subscriberInfo;

        void initForSubscriber(Class<?> subscriberClass) {
            this.subscriberClass = clazz = subscriberClass;
            skipSuperClasses = false;
            subscriberInfo = null;
        }

        void recycle() {
            subscriberMethods.clear();
            anyMethodByEventType.clear();
            subscriberClassByMethodKey.clear();
            methodKeyBuilder.setLength(0);
            subscriberClass = null;
            clazz = null;
            skipSuperClasses = false;
            subscriberInfo = null;
        }


        /**
         * EventBus 不仅仅获取当前类的订阅方法，还会获取它所有父类的订阅方法。
         *
         * 在 EventBus 中，一个订阅者包括这个订阅者的所有父类和子类，不会有多个方法相同的去接收同一个事件.
         * 但是有可能出现这样一种情况，子类去订阅了该事件，父类也去订阅了该事件。
         * 当出现这种情况，EventBus 如何判断？通过调用 checkAddWithMethodSignature() 方法，根据方法签名来检查
         */
        boolean checkAdd(Method method, Class<?> eventType) {
            // 2 level check: 1st level with event type only (fast), 2nd level with complete signature when required.
            // Usually a subscriber doesn't have methods listening to the same event type.
            // put()方法执行之后，返回的是之前put的值
            Object existing = anyMethodByEventType.put(eventType, method);
            if (existing == null) {
                return true;
            } else {
                if (existing instanceof Method) {
                    if (!checkAddWithMethodSignature((Method) existing, eventType)) {
                        // Paranoia check
                        throw new IllegalStateException();
                    }
                    // Put any non-Method object to "consume" the existing Method
                    anyMethodByEventType.put(eventType, this);
                }

                // 根据方法签名来检查
                return checkAddWithMethodSignature(method, eventType);
            }
        }

        private boolean checkAddWithMethodSignature(Method method, Class<?> eventType) {
            methodKeyBuilder.setLength(0);
            methodKeyBuilder.append(method.getName());
            methodKeyBuilder.append('>').append(eventType.getName());

            String methodKey = methodKeyBuilder.toString();
            Class<?> methodClass = method.getDeclaringClass();
            // put方法返回的是put之前的对象
            Class<?> methodClassOld = subscriberClassByMethodKey.put(methodKey, methodClass);

            // 如果methodClassOld不存在或者是methodClassOld的父类的话，则表明是它的父类，直接返回true。
            // 否则，就表明在它的子类中也找到了相应的订阅，执行的 put 操作是一个 revert 操作，put 进去的是 methodClassOld，而不是 methodClass
            if (methodClassOld == null || methodClassOld.isAssignableFrom(methodClass)) {
                // Only add if not already found in a sub class
                return true;
            } else {
                // Revert the put, old class is further down the class hierarchy
                // 这里是一个revert操作，所以如果找到了它的子类也订阅了该方法，则不允许父类和子类都同时订阅该事件，put 的是之前的那个 methodClassOld，就是将以前的那个 methodClassOld 存入 HashMap 去覆盖相同的订阅者。
                // 不允许出现一个订阅者有多个相同方法订阅同一个事件
                subscriberClassByMethodKey.put(methodKey, methodClassOld);
                return false;
            }
        }

        void moveToSuperclass() {
            if (skipSuperClasses) {
                clazz = null;
            } else {
                clazz = clazz.getSuperclass();
                String clazzName = clazz.getName();
                /** Skip system classes, this just degrades performance. */
                if (clazzName.startsWith("java.") || clazzName.startsWith("javax.") || clazzName.startsWith("android.")) {
                    clazz = null;
                }
            }
        }
    }

}
