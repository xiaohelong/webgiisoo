/*
 *   WebGiisoo, a java web foramewrok.
 *   Copyright (C) <2014>  <giisoo inc.>
 *
 */
package com.giisoo.app.web.admin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.giisoo.core.bean.Bean;
import com.giisoo.core.bean.Beans;
import com.giisoo.core.bean.UID;
import com.giisoo.core.bean.X;
import com.giisoo.core.worker.WorkerTask;
import com.giisoo.framework.common.OpLog;
import com.giisoo.framework.common.Session;
import com.giisoo.framework.common.Temp;
import com.giisoo.framework.web.*;
import com.mongodb.BasicDBObject;

/**
 * web api: /admin/log
 * <p>
 * used to manage oplog
 * 
 * @author joe
 *
 */
public class log extends Model {

    @Path(path = "deleteall", login = true, access = "access.config.admin")
    public void deleteall() {
        JSONObject jo = new JSONObject();
        OpLog.remove();
        jo.put(X.STATE, 200);
        this.response(jo);
    }

    /**
     * Popup2.
     */
    @Path(path = "popup2", login = true, access = "access.log.query")
    public void popup2() {
        String type = this.getString("type");
        String cate = this.getString("cate");

        JSONObject jo = new JSONObject();

        List<String> list = null;// OpLog.loadCategory(type, cate);
        if (list != null && list.size() > 0) {
            JSONArray arr = new JSONArray();
            for (String e : list) {
                JSONObject j = new JSONObject();
                j.put("value", e);
                if ("module".equals(type)) {
                    j.put("name", lang.get("log.module_" + e));
                } else if ("op".equals(type)) {
                    j.put("name", lang.get("log.opt_" + e));
                } else {
                    j.put("name", e);
                }
                arr.add(j);
            }
            jo.put("list", arr);
            jo.put(X.STATE, 200);

        } else {
            jo.put(X.STATE, 201);
        }

        this.response(jo);

    }

