/*
 *   WebGiisoo, a java web foramewrok.
 *   Copyright (C) <2014>  <giisoo inc.>
 *
 */
package com.giisoo.framework.web;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.zip.*;

import org.apache.commons.configuration.*;
import org.apache.commons.logging.*;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;

import com.giisoo.core.bean.Bean;
import com.giisoo.core.bean.Beans;
import com.giisoo.core.bean.X;
import com.giisoo.core.conf.SystemConfig;
import com.giisoo.framework.common.Access;
import com.giisoo.framework.common.Menu;
import com.giisoo.framework.common.OpLog;
import com.giisoo.framework.common.User;
import com.giisoo.framework.utils.FileUtil;

/**
 * module includes: a module.ini, a group of model/view/images/css/js/language,
 * etc. and it will handle the request, if not found the handler in the module,
 * then will let the parent to handle it
 * 
 * the module organized as a chain, the home module will handler first, if found
 * then stop, else parent or parent's parent
 * 
 * this purpose of module is reuse the module with only changing of
 * configuration file
 * 
 * @author yjiang
 * 
 */
public class Module {

    static Log log = LogFactory.getLog(Module.class);

    /**
     * the absolute path of the module
     */
    String path;

    /**
     * the id of the module, MUST unique, and also is a sequence of the loading:
     * biggest first
     */
    public int id;

    /**
     * the url pattern which transfer directly, no parse
     */
    String transparent;

    /**
     * the name of the module, the name of module should be unique in whole
     * context
     */
    String name;

    /**
     * lifelistener which will be invoke in each life cycle of the module
     */
    String lifelistener;

    boolean enabled = false;

    String version;
    String build;

    String screenshot;

    /**
     * the root package name of the module, which will use to mapping the
     * handler
     */
    String pack;

    Map<String, String> settings = new TreeMap<String, String>();

