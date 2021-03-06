/*
 *   WebGiisoo, a java web foramewrok.
 *   Copyright (C) <2014>  <giisoo inc.>
 *
 */
package com.giisoo.app.web.admin;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import net.sf.json.JSONObject;

import org.apache.commons.fileupload.FileItem;

import com.giisoo.core.bean.UID;
import com.giisoo.core.bean.X;
import com.giisoo.core.worker.WorkerTask;
import com.giisoo.framework.common.*;
import com.giisoo.framework.common.Repo.Entity;
import com.giisoo.framework.web.*;

/**
 * web api: /admin/module
 * <p>
 * used to manage modules
 * 
 * @author joe
 *
 */
public class module extends Model {

    private static String ROOT = "/tmp/modules/";

    /**
     * create a new module
     */
    @Path(path = "create", login = true, access = "access.config.admin", log = Model.METHOD_POST | Model.METHOD_GET)
    public void create() {
        if (method.isPost()) {
            /**
             * create
             */
            JSONObject jo = new JSONObject();

            try {
                String file = createmodule();
                jo.put(X.STATE, 200);
                jo.put("file", file);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                jo.put(X.MESSAGE, e.getMessage());
                jo.put(X.STATE, 201);
            }
            this.response(jo);
        } else {
            this.show("/admin/module.create.html");
        }
    }