    private BasicDBObject getW(JSONObject jo) {

        BasicDBObject q = new BasicDBObject();

        if (!X.isEmpty(jo.get("op"))) {
            q.append("op", jo.get("op"));
        }
        if (!X.isEmpty(jo.get("ip"))) {
            q.append("ip", Pattern.compile(jo.getString("ip"), Pattern.CASE_INSENSITIVE));
        }
        if (!X.isEmpty(jo.get("uid"))) {
            q.append("uid", Bean.toInt(jo.get("uid")));
        }
        if (!X.isEmpty(jo.get("type"))) {
            q.append("type", Bean.toInt(jo.get("type")));
        }

        if (!X.isEmpty(jo.getString("_module"))) {
            q.append("module", jo.getString("_module"));
        }

        if (!X.isEmpty(jo.getString("_system"))) {
            q.append("system", jo.getString("_system"));
        }

        if (!X.isEmpty(jo.getString("starttime"))) {
            q.append("created", new BasicDBObject().append("$gte", Bean.toInt(lang.format(lang.parse(jo.getString("starttime"), "yyyy-MM-dd"), "yyyyMMdd"))));

        } else {
            long today_2 = System.currentTimeMillis() - X.ADAY * 2;
            jo.put("starttime", lang.format(today_2, "yyyy-MM-dd"));
            q.append("created", new BasicDBObject().append("$gte", Bean.toInt(lang.format(today_2, "yyyyMMdd"))));
        }

        if (!X.isEmpty(jo.getString("endtime"))) {
            q.append("created", new BasicDBObject().append("$lte", Bean.toInt(lang.format(lang.parse(jo.getString("endtime"), "yyyy-MM-dd"), "yyyyMMdd"))));
        }

        this.set(jo);

        return q;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.giisoo.framework.web.Model#onGet()
     */
    @Path(login = true, access = "access.log.query")
    public void onGet() {

        int s = this.getInt("s");
        int n = this.getInt("n", 10, "number.per.page");

        this.set("currentpage", s);

        JSONObject jo = this.getJSON();
        BasicDBObject w = getW(jo);

        Beans<OpLog> bs = OpLog.load(w, s, n);
        this.set(bs, s, n);

        this.query.path("/admin/log");
        this.show("/admin/oplog.index.html");
    }

    /**
     * Export.
     */
    @Path(path = "export", login = true, access = "access.log.export")
    public void export() {

        /**
         * export the logs to "csv" file, and redirect to the cvs file
         */

        final JSONObject jo = this.getJSON();
        final BasicDBObject q = getW(jo);

        String id = UID.id(login.get("name"), jo.toString());
        String name = "oplog_" + lang.format(System.currentTimeMillis(), "yyyMMdd") + ".csv";
        final File f = Temp.get(id, name);

        if (f.exists() && System.currentTimeMillis() - f.lastModified() > X.AHOUR) {
            f.delete();
        } else {
            f.getParentFile().mkdirs();
        }

        if (!f.exists()) {
            final Session session = this.getSession();
            session.set("oplog.exporting", 1).store();
            new WorkerTask() {

                @Override
                public void onExecute() {
                    try {
                        int s = 0;

                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));

                        /**
                         * output the header
                         */
                        StringBuilder sb = new StringBuilder();
                        sb.append("\"").append(lang.get("log.created")).append("\",\"");
                        sb.append(lang.get("log.user")).append("\",\"");
                        sb.append(lang.get("log.ip")).append("\",\"");
                        sb.append(lang.get("log.system")).append("\",\"");
                        sb.append(lang.get("log.module")).append("\",\"");
                        sb.append(lang.get("log.op")).append("\", \"");
                        sb.append(lang.get("log.message")).append("\"");
                        out.write(sb.toString() + "\r\n");

                        Beans<OpLog> bs = OpLog.load(q, s, 100);
                        while (bs != null && bs.getList() != null && bs.getList().size() > 0) {
                            for (OpLog p : bs.getList()) {
                                sb = new StringBuilder();
                                sb.append("\"").append(lang.format(p.getLong("created"), "yyyy-MM-dd hh:mm:ss")).append("\",\"");

                                if (p.getUser() != null) {
                                    sb.append(p.getUser().get("name")).append("\",\"");
                                } else {
                                    sb.append("\",\"");
                                }

                                if (X.isEmpty(p.get("ip"))) {
                                    sb.append(p.get("ip")).append("\",\"");
                                } else {
                                    sb.append("\",\"");
                                }

                                if (p.getSystem() != null) {
                                    sb.append(p.getSystem()).append("\",\"");
                                } else {
                                    sb.append("\",\"");
                                }
                                if (p.getModule() != null) {
                                    sb.append(lang.get("log.module_" + p.getModule())).append("\",\"");
                                } else {
                                    sb.append("\",\"");
                                }

                                if (p.getOp() != null) {
                                    sb.append(lang.get("log.opt_" + p.getOp())).append("\"");
                                } else {
                                    sb.append("\",\"");
                                }

                                if (p.getMessage() != null) {
                                    sb.append(p.getMessage()).append("\"");
                                } else {
                                    sb.append("\",\"");
                                }

                                out.write(sb.toString() + "\r\n");
                            }
                            s += bs.getList().size();
                            bs = OpLog.load(q, s, 100);
                        }

                        out.close();

                        OpLog.info(OpLog.class, "export", jo.toString(), (String) null, login.getId(), log.this.getRemoteHost());

                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    } finally {
                        Session.load(session.sid()).remove("oplog.exporting").store();
                    }
                }

            }.schedule(0);

        }

        JSONObject jo1 = new JSONObject();
        jo1.put(X.STATE, 200);
        jo1.put("file", "/temp/" + id + "/" + name);
        jo1.put("size", f.length());
        jo1.put("updated", f.lastModified());
        if (this.getSession().get("oplog.exporting") != null) {
            jo1.put("exporting", 1);
        } else {
            jo1.put("exporting", 0);
        }

        this.response(jo1);
    }
}
