package com.giisoo.app.web.admin;

import com.giisoo.app.web.admin.setting;
import com.giisoo.core.bean.X;
import com.giisoo.core.conf.SystemConfig;
import com.giisoo.framework.utils.SyncTask;

/**
 * web api: /admin/setting/[method]/sync
 * <p>
 * used to set syncing configuration
 * 
 * @author joe
 *
 */
public class sync extends setting {

    @Override
    public void reset() {
        for (String c : SyncTask.getCollections().keySet()) {
            SystemConfig.setConfig("sync." + c + ".lasttime", 0L);
        }
        super.reset();
    }

    @Override
    public void set() {
        String url = this.getString("sync_url");
        boolean changed = false;
        if (url != null && !url.equals(SystemConfig.s("sync.url", X.EMPTY))) {
            // was changed, reset all synced flag
            changed = true;
        }
        SystemConfig.setConfig("sync.url", this.getString("sync_url"));
        SystemConfig.setConfig("sync.appid", this.getString("sync_appid"));
        SystemConfig.setConfig("sync.appkey", this.getString("sync_appkey"));

        for (String group : SyncTask.getGroups()) {
            String s = this.getString("sync_" + group);
            SystemConfig.setConfig("sync." + group, s);
            for (String c : SyncTask.instance.collections(group)) {
                SystemConfig.setConfig("sync." + c, s);
            }
        }

        if (changed) {
            for (String c : SyncTask.getCollections().keySet()) {
                SystemConfig.setConfig("sync." + c + ".lasttime", 0L);
            }
        }

        SyncTask.instance.schedule(1000);

        this.set(X.MESSAGE, "修改成功！");

        get();
    }

    @Override
    public void get() {
        this.set("sync_url", SystemConfig.s("sync.url", null));
        this.set("sync_appid", SystemConfig.s("sync.appid", null));
        this.set("sync_appkey", SystemConfig.s("sync.appkey", null));

        // this.set("collections", SyncTask.getCollections().keySet());
        this.set("t", SyncTask.instance);
        this.set("groups", SyncTask.getGroups());

        this.set("page", "/admin/setting.sync.html");
    }

}