    private String createmodule() throws Exception {
        int id = this.getInt("id");
        String name = this.getString("name");
        String package1 = this.getString("package");
        String lifelistener = package1 + "." + name.substring(0, 1).toUpperCase() + name.substring(1) + "Listener";
        String setting = this.getString("setting");
        String readme = this.getString("readme");

        String fid = UID.id(login.getId(), System.currentTimeMillis());

        File f = Temp.get(fid, name + ".zip");
        if (f.exists()) {
            f.delete();
        } else {
            f.getParentFile().mkdirs();
        }

        ZipOutputStream out = null;

        try {
            out = new ZipOutputStream(new FileOutputStream(f));
            create(out, name + "/").closeEntry();
            create(out, name + "/.project");

            File f1 = module.getFile("/admin/demo/.project");
            if (f1.exists()) {
                // copy to out
                copy(out, f1, new String[] { "webdemo", name });
            }

            create(out, name + "/depends/").closeEntry();
            f1 = new File(Model.HOME + "/WEB-INF/lib/");
            List<String> list = new ArrayList<String>();
            for (File f2 : f1.listFiles()) {
                list.add(f2.getName());
                create(out, name + "/depends/" + f2.getName());
                copy(out, f2);
                out.closeEntry();
            }

            create(out, name + "/.classpath");
            f1 = module.getFile("/admin/demo/.classpath");
            copy(out, f1, list);
            out.closeEntry();

            create(out, name + "/build.xml");
            f1 = module.getFile("/admin/demo/build.xml");
            if (f1 != null) {
                copy(out, f1, new String[] { "WEBDEMO", name.toUpperCase() });
            }
            out.closeEntry();

            create(out, name + "/src/").closeEntry();
            create(out, name + "/src/module.ini");
            println(out, "id=" + id, "enabled=true", "default.language=zh_cn", "name=" + name, "package=" + package1, "lifelistener=" + lifelistener, "screenshot=/images/demo-screenshot.png",
                    "readme=" + readme, "version=1.0", "build=0");
            out.closeEntry();

            create(out, name + "/src/WEB-INF/").closeEntry();
            create(out, name + "/src/WEB-INF/lib/").closeEntry();
            create(out, name + "/src/i18n/").closeEntry();
            create(out, name + "/src/i18n/zh_cn.lang").closeEntry();
            create(out, name + "/src/view/").closeEntry();

            f1 = module.getFile("/admin/demo/src/view/");
            if (f1.exists()) {
                for (File f2 : f1.listFiles()) {
                    if (f2.isFile()) {
                        create(out, name + "/src/view/" + f2.getName());
                        copy(out, f2);
                        out.closeEntry();
                    }
                }
            }
            create(out, name + "/src/view/js/").closeEntry();
            f1 = module.getFile("/admin/demo/src/view/js");
            if (f1 != null) {
                for (File f2 : f1.listFiles()) {
                    if (f2.isFile()) {
                        create(out, name + "/src/view/js/" + f2.getName());
                        copy(out, f2);
                        out.closeEntry();
                    }
                }
            }
            create(out, name + "/src/view/css/").closeEntry();
            f1 = module.getFile("/admin/demo/src/view/css");
            if (f1 != null) {
                for (File f2 : f1.listFiles()) {
                    if (f2.isFile()) {
                        create(out, name + "/src/view/css/" + f2.getName());
                        copy(out, f2);
                        out.closeEntry();
                    }
                }
            }
            create(out, name + "/src/view/images/").closeEntry();
            f1 = module.getFile("/admin/demo/src/view/images");
            if (f1 != null) {
                for (File f2 : f1.listFiles()) {
                    if (f2.isFile()) {
                        create(out, name + "/src/view/images/" + f2.getName());
                        copy(out, f2);
                        out.closeEntry();
                    }
                }
            }
            create(out, name + "/src/view/admin/").closeEntry();
            f1 = module.getFile("/admin/demo/src/view/admin");
            if (f1 != null) {
                for (File f2 : f1.listFiles()) {
                    if (f2.isFile()) {
                        create(out, name + "/src/view/admin/" + f2.getName());
                        copy(out, f2);
                        out.closeEntry();
                    }
                }
            }
            f1 = module.getFile("/admin/demo/src/view/install");
            if (f1 != null) {
                create(out, name + "/src/view/install/").closeEntry();
                for (File f2 : f1.listFiles()) {
                    if (f2.isFile()) {
                        create(out, name + "/src/view/install/" + f2.getName());
                        copy(out, f2);
                        out.closeEntry();
                    }
                }
            }

            create(out, name + "/src/model/").closeEntry();
            create(out, name + "/src/model/java/").closeEntry();
            String[] ss = package1.split("\\.");
            String s1 = null;
            for (String s : ss) {
                if (s1 == null) {
                    s1 = s;
                } else {
                    s1 = s1 + "/" + s;
                }
                create(out, name + "/src/model/java/" + s1 + "/").closeEntry();
            }

            String p1 = package1.replaceAll("\\.", "/");
            create(out, name + "/src/model/java/" + p1.substring(0, p1.lastIndexOf("/")) + "/bean/").closeEntry();
            create(out, name + "/src/model/java/" + p1 + "/admin/").closeEntry();

            // copy Demo.java
            create(out, name + "/src/model/java/" + p1.substring(0, p1.lastIndexOf("/")) + "/bean/Demo.java");
            f1 = module.getFile("/admin/demo/src/model/Demo.java");
            if (f1 != null) {
                copy(out, f1, new String[] { "com.giisoo.demo.bean", (p1.substring(0, p1.lastIndexOf("/")) + "/bean").replaceAll("/", ".") });
            }
            out.closeEntry();

            // copy demo.java
            create(out, name + "/src/model/java/" + p1 + "/demo.java");
            f1 = module.getFile("/admin/demo/src/model/demo.java");
            if (f1 != null) {
                copy(out, f1, new String[] { "com.giisoo.demo.web", p1.replaceAll("/", ".") });
            }
            out.closeEntry();

            if (!X.isEmpty(lifelistener)) {
                create(out, name + "/src/model/java/" + lifelistener.replaceAll("\\.", "/") + ".java");
                if (!X.isEmpty(setting)) {
                    f1 = module.getFile("/admin/demo/src/model/DemoListener.java");
                    if (f1 != null) {
                        copy(out, f1, new String[] { "com.giisoo.demo.web", lifelistener.substring(0, lifelistener.lastIndexOf(".")) }, new String[] { "demosetting",
                                setting.substring(setting.lastIndexOf(".") + 1) }, new String[] { "DemoListener", lifelistener.substring(lifelistener.lastIndexOf(".") + 1) }, new String[] {
                                "WEBDEMO", name });
                    }
                    out.closeEntry();
                } else {
                    f1 = module.getFile("/admin/demo/src/model/DemoListener.java");
                    if (f1 != null) {
                        copy(out, f1, new String[] { "com.giisoo.demo.web", lifelistener.substring(0, lifelistener.lastIndexOf(".")) }, new String[] { "demosetting", null }, new String[] {
                                "DemoListener", lifelistener.substring(lifelistener.lastIndexOf(".") + 1) }, new String[] { "WEBDEMO", name });
                    }
                    out.closeEntry();
                }

                if (!X.isEmpty(setting)) {
                    create(out, name + "/src/model/java/" + setting.replaceAll("\\.", "/") + ".java");
                    f1 = module.getFile("/admin/demo/src/model/demosetting.java");
                    if (f1 != null) {
                        copy(out, f1, new String[] { "com.giisoo.demo.web.admin", setting.substring(0, setting.lastIndexOf(".")) }, new String[] { "demosetting",
                                setting.substring(setting.lastIndexOf(".") + 1) });
                    }
                    out.closeEntry();
                }
            }

        } finally {
            if (out != null) {
                out.close();
            }
        }

        return "/temp/" + fid + "/" + name + ".zip";
    }