    /**
     * readme file maybe html
     */
    String readme;

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return new StringBuilder("Module [").append(id).append(",").append(name).append(",enabled=").append(enabled).append("]").toString();
    }

    /**
     * parent module defined in module.ini
     */
    // protected Module parent = null;
    //
    // protected Module child = null;

    public String getLanguage() {
        if (get("default.language") != null) {
            return get("default.language");
        }

        Module f = this.floor();
        if (f != null) {
            return f.getLanguage();
        } else {
            return "zh_cn";
        }
    }

    /**
     * module class loader
     */
    public static GClassLoader classLoader;

    /**
     * the default home module
     */
    public static Module home;

    /**
     * cache all modules by name
     */
    private static TreeMap<Integer, Module> modules = new TreeMap<Integer, Module>();

    /**
     * cache the model in the module, the modelMap structure: {"uri", "class"}
     */
    protected Map<String, Class<Model>> modelMap = new HashMap<String, Class<Model>>();

    /**
     * the structure of the pathmapping: {"class", {"method", {"path",
     * Path|Method}}}
     */
    protected Map<Class<Model>, Map<Integer, Map<String, Model.PathMapping>>> pathmapping = new HashMap<Class<Model>, Map<Integer, Map<String, Model.PathMapping>>>();

    /**
     * configuration
     */
    public static Configuration _conf;

    // public Module getParent() {
    // return parent;
    // }

    /**
     * get the module by name
     * 
     * @param module
     * @return Module
     */
    public Module module(String module) {
        for (Module m : modules.values()) {
            if (m.name.equals(module)) {
                return m;
            }
        }
        return null;
    }

    /**
     * Clean.
     */
    public static void clean() {
        home = null;
        modules.clear();
    }

    /**
     * Store.
     */
    public void store() {
        File f = new File(_conf.getString("home") + "/modules/" + name + "/module.ini");
        try {
            PropertiesConfiguration p = new PropertiesConfiguration();
            p.setEncoding("utf-8");
            // p.set
            // p.load(new FileInputStream(f));
            p.setProperty("name", name);
            p.setProperty("package", pack);
            p.setProperty("lifelistener", lifelistener);
            p.setProperty("id", id);
            p.setProperty("enabled", enabled);

            p.setProperty("version", version);
            p.setProperty("build", build);
            p.setProperty("screenshot", screenshot);
            p.setProperty("readme", readme);
            p.setProperty("transparent", transparent);

            for (String name : settings.keySet()) {
                p.setProperty(name, settings.get(name));
            }

            p.save(f);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Gets the.
     * 
     * @param name
     *            the name
     * @return the string
     */
    public String get(String name) {
        return get(name, null);
    }

    public String getRepo() {
        return get(this.name + "_repo");
    }

    /**
     * Gets the int.
     * 
     * @param setting
     *            the setting
     * @return the int
     */
    public int getInt(String setting) {
        return Bean.toInt(get(setting));
    }

    /**
     * Gets the long.
     * 
     * @param setting
     *            the setting
     * @return the long
     */
    public long getLong(String setting) {
        return Bean.toLong(get(setting));
    }

    /**
     * Checks if is enabled.
     * 
     * @param setting
     *            the setting
     * @return true, if is enabled
     */
    public boolean isEnabled(String setting) {
        return "true".equalsIgnoreCase(get(setting, "false"));
    }

    /**
     * Gets the.
     * 
     * @param name
     *            the name
     * @param defaultValue
     *            the default value
     * @return the string
     */
    public String get(String name, String defaultValue) {
        String s = name;
        if (!s.startsWith("setting.")) {
            s = "setting." + name;
        }

        if (settings.containsKey(s)) {
            return settings.get(s);
        } else {
            Module m = this.floor();
            if (m != null) {
                return m.get(s, defaultValue);
            }

            // set(s, defaultValue);
            //
            return SystemConfig.s(name, defaultValue);
        }
    }

    /**
     * Sets the.
     * 
     * @param name
     *            the name
     * @param value
     *            the value
     */
    public void set(String name, String value) {
        if (name.startsWith("setting.")) {
            settings.put(name, value);
        } else {
            settings.put("setting." + name, value);
        }

    }

    /**
     * loading all jar files in /model
     */
    private void initModels() {
        File root = new File(path + "/model");
        // log.debug("looking for: " + root.getAbsolutePath());

        File[] list = root.listFiles();
        if (list != null) {
            for (File f : list) {
                if (f.getName().endsWith(".jar")) {
                    classLoader.addJar(f.getAbsolutePath());
                    log.debug("loading: " + f.getAbsolutePath());
                }
            }
        }

        // if (parent != null) {
        // parent.initModels();
        // }
    }

    /**
     * Gets the file.
     * 
     * @param resource
     *            the resource
     * @return the file
     */
    public File getFile(String resource) {
        return getFile(resource, true);
    }

    /**
     * Gets the file.
     * 
     * @param resource
     *            the resource
     * @param inFloor
     *            the in floor
     * @return the file
     */
    public File getFile(String resource, boolean inFloor) {
        try {
            File f = new File(path + "/view/" + resource);
            if (f.exists()) {
                /**
                 * test the file is still in the path? if not, then do not allow
                 * to access, avoid user using "../../" to access system file
                 */
                if (f.getCanonicalPath().startsWith(path + "/view")) {
                    return f;
                }
            }

            if (inFloor) {
                Module e = floor();
                if (e != null) {
                    return e.getFile(resource);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    public String[] getSupportLocale() {
        File f = new File(path + "/i18n/");
        if (f.exists()) {
            return f.list();
        }

        return null;
    }

    /**
     * Gets the all langs.
     * 
     * @param locale
     *            the locale
     * @return the all langs
     */
    public File[] getAllLangs(String locale) {
        File f = new File(path + "/i18n/" + locale);
        if (f.exists()) {
            return f.listFiles();
        }

        return null;
    }

    /**
     * Gets the all langs.
     * 
     * @param locale
     *            the locale
     * @param lang
     *            the lang
     * @return the all langs
     */
    public String getAllLangs(String locale, String lang) {
        File f = new File(path + "/i18n/" + locale + "/" + lang);
        if (f.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf-8"));
                StringBuilder sb = new StringBuilder();
                String line = reader.readLine();
                while (line != null) {
                    sb.append(line).append("\r\n");
                    line = reader.readLine();
                }
                return sb.toString();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            }
        }

        return null;
    }

    public String getPath() {
        return path;
    }

    public int getId() {
        return id;
    }

    public String getLifelistener() {
        return lifelistener;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getTransparent() {
        Module m = floor();
        if (X.isEmpty(transparent)) {
            if (m != null) {
                return m.getTransparent();
            }
            return X.EMPTY;
        }

        if (transparent.indexOf("$(transparent.url)") >= 0) {
            String s = X.EMPTY;
            if (m != null) {
                s = m.getTransparent();
            }
            return transparent.replaceAll("\\$\\(transparent\\.url\\)", s);
        }

        return transparent;
    }

    public String getScreenshot() {
        return screenshot;
    }

    public String getVersion() {
        return version;
    }

    public String getBuild() {
        return build;
    }

    public String getPack() {
        return pack;
    }

    public String getReadme() {
        return readme;
    }

    /**
     * Inits the.
     * 
     * @param conf
     *            the conf
     */
    public static void init(Configuration conf) {
        try {
            _conf = conf;
            classLoader = new GClassLoader(Module.class.getClassLoader());

            Thread thread = Thread.currentThread();
            thread.setContextClassLoader(classLoader);

            // String t = SystemConfig.s("home.module", "default");
            File f = new File(_conf.getString("home") + "/modules/");
            if (f.exists()) {
                File[] list = f.listFiles();
                for (File f1 : list) {
                    if (f1.isDirectory()) {
                        Module m = load(f1.getName());

                        if (m == null) {
                            log.info("[" + f1.getName() + "] is not a valid module");
                        } else if (!m.enabled) {
                            log.info("[" + f1.getName() + "] is disabled");
                        } else if (modules.containsKey(m.id)) {
                            log.error("the [id] duplicated, [" + m.name + ", " + modules.get(m.id).name + "], ignore the [" + m.name + "]");
                        } else {
                            /**
                             * cache the module
                             */
                            modules.put(m.id, m);
                        }
                    }
                }
            }

            if (modules.size() > 0) {
                home = modules.lastEntry().getValue();
            }

            Menu.reset();

            for (Module m : modules.values()) {
                /**
                 * loading the models
                 */
                m.initModels();

                /**
                 * initialize the life listener
                 */
                m._init(_conf);

            }
            /**
             * initialize template loader for velocity
             */
            Properties p = new Properties();
            p.setProperty("file.resource.loader.class", "com.giisoo.framework.web.TemplateLoader");
            Velocity.init(p);

            Menu.cleanup();

            // the the default locale
            String locale = null;
            Module f1 = home;
            while (locale == null && f1 != null) {
                locale = f1.get("default.locale");
                f1 = f1.floor();
            }
            if (locale != null) {
                Locale.setDefault(new Locale(locale));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Load.
     * 
     * @param name
     *            the name
     * @return the module
     */
    public static Module load(String name) {
        try {
            for (Module m : modules.values()) {
                if (name.equals(m.name)) {
                    return m;
                }
            }

            Module t = new Module();
            File f = new File(Model.HOME + "/modules/" + name + "/module.ini");
            if (f.exists()) {
                /**
                 * initialize the module
                 */
                PropertiesConfiguration p = new PropertiesConfiguration();
                p.setEncoding("UTF-8");
                p.load(f);

                t.name = p.getString("name");
                t.pack = p.containsKey("package") ? p.getString("package") : null;
                t.lifelistener = p.containsKey("lifelistener") ? p.getString("lifelistener") : null;
                t.path = f.getParent();
                if (p.containsKey("id")) {
                    t.id = p.getInt("id");
                } else {
                    log.error("[id] missed in module:" + t.name + ", ignore the module");
                    return null;
                }

                t.enabled = p.getBoolean("enabled", false);
                t.screenshot = p.getString("screenshot", "/images/nopic.png");
                t.readme = p.getString("readme", null);
                t.version = p.getString("version", null);
                t.build = p.getString("build", null);
                t.transparent = p.getString("transparent.url", null);

                // Iterator<String> it = p.getKeys("setting.");
                //
                // while (it.hasNext()) {
                // String s = it.next();
                // t.settings.put(s, p.getString(s));
                // }
                @SuppressWarnings("unchecked")
                Iterator<String> it = p.getKeys();

                while (it.hasNext()) {
                    String s = it.next();
                    // log.debug(s);
                    if (s.startsWith("setting.")) {
                        t.settings.put(s, p.getString(s));
                    }
                }

                return t;
            }

        } catch (Exception e) {
            log.error("loadModule:" + name, e);
        }

        return null;
    }

    /**
     * invoke the life listener of the module
     * 
     * @param conf
     */
    private void _init(Configuration conf) {
        // find the lifelistener, and init
        try {
            if (this.id > 0) {
                /**
                 * force: using default lifelistener to initialize the install
                 * script
                 */
                Module m = modules.get(0);
                String name = m.lifelistener;
                if (name != null) {

                    Class<?> c = Class.forName(name, true, classLoader);
                    Object o = c.newInstance();

                    if (o instanceof LifeListener) {

                        LifeListener l = (LifeListener) o;

                        l.upgrade(conf, this);
                    }
                }
            }

            /**
             * loading the module's lifelistener, to initialize the install
             * script
             */
            if (!X.isEmpty(lifelistener)) {
                String name = lifelistener;
                if (name != null) {

                    Class<?> c = Class.forName(name, true, classLoader);
                    Object o = c.newInstance();

                    if (o instanceof LifeListener) {

                        log.info("initializing: " + name);
                        LifeListener l = (LifeListener) o;

                        l.upgrade(conf, this);

                        l.onStart(conf, this);
                    }
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * get all modules
     * 
     * @return List
     */
    public static List<Module> getAll() {
        String home = _conf.getString("home");
        File troot = new File(home + "/modules/");
        File[] files = troot.listFiles();

        List<Module> list = new ArrayList<Module>();

        for (File f : files) {
            if (f.isDirectory()) {
                Module m = load(f.getName());
                if (m != null && !m.enabled) {
                    list.add(m);
                }
            }
        }

        return list;
    }

    /**
     * Load model from cache.
     * 
     * @param uri
     *            the uri
     * @return Model
     */
    public Model loadModelFromCache(String uri) {
        try {
            Class<Model> c = modelMap.get(uri);

            if (c != null) {
                Model m = c.newInstance();
                m.module = this;
                m.pathmapping = pathmapping.get(c);
                return m;
            }

        } catch (Exception e) {
            // e.printStackTrace();
        }

        Module e = floor();
        if (e != null) {
            return e.loadModelFromCache(uri);
        }

        return null;
    }

    /**
     * Load model.
     * 
     * @param uri
     *            the uri
     * @return the model
     */
    @SuppressWarnings("unchecked")
    public synchronized Model loadModel(String uri) {

        try {
            Class<Model> c = modelMap.get(uri);

            if (c != null) {

                Model m = c.newInstance();
                m.module = this;
                m.pathmapping = pathmapping.get(c);

                return m;
            }

            /**
             * looking for the model class
             */
            String name = (pack + "." + uri).replace("/", ".").replace("..", ".");
            // log.debug("loading [" + name + "]");

            c = (Class<Model>) Class.forName(name, false, classLoader);

            /**
             * cache it and cache all the path
             */
            modelMap.put(uri, c);

            _loadPath(c);

            Model m = c.newInstance();
            m.module = this;
            m.pathmapping = pathmapping.get(c);

            return m;
            // System.out.println("loading [" + name + "], c=" + c);

        } catch (Throwable e) {
            /**
             * not found, or is not a model, ignore the exception
             */

        }

        Module e = floor();
        if (e != null) {
            return e.loadModel(uri);
        }

        return null;
    }

    private void _loadPath(Class<Model> c) {
        Method[] list = c.getMethods();
        if (list != null && list.length > 0) {

            Map<Integer, Map<String, Model.PathMapping>> map = new HashMap<Integer, Map<String, Model.PathMapping>>();
            for (Method m : list) {
                Path p = m.getAnnotation(Path.class);
                if (p != null) {
                    /**
                     * check the access and insert
                     */
                    String access = p.access();
                    if (!X.isEmpty(access) && !X.NONE.equals(access)) {
                        if (access.startsWith("access.")) {
                            log.debug("access[" + access + "] at " + c.getCanonicalName() + "." + m.getName());

                            String[] ss = access.split("\\|");
                            for (String s : ss) {
                                Access.set(s);
                            }
                        } else if (!X.isEmpty(access)) {
                            log.error("access error! [" + access + "] at " + c.getCanonicalName() + "." + m.getName());
                        }
                    }

                    int method = p.method();
                    String path = p.path();

                    Model.PathMapping oo = Model.PathMapping.create(Pattern.compile(path), p, m);

                    /**
                     * set the method mapping info
                     */
                    if ((method & Model.METHOD_GET) > 0) {
                        Map<String, Model.PathMapping> mm = map.get(Model.METHOD_GET);
                        if (mm == null) {
                            mm = new HashMap<String, Model.PathMapping>();
                            map.put(Model.METHOD_GET, mm);
                        }
                        mm.put(path, oo);
                    }

                    if ((method & Model.METHOD_MDC) > 0) {
                        Map<String, Model.PathMapping> mm = map.get(Model.METHOD_MDC);
                        if (mm == null) {
                            mm = new HashMap<String, Model.PathMapping>();
                            map.put(Model.METHOD_MDC, mm);
                        }
                        mm.put(path, oo);
                    }

                    if ((method & Model.METHOD_POST) > 0) {
                        Map<String, Model.PathMapping> mm = map.get(Model.METHOD_POST);
                        if (mm == null) {
                            mm = new HashMap<String, Model.PathMapping>();
                            map.put(Model.METHOD_POST, mm);
                        }
                        mm.put(path, oo);
                    }
                }
            }

            if (map.size() > 0) {
                pathmapping.put(c, map);
            }
        }

    }

    /**
     * Load resource.
     * 
     * @param uri
     *            the uri
     * @return the file
     */
    public File loadResource(String uri) {
        return loadResource(uri, true);
    }

    /**
     * Load resource.
     * 
     * @param uri
     *            the uri
     * @param infloor
     *            the infloor
     * @return the file
     */
    public File loadResource(String uri, boolean infloor) {

        try {
            File f = new File(path + "/view" + uri);
            // log.debug("testing: " + f.getAbsolutePath() + ", exists?" +
            // f.exists());

            if (f.exists() && f.getCanonicalPath().startsWith(path + "/view"))
                return f;

            if (infloor) {
                Module e = floor();
                if (e != null) {
                    return e.loadResource(uri, infloor);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * Gets the template.
     * 
     * @param viewname
     *            the viewname
     * @return the template
     */
    public Template getTemplate(String viewname, boolean allowEmpty) {
        try {
            viewname = viewname.replaceAll("/", File.separator);
            File f = new File(path + File.separator + "view" + File.separator + viewname);
            // System.out.println(f.getAbsolutePath());

            if (f.exists() && f.getCanonicalPath().startsWith(path + File.separator + "view")) {
                return Velocity.getTemplate(viewname, "UTF-8");
            }

            Module e = floor();
            if (e != null) {
                Model.setCurrentModule(e);
                return e.getTemplate(viewname, allowEmpty);
            }
        } catch (Exception e) {
            /**
             * load resource error, please restart server
             */
            OpLog.error("system", "load resource error", e.getMessage());
            log.error(e.getMessage(), e);
        }

        /**
         * return empty.html
         */
        if (allowEmpty) {
            return Velocity.getTemplate("empty.html", "UTF-8");
        }

        return null;
    }

    /**
     * Load lang.
     * 
     * @param data
     *            the data
     * @param locale
     *            the locale
     */
    public void loadLang(Map<String, String> data, String locale) {

        Module e = floor();
        if (e != null) {
            e.loadLang(data, locale);
        }

        /**
         * read the file
         */
        File f = new File(path + "/i18n/" + locale + ".lang");

        // log.debug("loading:" + f.getAbsolutePath() + ", path=" + path +
        // ", locale:" + locale + ", lang:" + lang);

        if (f.exists()) {
            try {
                /**
                 * read the language file using utf-8 encoding?
                 */
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
                try {
                    String line = reader.readLine();
                    while (line != null) {
                        if (!line.startsWith("#")) {
                            int i = line.indexOf("=");
                            if (i > 0) {
                                String name = line.substring(0, i).trim();
                                String value = line.substring(i + 1).trim();
                                if ("@include".equals(name)) {
                                    /**
                                     * load the include value as lang file
                                     */
                                    loadLang(data, name);
                                } else {
                                    data.put(name, value);
                                }
                            }
                        }

                        line = reader.readLine();
                    }
                } finally {
                    reader.close();
                }
            } catch (Exception e1) {
                log.error("loadLang:" + locale, e1);
            }

        }
    }

    public String getName() {
        return name;
    }

    public static Module getHome() {
        return home;
    }

    /**
     * Put lang.
     * 
     * @param locale
     *            the locale
     * @param name
     *            the name
     */
    public void putLang(String locale, String name) {

        // log.error("not found", new Exception("not found [" + name + "] "));

        /**
         * read the file
         */
        File f = new File(path + "/i18n/" + locale + ".lang");

        Map<String, String> tmp = new TreeMap<String, String>();
        tmp.put(name, name);

        if (f.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
                String line = reader.readLine();
                while (line != null) {
                    line = line.trim();
                    if (!line.startsWith("#")) {
                        int i = line.indexOf("=");
                        if (i > 0) {
                            String s1 = line.substring(0, i);
                            String s2 = line.substring(i + 1);
                            tmp.put(s1, s2);
                        }
                    }

                    line = reader.readLine();
                }

            } catch (Exception e) {
                log.error(f.getAbsolutePath(), e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            }
        } else {
            f.getParentFile().mkdirs();
        }

        PrintStream out = null;
        try {
            out = new PrintStream(f, "UTF-8");

            for (String key : tmp.keySet()) {
                out.println(key + "=" + tmp.get(key));
            }
            out.flush();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Floor.
     * 
     * @return the module
     */
    public Module floor() {
        Entry<Integer, Module> e = modules.floorEntry(id - 1);
        if (e != null) {
            return e.getValue();
        }
        return null;
    }

    public void setEnabled(boolean b) {
        if (id == 0 && !b) {
            return;
        }

        enabled = b;

        if (b) {
            modules.put(id, this);
        } else {
            modules.remove(id);
        }

        if (modules.size() > 0) {
            home = modules.lastEntry().getValue();
        }

        if (!b) {

            try {
                if (this.id > 0) {
                    Module m = modules.get(0);
                    String name = m.lifelistener;
                    if (name != null) {
                        Class<?> c = Class.forName(name, true, classLoader);
                        Object o = c.newInstance();

                        if (o instanceof LifeListener) {

                            LifeListener l = (LifeListener) o;

                            l.upgrade(_conf, this);
                        }
                    }
                }
                if (this.lifelistener != null) {
                    String name = lifelistener;
                    if (name != null) {
                        Class<?> c = Class.forName(name, true, classLoader);
                        Object o = c.newInstance();

                        if (o instanceof LifeListener) {

                            log.info("initializing: " + name);
                            LifeListener l = (LifeListener) o;

                            l.onStop();
                            l.uninstall(_conf, this);
                        }
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        store();

    }

    public String getMenu() {
        /**
         * check the menus
         * 
         */
        File f = getFile("/install/menu.json", false);
        if (f != null && f.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line = reader.readLine();
                while (line != null) {
                    sb.append(line).append("\r\n");
                    line = reader.readLine();
                }

                return sb.toString();

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            }
        } else {
            return "[{'name':'admin', 'childs':[]},\r\n{'name':'home', childs:[]},\r\n{'name':'user', childs:[]}]\r\n";
        }

        return null;
    }

    /**
     * Zip to.
     * 
     * @param file
     *            the file
     * @return the file
     */
    public File zipTo(String file) {
        /**
         * model, view, i18n, module.ini
         */
        try {
            File f = new File(file);
            f.getParentFile().mkdirs();
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));

            for (String s : source) {
                addFile(out, s);
            }

            out.close();

            return f;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    private int addFile(ZipOutputStream out, String filename) {

        File f = new File(path + filename);
        if (f.isFile()) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(f);

                // Add ZIP entry to output stream.
                out.putNextEntry(new ZipEntry(filename));

                // Transfer bytes from the file to the ZIP file
                int len;
                byte[] buf = new byte[1024];
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                // Complete the entry
                out.closeEntry();

                return 1;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            }
        } else if (f.isDirectory()) {
            try {
                ZipEntry z = new ZipEntry(filename + "/");
                out.putNextEntry(z);

                String[] list = f.list();
                int i = 0;
                if (list != null) {
                    for (String s : list) {
                        i += addFile(out, filename + "/" + s);
                    }
                }
                return i;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        return 0;
    }

    public Set<String> getSettings() {
        return settings.keySet();
    }

    private static String[] source = new String[] { "/model", "/view", "/i18n", "/module.ini" };

    /**
     * Update lang.
     * 
     * @param locale
     *            the locale
     * @param langname
     *            the langname
     * @param text
     *            the text
     */
    public void updateLang(String locale, String langname, String text) {
        File f = new File(path + "/i18n/" + locale + "/" + langname);
        if (f.exists()) {
            PrintStream out = null;
            try {
                out = new PrintStream(new FileOutputStream(f));
                out.println(text);

                Language.clean();

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    /**
     * Support locale.
     * 
     * @param locale
     *            the locale
     * @return true, if successful
     */
    public boolean supportLocale(String locale) {
        return new File(path + "/i18n/" + locale + ".lang").exists();
    }

    /**
     * Inits the.
     * 
     * @param m
     *            the m
     * @return true, if successful
     */
    public static boolean init(Module m) {
        if (!m.enabled) {
            log.info("[" + m.getName() + "] is disabled");
        } else if (modules.containsKey(m.id)) {
            log.error("the [id] duplicated, [" + m.name + ", " + modules.get(m.id).name + "], ignore the [" + m.name + "]");
        } else {
            try {
                /**
                 * possible the original has been moved to ..., always using the
                 * package to initialize
                 */
                m.path = new File(Model.HOME + "/modules/" + m.name).getCanonicalPath();

                /**
                 * loading the models
                 */
                m.initModels();

                /**
                 * initialize the life listener
                 */
                m._init(_conf);

                /**
                 * cache the module
                 */
                modules.put(m.id, m);

                if (modules.size() > 0) {
                    home = modules.lastEntry().getValue();
                }

                return true;
            } catch (Exception e) {
                log.error(m.name, e);
            }
        }

        return false;

    }

    /**
     * Delete.
     */
    public void delete() {
        File f = new File(path);
        delete(f);

        modules.remove(id);
    }

    private void delete(File f) {

        if (f.isFile()) {
            /**
             * delete the file
             */
            f.delete();
        } else if (f.isDirectory()) {
            File[] list = f.listFiles();
            if (list != null && list.length > 0) {
                /**
                 * delete all file's or dir
                 */
                for (File f1 : list) {
                    delete(f1);
                }
            }

            /**
             * delete the empty dir
             */
            f.delete();
        }
    }

    /**
     * Merge.
     */
    public boolean merge() {

        String webinf = this.path + File.separator + "WEB-INF";
        File f = new File(webinf);

        if (f.exists() && f.isDirectory()) {
            /**
             * copy all files to application, and remove original one
             */
            return move(f, Model.HOME);
        } else {
            log.debug("no files need merge!");
        }
        return false;
    }

    private boolean move(File f, String dest) {

        // log.debug("moving ..." + f.getAbsolutePath());

        boolean r1 = false;
        if (f.isDirectory()) {
            File[] list = f.listFiles();
            if (list != null && list.length > 0) {
                for (File f1 : list) {
                    if (move(f1, dest + File.separator + f.getName())) {
                        r1 = true;
                    }
                }
            }
        } else {
            /**
             * check the file version, and remove all the related version (may
             * newer or elder); <br>
             * looking for all the "f.getName()" in "classpath", and remove the
             * same package but different "version"<br>
             */
            {
                FileUtil f1 = new FileUtil(f);

                log.debug("checking [" + f1.getName() + "]");

                // check the version
                File m = new File(Model.HOME + File.separator + "WEB-INF" + File.separator + "lib");
                File[] list = m.listFiles();
                if (list != null) {
                    for (File f2 : list) {
                        if (f2.getName().endsWith(".jar")) {
                            FileUtil.R r = f1.compareTo(f2);
                            if (r != FileUtil.R.DIFF) {
                                log.warn("same jar file, but different varsion, remove [" + f2.getAbsolutePath() + "]");
                                f2.delete();
                                r1 = true;
                            }
                        }
                    }
                } else {
                    log.debug("no file in [" + m.getAbsolutePath() + "]");
                }
            }

            // ------

            File d = new File(dest + File.separator + f.getName());
            if (d.exists()) {
                d.delete();
            } else {
                d.getParentFile().mkdirs();
            }

            f.renameTo(d);

        }

        f.delete();

        return r1;
    }

    /**
     * Load menu.
     * 
     * @param me
     *            the me
     * @param name
     *            the name
     * @return the list
     */
    public List<Menu> loadMenu(User me, String name) {
        return loadMenu(me, 0, name);
    }

    /**
     * Load menu.
     * 
     * @param me
     *            the me
     * @param id
     *            the id
     * @param name
     *            the name
     * @return the list
     */
    public List<Menu> loadMenu(User me, int id, String name) {
        Beans<Menu> bs = null;
        Menu m = null;
        if (name != null) {
            /**
             * load the menu by id and name
             */
            m = Menu.load(id, name);

            if (m != null) {

                /**
                 * load the submenu of the menu
                 */
                bs = m.submenu();
            }
        } else {
            /**
             * load the submenu by id
             */
            bs = Menu.submenu(id);

        }
        List<Menu> list = bs == null ? null : bs.getList();

        /**
         * filter out the item which no access
         */
        Menu.filterAccess(list, me);

        return list;
    }

}
