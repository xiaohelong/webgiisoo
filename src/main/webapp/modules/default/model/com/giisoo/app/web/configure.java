package com.giisoo.app.web;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.commons.configuration.Configuration;

import net.sf.json.JSONObject;

import com.giisoo.core.bean.Bean;
import com.giisoo.core.bean.Bean.V;
import com.giisoo.core.bean.X;
import com.giisoo.core.conf.Config;
import com.giisoo.core.db.DB;
import com.giisoo.framework.common.User;
import com.giisoo.framework.web.Model;
import com.giisoo.framework.web.Module;
import com.giisoo.framework.web.Path;
import com.mongodb.Mongo;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;

/**
 * the initial configure, once configured, it will not be accessed
 * 
 * @author joe
 *
 */
public class configure extends Model {

    @Path()
    public void onGet() {

        try {
            if (Bean.isConfigured()) {
                this.redirect("/");
                return;
            }

            this.show("/admin/config.edit.html");

        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            this.error(e);
        }

    }

    @Path(path = "save")
    public void save() {
        JSONObject jo = new JSONObject();
        if (Bean.isConfigured()) {
            jo.put(X.STATE, 201);
            this.response(jo);
            return;
        }

        Configuration conf = Config.getConfig();

        String dbdriver = this.getHtml("db.driver");
        String dburl = this.getHtml("db.url");
        conf.setProperty("db.driver", dbdriver);
        conf.setProperty("db.url", dburl);

        String mongourl = this.getHtml("mongo.url");
        String mmongodb = this.getString("mongo.db");
        conf.setProperty("mongo[prod].url", mongourl);
        conf.setProperty("mongo[prod].db", mmongodb);

        String mqenabled = this.getString("mq.enabled");
        String mqurl = this.getHtml("mq.url");
        conf.setProperty("mq.enabled", "true".equals(mqenabled) ? "yes" : "no");
        conf.setProperty("mq.url", mqurl);

        String node = this.getString("node");
        String systemcode = this.getString("systemcode");
        conf.setProperty("node", node);
        conf.setProperty("system.code", systemcode);

        Config.save();
        DB.init();
        Bean.init(conf);

        DefaultListener.owner.upgrade(conf, Module.load("default"));

        String admin = this.getString("admin");
        String password = this.getHtml("password").trim();

        User u = User.loadById(0);
        if (u != null) {
            User.update(0, V.create("name", admin).set("password", password));
        } else {
            User.create(V.create("name", admin).set("password", password).set("id", 0));
        }

        jo.put(X.STATE, 200);
        this.response(jo);
    }

    @Path(path = "check")
    public void check() {
        JSONObject jo = new JSONObject();

        // Configuration conf = Config.getConfig();

        String op = this.getString("op");
        if ("db".equals(op)) {

            String url = this.getHtml("url");
            // String driver = this.getHtml("driver");

            // conf.setProperty("db.url", url);
            // conf.setProperty("db.driver", driver);
            //
            try {
                Connection c = DB.getConnectionByUrl(url);
                Statement stat = c.createStatement();
                stat.execute("create table test_ppp(X char(1))");
                stat.execute("drop table test_ppp");

                jo.put(X.STATE, 200);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                jo.put(X.STATE, 201);
                jo.put(X.MESSAGE, e.getMessage());
            }

        } else if ("mongo".equals(op)) {
            String url = this.getHtml("url");
            String hosts[] = url.split(";");

            ArrayList<ServerAddress> list = new ArrayList<ServerAddress>();
            for (String s : hosts) {
                try {
                    String s2[] = s.split(":");
                    String host;
                    int port = 27017;
                    if (s2.length > 1) {
                        host = s2[0];
                        port = Integer.parseInt(s2[1]);
                    } else {
                        host = s2[0];
                    }

                    list.add(new ServerAddress(host, port));
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            String dbname = this.getString("db");
            try {
                MongoOptions mo = new MongoOptions();
                mo.connectionsPerHost = 10;
                Mongo mongodb = new Mongo(list, mo);
                mongodb.getDB(dbname);
                jo.put(X.STATE, 200);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                jo.put(X.STATE, 201);
                jo.put(X.MESSAGE, e.getMessage());
            }

        } else if ("mq".equals(op)) {

            jo.put(X.STATE, 200);

        } else if ("cache".equals(op)) {

            jo.put(X.STATE, 200);

        } else {

            jo.put(X.STATE, 201);

        }
        this.response(jo);
    }

}
