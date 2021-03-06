package com.giisoo.app.web.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import com.giisoo.core.bean.Beans;
import com.giisoo.core.bean.UID;
import com.giisoo.core.bean.X;
import com.giisoo.core.worker.WorkerTask;
import com.giisoo.framework.common.Data;
import com.giisoo.framework.common.Repo;
import com.giisoo.framework.common.Temp;
import com.giisoo.framework.common.Repo.Entity;
import com.giisoo.framework.web.Model;
import com.giisoo.framework.web.Path;
import com.mongodb.BasicDBObject;

/**
 * web api: /admin/data
 * <p>
 * used to manage custom data in mongo
 * 
 * @author joe
 *
 */
public class data extends Model {

    @Path(login = true, path = "addbatch", access = "access.config.admin", log = Model.METHOD_GET)
    public void addbatch() {

        String url = this.getString("url");
        JSONObject jo1 = new JSONObject();

        Entity e = Repo.loadByUri(url);
        if (e != null) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(e.getInputStream(), "UTF-8"));

                int lines = 0;
                int errors = 0;
                String line = reader.readLine().trim();

                line = reader.readLine();

                while (line != null) {
                    /**
                     * import nodes
                     */
                    lines++;
                    try {
                        JSONObject jo = JSONObject.fromObject(line);

                        String collection = jo.getString("collection");
                        if (!X.isEmpty(collection)) {
                            jo.remove("collection");
                            Data.update(collection, jo);
                        }
                    } catch (Exception e1) {
                        log.error(line, e1);
                        errors++;
                    }

                    line = reader.readLine();
                }
                jo1.put(X.MESSAGE, "导入成功，导入：" + lines + "；错误：" + errors);
                jo1.put(X.STATE, 200);