    private ZipOutputStream copy(ZipOutputStream out, File f) throws Exception {
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            Model.copy(in, out, false);
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return out;
    }

    private ZipOutputStream println(ZipOutputStream out, String... ss) throws Exception {
        PrintStream o = new PrintStream(out);
        for (String s : ss) {
            o.println(s);
        }

        return out;
    }

    private ZipOutputStream copy(ZipOutputStream out, File f, String[]... replacement) throws Exception {
        BufferedReader in = null;
        try {
            PrintStream o = new PrintStream(out);
            in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            String line = in.readLine();
            while (line != null) {
                if (replacement != null && replacement.length > 0) {
                    for (String[] ss : replacement) {
                        if (ss.length == 2) {
                            if (X.isEmpty(ss[1])) {
                                line = X.EMPTY;
                            } else {
                                line = line.replaceAll(ss[0], ss[1]);
                            }
                        }
                    }
                }
                o.println(line);

                line = in.readLine();
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return out;
    }

    private ZipOutputStream copy(ZipOutputStream out, File f, List<String> classpath) throws Exception {
        BufferedReader in = null;
        try {
            PrintStream o = new PrintStream(out);
            in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            String line = in.readLine();
            while (line != null) {
                if (line.indexOf("webdemo.jar") > 0) {
                    for (String s : classpath) {
                        String s1 = line.replaceAll("webdemo.jar", s);
                        o.println(s1);
                    }
                } else {
                    o.println(line);
                }
                line = in.readLine();
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return out;
    }

    private ZipOutputStream create(ZipOutputStream out, String filename) throws IOException {
        ZipEntry e = new ZipEntry(filename);
        out.putNextEntry(e);
        return out;
    }

    /**
     * Adds the.
     */
    @Path(path = "add", login = true, access = "access.config.admin", log = Model.METHOD_POST | Model.METHOD_GET)
    public void add() {

        String url = this.getString("url");
        Entity e = Repo.loadByUri(url);

        JSONObject jo = new JSONObject();
        if (e != null) {
            try {
                ZipInputStream in = new ZipInputStream(e.getInputStream());

                /**
                 * store all entry in temp file
                 */
                String temp = Language.getLanguage().format(System.currentTimeMillis(), "yyyyMMdd");
                String root = Model.HOME + "/modules/" + temp + "/";

                ZipEntry z = in.getNextEntry();
                byte[] bb = new byte[4 * 1024];
                while (z != null) {
                    File f = new File(root + z.getName());

                    // log.info("name:" + z.getName() + ", " +
                    // f.getAbsolutePath());
                    if (z.isDirectory()) {
                        f.mkdirs();
                    } else {
                        if (!f.exists()) {
                            f.getParentFile().mkdirs();
                        }

                        FileOutputStream out = new FileOutputStream(f);
                        int len = in.read(bb);
                        while (len > 0) {
                            out.write(bb, 0, len);
                            len = in.read(bb);
                        }

                        out.close();
                    }

                    z = in.getNextEntry();
                }

                Module m = Module.load(temp);
                File f = new File(root);
                File dest = new File(Model.HOME + File.separator + "modules" + File.separator + m.getName());
                if (dest.exists()) {
                    delete(dest);
                }

                Module m1 = Module.load(m.getName());
                if (m1 != null) {
                    String repo = m1.getRepo();
                    if (!X.isEmpty(repo)) {
                        log.debug("old.repo=" + repo + ", new.repo=" + url);
                        Entity e1 = Repo.loadByUri(repo);
                        if (e1 != null && !X.isSame(e1.getId(), e.getId())) {
                            // not the same file
                            e1.delete();
                        }
                    }
                }

                /**
                 * merge WEB-INF and depends lib
                 * 
                 */
                boolean restart = m.merge();

                /**
                 * move the temp to target dest
                 */
                f.renameTo(dest);

                Module.init(m);
                m.set(m.getName() + "_repo", url);
                m.store();

                jo.put("result", "ok");

                if (restart) {
                    jo.put(X.STATE, 201);
                    jo.put("message", lang.get("auto_restarting"));
                } else {
                    jo.put(X.STATE, 200);
                    jo.put("message", lang.get("required_restart"));
                }

                /**
                 * delete the old file in repo
                 */
                // e.delete();

                if (restart) {
                    new WorkerTask() {

                        @Override
                        public void onExecute() {
                            log.info("WEB-INF has been merged, need to restart");
                            System.exit(0);
                        }

                    }.schedule(2000);
                }
            } catch (Exception e1) {
                log.error(e.toString(), e1);

                /**
                 * the file is bad, delete it from the repo
                 */
                e.delete();

                jo.put(X.STATE, 202);
                jo.put("result", "fail");
                jo.put("message", lang.get("invalid.module.package"));
            } finally {
                e.close();
            }
        } else {
            jo.put(X.STATE, 202);
            jo.put("result", "fail");
            jo.put("message", "entity not found in repo for [" + url + "]");
        }

        this.response(jo);

    }

    /**
     * Index.
     */
    @Path(login = true, access = "access.config.admin")
    public void onGet() {

        List<Module> actives = new ArrayList<Module>();
        Module m = Module.home;
        while (m != null) {
            actives.add(m);
            m = m.floor();
        }

        this.set("actives", actives);

        this.set("list", Module.getAll());

        this.query.path("/admin/module");

        this.show("/admin/module.index.html");

    }

    /**
     * Download.
     */
    @Path(path = "download", login = true, access = "access.config.admin")
    public void download() {
        String name = this.getString("name");

        /**
         * zip module
         */
        Module m = Module.load(name);
        String file = ROOT + name + ".zip";
        File f = m.zipTo(Model.HOME + file);
        if (f != null && f.exists()) {

            this.set("f", f);
            this.set("link", file);

            this.show("/admin/module.download.html");
            return;
        } else {
            this.set(X.MESSAGE, lang.get("message.fail"));
            onGet();
        }
    }

    /**
     * Disable.
     */
    @Path(path = "disable", login = true, access = "access.config.admin", log = Model.METHOD_POST | Model.METHOD_GET)
    public void disable() {
        String name = this.getString("name");

        Module m = Module.load(name);
        m.setEnabled(false);

        onGet();
    }

    /**
     * Enable.
     */
    @Path(path = "enable", login = true, access = "access.config.admin", log = Model.METHOD_POST | Model.METHOD_GET)
    public void enable() {
        String name = this.getString("name");

        Module m = Module.load(name);
        m.setEnabled(true);

        onGet();
    }

    /**
     * Delete.
     */
    @Path(path = "delete", login = true, access = "access.config.admin", log = Model.METHOD_POST | Model.METHOD_GET)
    public void delete() {
        String name = this.getString("name");
        Module m = Module.load(name);
        String url = m.get("repo");
        if (!X.isEmpty(url)) {
            Entity e = Repo.loadByUri(url);
            if (e != null) {
                e.delete();
            }
        }
        m.delete();

        onGet();
    }

    @SuppressWarnings("unused")
    private boolean validate(FileItem file) {
        return false;
    }
}