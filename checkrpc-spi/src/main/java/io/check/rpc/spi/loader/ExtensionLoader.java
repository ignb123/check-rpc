package io.check.rpc.spi.loader;

import io.check.rpc.spi.annotation.SPI;
import io.check.rpc.spi.annotation.SPIClass;
import io.check.rpc.spi.factory.ExtensionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ExtensionLoader<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ExtensionLoader.class);


    // 服务目录路径，用于存放服务实现的配置文件
    private static final String SERVICES_DIRECTORY = "META-INF/services/";

    // 检查目录路径，分为内部和外部检查目录，用于存放检查逻辑的实现
    private static final String CHECK_DIRECTORY = "META-INF/check/";

    private static final String CHECK_DIRECTORY_EXTERNAL = "META-INF/check/external/";

    private static final String CHECK_DIRECTORY_INTERNAL = "META-INF/check/internal/";

    // 支持的服务和检查目录列表
    private static final String[] SPI_DIRECTORIES = new String[]{
            SERVICES_DIRECTORY,
            CHECK_DIRECTORY,
            CHECK_DIRECTORY_EXTERNAL,
            CHECK_DIRECTORY_INTERNAL
    };


    // 扩展加载器的缓存，键为扩展接口类型，值为对应的扩展加载器实例
    private static final Map<Class<?>, ExtensionLoader<?>> LOADERS = new ConcurrentHashMap<>();

    // 被加载的扩展接口的类对象
    private final Class<T> clazz;

    // 使用的类加载器
    private final ClassLoader classLoader;

    // 缓存的类映射，用于缓存 SPI 目录下所有类的映射
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    // 缓存的实例映射，键为扩展名称，值为对应的扩展实例
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();

    // SPI 类实例的缓存，键为类，值为该类的实例
    private final Map<Class<?>, Object> spiClassInstances = new ConcurrentHashMap<>();

    // 缓存的默认扩展名
    private String cachedDefaultName;

    /**
     * ExtensionLoader的私有构造方法，用于初始化一个特定类型的扩展加载器。
     *
     * @param clazz 要加载的扩展类的类型。
     * @param cl    要使用的类加载器。
     */
    private ExtensionLoader(final Class<T> clazz, final ClassLoader cl) {
        this.clazz = clazz;
        this.classLoader = cl;
        // 如果当前加载的类不是ExtensionFactory类，则强制加载ExtensionFactory的扩展类
        if (!Objects.equals(clazz, ExtensionFactory.class)) {
            ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getExtensionClasses();
        }
    }

    /**
     * 获取指定扩展类型的加载器。如果该类型的加载器已存在，则直接返回；否则，创建一个新的加载器实例。
     *
     * @param clazz 要加载的扩展类的类型。
     * @param cl    要使用的类加载器，如果为null，则使用默认类加载器。
     * @return 对应于指定扩展类型的加载器实例。
     * @throws NullPointerException     如果提供的扩展类类型为null。
     * @throws IllegalArgumentException 如果提供的扩展类不是接口，或者没有标注@SPI注解。
     */
    public static <T> ExtensionLoader<T> getExtensionLoader(final Class<T> clazz, final ClassLoader cl) {

        // 确保传入的clazz不为null
        Objects.requireNonNull(clazz, "extension clazz is null");

        // 确保传入的clazz为接口，并且标注了@SPI注解
        if (!clazz.isInterface()) {
            throw new IllegalArgumentException("extension clazz (" + clazz + ") is not interface!");
        }
        if (!clazz.isAnnotationPresent(SPI.class)) {
            throw new IllegalArgumentException("extension clazz (" + clazz + ") without @" + SPI.class + " Annotation");
        }
        // 从LOADERS缓存中尝试获取或创建对应的ExtensionLoader实例
        ExtensionLoader<T> extensionLoader = (ExtensionLoader<T>) LOADERS.get(clazz);
        if (Objects.nonNull(extensionLoader)) {
            return extensionLoader;
        }
        // 如果缓存中不存在，则创建新实例并加入缓存
        LOADERS.putIfAbsent(clazz, new ExtensionLoader<>(clazz, cl));
        return (ExtensionLoader<T>) LOADERS.get(clazz);
    }

    /**
     * 直接获取想要的类实例
     * @param clazz 接口的Class实例
     * @param name SPI名称
     * @param <T> 泛型类型
     * @return 泛型实例
     */
    public static <T> T getExtension(final Class<T> clazz, String name){
        return StringUtils.isEmpty(name) ? getExtensionLoader(clazz).getDefaultSpiClassInstance() : getExtensionLoader(clazz).getSpiClassInstance(name);
    }


    /**
     * 获取指定扩展类型的扩展加载器。
     *
     * @param <T> 扩展类型。
     * @param clazz 扩展类的Class对象。
     * @return 对应扩展类型的扩展加载器实例。
     */
    public static <T> ExtensionLoader<T> getExtensionLoader(final Class<T> clazz) {
        return getExtensionLoader(clazz, ExtensionLoader.class.getClassLoader());
    }

    /**
     * 获取默认扩展实例。
     *
     * @return 默认扩展实例。如果不存在默认扩展，则返回null。
     */
    public T getDefaultSpiClassInstance() {
        getExtensionClasses(); // 初始化扩展类
        if (StringUtils.isBlank(cachedDefaultName)) {
            return null;
        }
        return getSpiClassInstance(cachedDefaultName);
    }

        /**
     * 根据名称获取SPI类的实例。
     *
     * @param name SPI类的名称，不能为空。
     * @return 返回指定名称的SPI类的实例。
     * @throws NullPointerException 如果名称为null，则抛出此异常。
     */
    public T getSpiClassInstance(final String name) {
        // 检查名称是否为空，为空则抛出异常
        if (StringUtils.isBlank(name)) {
            throw new NullPointerException("get spi class name is null");
        }
        // 尝试从缓存中获取实例，如果不存在则进行初始化
        Holder<Object> objectHolder = cachedInstances.get(name);
        if (Objects.isNull(objectHolder)) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            objectHolder = cachedInstances.get(name);
        }
        // 双重检查锁定，确保线程安全地初始化实例
        Object value = objectHolder.getValue();
        if (Objects.isNull(value)) {
            synchronized (cachedInstances) {
                value = objectHolder.getValue();
                if (Objects.isNull(value)) {
                    // 创建扩展名对应的实例并设置到缓存中
                    value = createExtension(name);
                    objectHolder.setValue(value);
                }
            }
        }
        // 返回转换后的实例
        return (T) value;
    }

    /**
     * 获取所有SPI类的实例列表。
     *
     * @return 返回所有SPI类实例的列表，如果没有任何SPI类，则返回空列表。
     */
    public List<T> getSpiClassInstances() {
        // 获取所有扩展类的映射
        Map<String, Class<?>> extensionClasses = this.getExtensionClasses();
        // 如果扩展类为空，则直接返回空列表
        if (extensionClasses.isEmpty()) {
            return Collections.emptyList();
        }
        // 检查缓存是否已经包含了所有实例
        if (Objects.equals(extensionClasses.size(), cachedInstances.size())) {
            // 如果是，则直接从缓存中收集所有实例
            return (List<T>) this.cachedInstances.values().stream().map(e -> {
                return e.getValue();
            }).collect(Collectors.toList());
        }
        // 遍历扩展类映射，为每个名称获取并添加实例到列表中
        List<T> instances = new ArrayList<>();
        extensionClasses.forEach((name, v) -> {
            T instance = this.getSpiClassInstance(name);
            instances.add(instance);
        });
        // 返回实例列表
        return instances;
    }

    /**
     * 根据提供的名称创建扩展类的实例。
     *
     * @param name 扩展的名称，对应扩展类的唯一标识。
     * @return T 返回扩展类的实例。
     * @throws IllegalArgumentException 如果提供的名称不存在于扩展类集合中。
     * @throws IllegalStateException 如果扩展类实例化失败。
     */
    @SuppressWarnings("unchecked")
    private T createExtension(final String name) {
        // 获取指定名称的扩展类
        Class<?> aClass = getExtensionClasses().get(name);
        if (Objects.isNull(aClass)) {
            throw new IllegalArgumentException("name is error");
        }
        // 尝试从缓存中获取实例，如果不存在则尝试创建新实例
        Object o = spiClassInstances.get(aClass);
        if (Objects.isNull(o)) {
            try {
                spiClassInstances.putIfAbsent(aClass, aClass.newInstance());
                o = spiClassInstances.get(aClass);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("Extension instance(name: " + name + ", class: "
                        + aClass + ")  could not be instantiated: " + e.getMessage(), e);
            }
        }
        return (T) o;
    }

    /**
     * 获取所有扩展类的映射。
     *
     * @return Map<String, Class<?>> 扩展类的名称与其对应类的映射。
     */
    public Map<String, Class<?>> getExtensionClasses() {
        // 从缓存中获取扩展类映射，若缓存未初始化则进行加载并存入缓存
        Map<String, Class<?>> classes = cachedClasses.getValue();
        if (Objects.isNull(classes)) {
            synchronized (cachedClasses) {
                classes = cachedClasses.getValue();
                if (Objects.isNull(classes)) {
                    classes = loadExtensionClass(); // 加载扩展类
                    cachedClasses.setValue(classes); // 存入缓存
                }
            }
        }
        return classes;
    }

    /**
     * 加载扩展类。
     * 该方法首先检查当前类是否有SPI注解，并从注解中获取默认名称，然后加载指定目录下的所有扩展类。
     *
     * @return 返回一个包含所有加载的扩展类的映射，键为扩展名，值为对应的类。
     */
    private Map<String, Class<?>> loadExtensionClass() {
        // 检查当前类是否有SPI注解，并获取注解中指定的默认名称
        SPI annotation = clazz.getAnnotation(SPI.class);
        if (Objects.nonNull(annotation)) {
            String value = annotation.value();
            if (StringUtils.isNotBlank(value)) {
                cachedDefaultName = value;
            }
        }
        Map<String, Class<?>> classes = new HashMap<>(16);
        // 加载指定目录下的所有扩展类
        loadDirectory(classes);
        return classes;
    }

    /**
     * 从指定的目录下加载扩展类。
     * 遍历SPI_DIRECTORIES数组，尝试加载指定名称的类的资源，将加载到的类添加到classes映射中。
     *
     * @param classes 用于存储加载的扩展类的映射。
     */
    private void loadDirectory(final Map<String, Class<?>> classes) {
        for (String directory : SPI_DIRECTORIES){
            // 构造资源文件名称
            String fileName = directory + clazz.getName();
            try {
                // 尝试从当前类加载器或系统类加载器中获取资源
                Enumeration<URL> urls = Objects.nonNull(this.classLoader) ? classLoader.getResources(fileName)
                        : ClassLoader.getSystemResources(fileName);
                if (Objects.nonNull(urls)) {
                    // 遍历所有找到的资源并加载
                    while (urls.hasMoreElements()) {
                        URL url = urls.nextElement();
                        loadResources(classes, url);
                    }
                }
            } catch (IOException t) {
                // 记录加载资源失败的错误日志
                LOG.error("load extension class error {}", fileName, t);
            }
        }
    }


    /**
     * 加载资源文件，并将其中的属性与对应的类加载到提供的Map中。
     *
     * @param classes 用于存储属性名与对应类的映射关系的Map
     * @param url 资源文件的URL
     * @throws IOException 如果读取资源文件失败
     */
    private void loadResources(final Map<String, Class<?>> classes, final URL url) throws IOException {
        // 从URL打开输入流，读取资源文件
        try (InputStream inputStream = url.openStream()) {
            Properties properties = new Properties();
            properties.load(inputStream);
            // 遍历属性，加载每个属性指定的类
            properties.forEach((k, v) -> {
                String name = (String) k;
                String classPath = (String) v;
                // 忽略空名称或路径的属性
                if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(classPath)) {
                    try {
                        // 加载类，并将其与名称映射存入classes Map中
                        loadClass(classes, name, classPath);
                    } catch (ClassNotFoundException e) {
                        // 如果类加载失败，抛出运行时异常
                        throw new IllegalStateException("load extension resources error", e);
                    }
                }
            });
        } catch (IOException e) {
            // 如果读取资源文件失败，抛出运行时异常
            throw new IllegalStateException("load extension resources error", e);
        }
    }

    /**
     * 加载指定的类，并进行类型检查和唯一性验证。
     *
     * @param classes 一个存储已加载类的映射，键为类名，值为类本身。
     * @param name 待加载类的名称。
     * @param classPath 待加载类的路径。
     * @throws ClassNotFoundException 如果指定的类找不到。
     * @throws IllegalStateException 如果待加载类不是期望的子类型，或者没有指定的注解。
     */
    private void loadClass(final Map<String, Class<?>> classes,
                           final String name, final String classPath) throws ClassNotFoundException {
        // 尝试使用提供的类加载器或默认类加载器加载指定的类
        Class<?> subClass = Objects.nonNull(this.classLoader) ? Class.forName(classPath, true, this.classLoader) : Class.forName(classPath);

        // 检查加载的类是否为期望的子类型
        if (!clazz.isAssignableFrom(subClass)) {
            throw new IllegalStateException("load extension resources error," + subClass + " subtype is not of " + clazz);
        }

        // 检查加载的类是否包含指定的注解
        if (!subClass.isAnnotationPresent(SPIClass.class)) {
            throw new IllegalStateException("load extension resources error," + subClass + " without @" + SPIClass.class + " annotation");
        }

        // 检查是否已加载同名类，并处理重复加载的情况
        Class<?> oldClass = classes.get(name);
        if (Objects.isNull(oldClass)) {
            // 如果之前未加载过该类，则添加到类映射中
            classes.put(name, subClass);
        } else if (!Objects.equals(oldClass, subClass)) {
            // 如果已加载过同名但不同类的冲突，则抛出异常
            throw new IllegalStateException("load extension resources error,Duplicate class " + clazz.getName() + " name " + name + " on " + oldClass.getName() + " or " + subClass.getName());
        }
    }


    /**
     * 一个泛型类，用于持有类型为T的对象引用。
     * 此类适用于在多线程环境中以线程安全的方式存储和交换数据，由于使用了volatile关键字。
     */
    public static class Holder<T> {

        // 使用volatile关键字确保对此变量的更改对所有线程立即可见。
        private volatile T value;

        /**
         * 获取此Holder当前持有的值。
         *
         * @return 当前的值
         */
        public T getValue() {
            return value;
        }

        /**
         * 设置此Holder的值。
         *
         * @param value 要设置的新值
         */
        public void setValue(final T value) {
            this.value = value;
        }
    }
}