                reader.close();

            } catch (Exception e1) {
                log.error(e1.getMessage(), e1);
                jo1.put(X.MESSAGE, "导入失败，请查看日志, error=" + e1.getMessage());
                jo1.put(X.STATE, 201);
            } finally {
                e.delete();
            }
        } else {
            jo1.put(X.MESSAGE, "无法读取文件，请重试");
            jo1.put(X.STATE, 201);
        }

        this.response(jo1);
    }

    @Path(login = true, access = "access.config.admin")
    public void onGet() {

        String collection = this.getString("collection");
        String query = this.getHtml("_query");

        this.set("collection", collection);
        this.set("_query", query);

        BasicDBObject q = new BasicDBObject();
        if (!X.isEmpty(query)) {
            JSONObject jo = JSONObject.fromObject(query);
            for (Object name : jo.keySet()) {
                q.append(name.toString(), jo.get(name));
            }
        }

        int s = this.getInt("s");
        int n = this.getInt("n", 10, "number.per.page");

        if (!X.isEmpty(collection)) {
            Beans<Data> bs = Data.load(collection, q, new BasicDBObject().append(X._ID, 1), s, n);

            List<String> fields = new ArrayList<String>();
            if (bs != null && bs.getList() != null && bs.getList().size() > 0) {
                Data d = bs.getList().get(0);
                for (String name : d.keySet()) {
                    if (!X._ID.equals(name)) {
                        Object o = d.get(name);
                        if (o instanceof List || o instanceof Map) {
                            continue;
                        }

                        if (fields.size() > 10) {
                            this.set("hasmore", 1);
                            break;
                        }
                        fields.add(name);
                    }
                }
            }

            this.set("fields", fields);
            this.set(bs, s, n);
        }

        this.query.path("/admin/data");
        this.show("/admin/data.index.html");

    }

    @Path(path = "detail", login = true, access = "access.config.admin")
    public void detail() {
        String collection = this.getString("collection");
        String id = this.getString("id");

        this.set("collection", collection);
        this.set("id", id);

        Data d = Data.load(collection, id);

        this.set("d", d);

        this.show("/admin/data.detail.html");
    }

    @Path(path = "update", login = true, access = "access.config.admin", log = Model.METHOD_POST)
    public void update() {
        String collection = this.getString("collection");
        String id = this.getString("id");

        if (method.isPost()) {
            String body = this.getHtml("body");

            if (!X.isEmpty(collection) && !X.isEmpty(body)) {
                Data.update(collection, JSONObject.fromObject(body));

                detail();

                return;
            }
        } else {
            this.set("collection", collection);
            this.set("id", id);

            Data d = Data.load(collection, id);
            if (d != null) {
                this.set("body", d.getJSON().toString());
            }
        }

        this.show("/admin/data.update.html");
    }

    @Path(path = "export", login = true, access = "access.config.admin")
    public void export() {
        String collection = this.getString("collection");
        String query = this.getString("_query");
        String type = this.getString("type");

        task = new ExportTask(collection, query, type);
        task.schedule(0);
    }

    @Path(path = "exporting", login = true, access = "access.config.admin")
    public void exporting() {
        JSONObject jo = new JSONObject();
        if (task == null) {
            jo.put(X.STATE, 201);
            jo.put(X.MESSAGE, "没有设置导出条件！");
        } else {
            if (task.done) {
                jo.put("done", 1);
            }
            if (!X.isEmpty(task.message)) {
                jo.put(X.MESSAGE, task.message);
            }
            jo.put("count", task.count);
            if (!X.isEmpty(task.filename)) {
                jo.put("file", task.filename);
            }
            jo.put(X.STATE, 200);
        }
        this.response(jo);
    }

    static ExportTask task = null;

    public static class ExportTask extends WorkerTask {

        String collection;
        String query;
        int type = 0; // 0: json, 1: excel

        boolean done = false;

        int count = 0;
        String filename;
        String message;

        ExportTask(String collection, String query, String type) {
            this.collection = collection;
            this.query = query;
            if ("excel".equals(type)) {
                this.type = 1;
            }
        }

        @Override
        public void onExecute() {

            String id = UID.id(collection, query);
            String name = collection + (type == 0 ? ".json" : ".csv");

            File f = Temp.get(id, name);
            if (f.exists()) {
                f.delete();
            } else {
                f.getParentFile().mkdirs();
            }

            PrintStream out = null;
            try {
                out = new PrintStream(new FileOutputStream(f));

                int s = 0;
                BasicDBObject order = new BasicDBObject(X._ID, 1);
                BasicDBObject q = new BasicDBObject();
                if (!X.isEmpty(query)) {
                    JSONObject jo = JSONObject.fromObject(query);
                    for (Object s1 : jo.keySet()) {
                        q.append(s1.toString(), jo.get(s1));
                    }
                }

                List<String> header = new ArrayList<String>();
                Beans<Data> bs = Data.load(collection, q, order, s, 100);
                if (s == 0 && type == 1 && bs != null && bs.getList() != null && bs.getList().size() > 0) {
                    // create head
                    for (Data d : bs.getList()) {
                        for (String s1 : d.keySet()) {
                            if (!header.contains(s1)) {
                                header.add(s1);
                            }
                        }
                    }
                    println(out, header.toArray());
                }

                while (bs != null && bs.getList() != null && bs.getList().size() > 0) {
                    for (Data d : bs.getList()) {
                        if (type == 0) {
                            JSONObject j = d.getJSON();
                            out.println(j.toString());
                        } else {
                            List<Object> list = new ArrayList<Object>();
                            for (String s1 : header) {
                                Object o = d.get(s1);
                                list.add(o);
                            }
                            println(out, list.toArray());
                        }
                        count++;
                    }
                    s += bs.getList().size();
                    bs = Data.load(collection, q, order, s, 100);
                }

            } catch (Exception e) {
                log.error(collection + ", " + query, e);
                message = e.getMessage();
            } finally {
                if (out != null) {
                    out.close();
                }
            }

            filename = "/temp/" + id + "/" + name;

        }

        @Override
        public void onFinish() {
            done = true;
        }

    }

    private static void println(PrintStream out, Object[] list) {
        StringBuilder sb = new StringBuilder();
        for (Object o : list) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append("\"");
            if (o instanceof String) {
                String s = (String) o;
                s = s.replaceAll("\"", "\\\"");
                sb.append(s);
            } else if (o != null) {
                sb.append(o);
            }
            sb.append("\"");
        }
        out.println(sb.toString());

    }
}
