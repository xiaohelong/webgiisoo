/*
 *   WebGiisoo, a java web foramewrok.
 *   Copyright (C) <2014>  <giisoo inc.>
 *
 */
package com.giisoo.app.web.admin;

import java.util.LinkedHashMap;
import java.util.Map;

import com.giisoo.core.bean.X;
import com.giisoo.core.conf.SystemConfig;
import com.giisoo.framework.common.OpLog;
import com.giisoo.framework.utils.Shell;
import com.giisoo.framework.web.*;

/**
 * web api: /admin/setting
 * <p>
 * use to custom setting, all module configuration MUST inherit from this class,
 * and override the "set" and "get" method
 * 
 * @author joe
 *
 */
public class setting extends Model {

    private static Map<String, Class<? extends setting>> settings = new LinkedHashMap<String, Class<? extends setting>>();

    final public static void register(String name, Class<? extends setting> m) {
        settings.put(name, m);
    }

    @Path(path = "reset/(.*)", login = true, access = "access.config.admin")
    final public Object reset(String name) {
        Class<? extends setting> c = settings.get(name);
        log.debug("/reset/" + c);
        if (c != null) {
            try {
                setting s = c.newInstance();
                s.req = this.req;
                s.resp = this.resp;
                s.login = this.login;
                s.lang = this.lang;
                s.module = this.module;
                s.reset();

                s.set("lang", lang);
                s.set("module", module);
                s.set("name", name);
                s.set("settings", settings.keySet());
                s.show("/admin/setting.html");

            } catch (Exception e) {
                log.error(name, e);
                this.show("/admin/setting.html");
            }
        }

        return null;
    }

    @Path(path = "get/(.*)", login = true, access = "access.config.admin")
    final public Object get(String name) {
        Class<? extends setting> c = settings.get(name);
        log.debug("/get/" + c);
        if (c != null) {
            try {
                setting s = c.newInstance();
                s.copy(this);
                s.get();

                s.set("lang", lang);
                s.set("module", module);
                s.set("name", name);
                s.set("settings", settings.keySet());
                s.show("/admin/setting.html");

            } catch (Exception e) {
                log.error(name, e);
                this.show("/admin/setting.html");
            }
        }

        return null;
    }

    @Path(path = "set/(.*)", login = true, access = "access.config.admin", log = Model.METHOD_POST)
    final public void set(String name) {
        Class<? extends setting> c = settings.get(name);
        log.debug("/set/" + c);
        if (c != null) {
            try {
                setting s = c.newInstance();
                s.copy(this);
                s.set();

                s.set("lang", lang);
                s.set("module", module);
                s.set("name", name);
                s.set("settings", settings.keySet());
                s.show("/admin/setting.html");
            } catch (Exception e) {
                log.error(name, e);
                this.show("/admin/setting.html");
            }
        }
    }

    /**
     * invoked when post setting form
     * 
     */
    public void set() {

    }

    /**
     * invoked when reset called
     */
    public void reset() {
        get();
    }

    /**
     * invoked when get the setting form
     * 
     */
    public void get() {

    }

    @Path(login = true, access = "access.config.admin")
    public final void onGet() {

        if (settings.size() > 0) {
            String name = settings.keySet().iterator().next();
            this.set("name", name);
            get(name);
            return;
        }

        this.println("not find page");

    }

    public static class system extends setting {

        @Override
        public void set() {
            SystemConfig.setConfig("node.name", this.getString("nodename"));
            SystemConfig.setConfig("mdc.tcp.enabled", "on".equals(this.getString("mdc_tcp")) ? "true" : "false");
            SystemConfig.setConfig("mdc.tcp.host", this.getString("mdc_tcp_host"));
            SystemConfig.setConfig(Model.node() + ".mdc.tcp.domain", this.getString("mdc_tcp_domain"));
            SystemConfig.setConfig("mdc.tcp.port", this.getString("mdc_tcp_port"));
            SystemConfig.setConfig("mdc.udp.enabled", "on".equals(this.getString("mdc_udp")) ? "true" : "false");
            SystemConfig.setConfig("mdc.udp.host", this.getString("mdc_udp_host"));
            SystemConfig.setConfig("mdc.udp.port", this.getString("mdc_udp_port"));
            SystemConfig.setConfig("mdc.allow.ip", this.getString("mdc_allow_ip"));
            SystemConfig.setConfig("mdc.allow.uid", this.getString("mdc_allow_uid"));
            SystemConfig.setConfig("mdc.allow.user", this.getString("mdc_allow_user"));

            this.set(X.MESSAGE, lang.get("save.success") + ", " + lang.get("restart.required"));

            SystemConfig.setConfig("ntp.server", this.getString("ntp"));

            if (!X.isEmpty(this.getString("ntp"))) {

                try {
                    String r = Shell.run("ntpdate " + this.getString("ntp"));
                    OpLog.info("ntp", null, "时钟同步： " + r);
                } catch (Exception e) {
                    OpLog.error("ntp", null, "时钟同步： " + e.getMessage());
                    log.error(e.getMessage(), e);
                }

            }

            get();
        }

        @Override
        public void get() {

            this.set("node", SystemConfig.s("node", null));
            this.set("system_code", SystemConfig.l("system.code", 1));
            this.set("mdc_tcp", SystemConfig.s("mdc.tcp.enabled", null));
            this.set("mdc_tcp_host", SystemConfig.s("mdc.tcp.host", null));
            this.set("mdc_tcp_domain", SystemConfig.s(Model.node() + ".mdc.tcp.domain", null));
            this.set("mdc_tcp_port", SystemConfig.s("mdc.tcp.port", null));
            this.set("mdc_udp", SystemConfig.s("mdc.udp.enabled", null));
            this.set("mdc_udp_host", SystemConfig.s("mdc.udp.host", null));
            this.set("mdc_udp_port", SystemConfig.s("mdc.udp.port", null));
            this.set("mdc_allow_ip", SystemConfig.s("mdc.allow.ip", null));
            this.set("mdc_allow_uid", SystemConfig.s("mdc.allow.uid", null));
            this.set("mdc_allow_user", SystemConfig.s("mdc.allow.user", null));

            // this.set("prikey", SystemConfig.s("pri_key", null));
            this.set("pubkey", SystemConfig.s("pub_key", null));

            this.set("ntp", SystemConfig.s("ntp.server", null));

            this.set("time", lang.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss"));

            this.set("page", "/admin/setting.system.html");
        }

    }

    public static class mail extends setting {

        @Override
        public void set() {
            SystemConfig.setConfig("mail.protocol", this.getString("protocol"));
            SystemConfig.setConfig("mail.host", this.getString("host"));
            SystemConfig.setConfig("mail.email", this.getString("email"));
            SystemConfig.setConfig("mail.title", this.getString("title"));
            SystemConfig.setConfig("mail.user", this.getString("user"));
            SystemConfig.setConfig("mail.passwd", this.getString("passwd"));

            this.set(X.MESSAGE, lang.get("save.success") + ", " + lang.get("restart.required"));

            get();
        }

        @Override
        public void get() {
            this.set("protocol", SystemConfig.s("mail.protocol", "smtp"));
            this.set("host", SystemConfig.s("mail.host", X.EMPTY));
            this.set("email", SystemConfig.s("mail.email", X.EMPTY));
            this.set("title", SystemConfig.s("mail.title", X.EMPTY));
            this.set("user", SystemConfig.s("mail.user", X.EMPTY));
            this.set("passwd", SystemConfig.s("mail.passwd", X.EMPTY));

            this.set("page", "/admin/setting.mail.html");
        }

    }
}
